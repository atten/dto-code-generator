package org.codegen

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.codegen.converters.OpenApiConverter
import org.codegen.schema.Document
import org.codegen.schema.openapi.Root
import java.io.File
import java.text.ParseException
import java.util.StringJoiner
import kotlin.reflect.full.createInstance

class Builder(
    private val params: Args
) {
    fun build(): String {
        val generatorClass = params.target.generatorClass
        val generator = generatorClass.createInstance()
        val defaultInputFile = generatorClass.java.getResource("/builtinExtensions.json")!!.path
        val includedFiles = params.includeFiles.toMutableList()
            .also {
                // prepend default extensions (might be overridden by custom extensions)
                it.add(0, defaultInputFile)
            }
        generator.excludeDefinitionNames.addAll(params.excludedEntities)

        includedFiles.forEach { filePath ->
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
                    .forEach { generator.addEntity(it, output = false) }
            }
        }

        params.inputFiles.forEach { filePath ->
            parseAnyFormatFromFile(filePath).let { document ->
                // add dtype extensions to specified generator
                document.extensions
                    .forEach { extension ->
                        extension.getForGenerator(params.target)
                            ?.let { generator.addDataType(extension.dtype, it.copy(sourcePath = filePath)) }
                    }

                // add entities to specified generator (with 'output' flag if not excluded)
                document.entities
                    .map { if (params.usePrefixed) it.prefixedFields() else it }
                    .forEach { generator.addEntity(it, output = it.name !in params.excludedEntities && generator.buildEntityName(it.name) !in params.excludedEntities) }

                // add root-level methods to default entity
                document.methods
                    .forEach { generator.defaultEntity.methods.add(it) }

                // add root-level endpoints to default entity
                document.endpoints
                    .forEach { generator.defaultEntity.endpoints.add(it) }
            }
        }

        return generator.build()
    }

    private fun parseAnyFormatFromFile(path: String): Document {
        val exceptions = mutableMapOf<String, Exception>()
        val content = File(path).readText()

        try {
            return parseDocumentFromFile(content)
        } catch (e: SerializationException) {
            exceptions["CodegenDoc"] = e
        }

        try {
            val spec = parseOpenApiSpecFromFile(content)
            return OpenApiConverter(spec).convertToDocument()
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

    private fun parseDocumentFromFile(content: String): Document {
        val format = Json { ignoreUnknownKeys = false; isLenient = true }
        return format.decodeFromString<Document>(content)
    }

    private fun parseOpenApiSpecFromFile(content: String): Root {
        val format = Json { ignoreUnknownKeys = true }
        return format.decodeFromString<Root>(content)
    }
}
