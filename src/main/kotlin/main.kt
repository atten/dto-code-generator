import com.beust.jcommander.*
import java.io.File
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.codegen.dto.*
import org.codegen.generators.AllGeneratorsEnum
import kotlin.reflect.full.createInstance

@Parameters(commandDescription = "Console tool which generates language-specific data classes and validators from JSON-like schema")
class Args {
    @Parameter(description="files/directories to parse", required = true)
    lateinit var inputFiles: List<String>

    @Parameter(names=["-t", "--target"], required = true, description="Target implementation. Available choices are:")
    lateinit var target: AllGeneratorsEnum

    @Parameter(names=["-i", "--include"], description="files/directories to include as dependencies", variableArity = true)
    var includeFiles: List<String> = listOf()

    @Parameter(names=["-p", "--prefixed"], description = "If enabled, add prefix to all fields")
    var usePrefixed = false

    @Parameter(names=["--exclude"], description = "Do not include specified entity names", variableArity = true)
    var excludedEntities: List<String> = listOf()

    @Parameter(names = ["--help"], help = true)
    var help: Boolean = false
}


fun List<String>.extractFiles(): List<String> {
    // include input files / directories
    val files = mutableListOf<String>()
    for (path in this) {
        val filePaths = File(path).listFiles()?.map { it.path }?.filter { File(it).listFiles() == null }?.sorted()
        if (filePaths != null) {
            // path is a directory
            files += filePaths
        } else {
            // path is file
            files.add(path)
        }
    }
    return files
}


fun main(args: Array<String>) {
    val params = Args()
    val parser = JCommander(Args())
//    parser.programName = "dto-codegen"

    try {
        JCommander.newBuilder().addObject(params).build().parse(*args)
    }
    catch (e: ParameterException) {
        println(e.toString())
        parser.usage()
        return
    }

    if (params.help) {
        parser.usage()
        return
    }

    val format = Json { ignoreUnknownKeys = true; isLenient = true }
    val generatorClass = params.target.generatorClass
    val generator = generatorClass.createInstance()
    val defaultInputFile = generatorClass.java.getResource("/builtinExtensions.json")!!.path
    val includedFiles = params.includeFiles.extractFiles().toMutableList()
        .also {
            // prepend default extensions (might be overridden by custom extensions)
            it.add(0, defaultInputFile)
        }
    val inputFiles = params.inputFiles.extractFiles()

    generator.excludeDefinitionNames.addAll(params.excludedEntities)

    includedFiles.forEach { filePath ->
        format.decodeFromString<Document>(File(filePath).readText()).let { document ->
            // add dtype extensions to specified generator
            document.extensions
                .forEach { extension ->
                    extension.getForGenerator(params.target)
                        ?.let {
                            generator.addDataType(extension.dtype, it.copy(sourcePath = filePath))
                        }
                }

            // include entities to specified generator (do not add them to output)
            document.entities
                .forEach { generator.addEntity(it, output=false) }
        }
    }

    inputFiles.forEach { filePath ->
        format.decodeFromString<Document>(File(filePath).readText()).let { document ->
            // add dtype extensions to specified generator
            document.extensions
                .forEach { extension ->
                    extension.getForGenerator(params.target)
                        ?.let { generator.addDataType(extension.dtype, it.copy(sourcePath = filePath)) }
                }

            // add entities to specified generator (with 'output' flag if not excluded)
            document.entities
                .map { if (params.usePrefixed) it.prefixedFields() else it }
                .forEach { generator.addEntity(it, output = it.name !in params.excludedEntities && generator.buildEntityName(it.name ) !in params.excludedEntities) }

            // add root-level methods to default entity
            document.methods
                .forEach { generator.defaultEntity.methods.add(it) }

            // add root-level endpoints to default entity
            document.endpoints
                .forEach { generator.defaultEntity.endpoints.add(it) }
        }
    }

    // output to stdout
    print(generator.build())
}
