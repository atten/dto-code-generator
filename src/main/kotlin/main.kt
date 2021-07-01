import com.beust.jcommander.*
import org.codegen.generators.*
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

    @Parameter(names=["-p", "--prefixed"], description = "If enabled, add prefix to all fields")
    var usePrefixed = false

    @Parameter(names = ["--help"], help = true)
    var help: Boolean = false
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
    val generatorClass = params.target.generator_class
    val generator = generatorClass.primaryConstructor!!.call()
    val defaultInputFile = generatorClass.java.getResource("/builtinExtensions.json")!!.path
    val allInputFiles = mutableListOf(defaultInputFile)

    // include input files / directories
    for (path in params.inputFiles) {
        val filePaths = File(path).listFiles()?.map { it.path }?.filter { File(it).listFiles() == null }?.sorted()
        if (filePaths != null) {
            // path is a directory
            allInputFiles += filePaths
        } else {
            // path is file
            allInputFiles.add(path)
        }
    }

    for (inputFile in allInputFiles) {
        val document = format.decodeFromString<Document>(File(inputFile).readText())

        // add dtype extensions to specified generator
        for (extension in document.extensions) {
            extension.implementations[params.target]?.let { generator.addDtypeExtension(extension.dtype, it) }
        }

        // add entities to specified generator
        for (entity in document.entities) {
            var _entity = entity
            if (params.usePrefixed)
                _entity = entity.prefixedFields()
            generator.addEntity(_entity)
        }
    }

    // output to stdout
    println(generator.build())
}
