import com.beust.jcommander.*
import java.io.File
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.codegen.dto.*
import kotlin.reflect.full.primaryConstructor

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
    val generator = generatorClass.primaryConstructor!!.call()
    val defaultInputFile = generatorClass.java.getResource("/builtinExtensions.json")!!.path
    val includedFiles = params.includeFiles.extractFiles().toMutableList().also { it.add(defaultInputFile) }
    val inputFiles = params.inputFiles.extractFiles()

    // add dtype extensions to specified generator
    (includedFiles + inputFiles)
        .map { format.decodeFromString<Document>(File(it).readText()) }
        .forEach { document ->
            document.extensions
                .forEach { extension ->
                    extension.implementations[params.target]
                        ?.let { generator.addDtypeExtension(extension.dtype, it) }
                }

            // include entities to specified generator (do not add them to output)
            document.entities
                .forEach { generator.addEntity(it, output=false) }
        }

    // add entities to specified generator
    inputFiles
        .map { format.decodeFromString<Document>(File(it).readText()) }
        .forEach { document ->
            document.entities
                .map { if (params.usePrefixed) it.prefixedFields() else it }
                .forEach { generator.addEntity(it) }
        }

    // output to stdout
    println(generator.build())
}
