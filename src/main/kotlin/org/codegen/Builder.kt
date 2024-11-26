package org.codegen

import kotlinx.serialization.SerializationException
import org.codegen.schema.Document
import org.codegen.schema.SchemaParser
import org.codegen.schema.openapi.OpenApiParser
import java.io.File
import java.text.ParseException
import java.util.*
import kotlin.reflect.full.createInstance

class Builder(
    private val params: Args,
) {
    fun build(): String {
        val generatorClass = params.target.generatorClass
        val generator = generatorClass.createInstance()
        val defaultInputFile = generatorClass.java.getResource("/builtinExtensions.json")!!.path

        params.inputFiles
            .toMutableList()
            .also {
                // prepend default extensions (might be overridden by custom extensions)
                it.add(0, defaultInputFile)
            }.forEach { filePath ->
                parseAnyFormatFromFile(filePath).let { document ->
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
                        .map { if (params.usePrefixed) it.prefixedFields() else it }
                        .forEach { generator.addEntity(it, output = false) }

                    // add root-level methods to default entity
                    document.methods
                        .forEach { generator.defaultEntity.methods.add(it) }

                    // add root-level endpoints to default entity
                    document.endpoints
                        .filter { endpoint -> params.excludePaths.none { it in endpoint.path } }
                        .filter { endpoint -> params.indludePaths.isEmpty() || params.indludePaths.any { it in endpoint.path } }
                        .sortedBy { it.path }
                        .forEach { generator.defaultEntity.endpoints.add(it) }
                }
            }

        return generator.build()
    }

    private fun parseAnyFormatFromFile(path: String): Document {
        val exceptions = mutableMapOf<String, Exception>()
        val content = File(path).readText()

        try {
            return SchemaParser().parse(content)
        } catch (e: SerializationException) {
            exceptions["CodegenDoc"] = e
        }

        try {
            return OpenApiParser().parse(content)
        } catch (e: SerializationException) {
            exceptions["OpenApi"] = e
        }

        val parserNames = exceptions.keys.joinToString(", ")
        val errorText = StringJoiner("\n")
        errorText.add("Failed to parse JSON at '$path'.")
        errorText.add("Parsers tried: $parserNames. Corresponding exceptions below:")
        exceptions.forEach { (name, exception) -> errorText.add("$name: $exception") }
        throw ParseException(errorText.toString(), 0)
    }
}
