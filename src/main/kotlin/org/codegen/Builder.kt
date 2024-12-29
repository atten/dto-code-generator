package org.codegen

import kotlinx.serialization.SerializationException
import org.codegen.generators.AbstractCodeGenerator
import org.codegen.schema.Document
import org.codegen.schema.Entity
import org.codegen.schema.Extension
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
        val defaultInputFile = this.javaClass.getResource("/builtinExtensions.json")!!.path
        val inputFiles =
            params.inputFiles
                .toMutableList()
                .also {
                    // prepend default extensions (might be overridden by custom extensions)
                    it.add(0, defaultInputFile)
                }

        val documents = inputFiles.associateWith { parseAnyFormatFromFile(it) }
        val generatorClass = params.target.generatorClass
        val generator = generatorClass.createInstance()
        val rootEntityName =
            params.name
                .ifEmpty { documents.values.map { it.name }.sortedBy { it.length }.reversed().first() }
                .ifEmpty { "Generated" }

        val rootEntity = Entity(name = rootEntityName)

        for (documentMap in documents) {
            val documentPath = documentMap.key
            val document = documentMap.value

            for (extension in documentMap.value.extensions) {
                applyGeneratorExtension(generator, extension, documentPath)
            }

            document.entities
                .map { if (params.usePrefixed) it.prefixedFields() else it }
                .forEach { generator.addEntity(it) }

            document.methods
                .forEach { rootEntity.methods.add(it) }

            // add root-level endpoints to default entity
            document.endpoints
                .filter { endpoint -> params.excludePaths.none { it in endpoint.path } }
                .filter { endpoint -> params.indludePaths.isEmpty() || params.indludePaths.any { it in endpoint.path } }
                .sortedBy { it.path }
                .forEach { rootEntity.endpoints.add(it) }
        }

        if (rootEntity.methods.isNotEmpty() || rootEntity.endpoints.isNotEmpty()) {
            generator.addEntity(rootEntity)
        }
        return generator.render()
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

    private fun applyGeneratorExtension(
        generator: AbstractCodeGenerator,
        extension: Extension,
        documentPath: String,
    ) {
        val implementationsByTarget = params.target.dtypeAliases().associateWith { extension.implementations[it] }
        val implementation = implementationsByTarget.values.filterNotNull().firstOrNull()?.copy(sourcePath = documentPath)
        if (implementation != null) {
            generator.addDataType(extension.dtype, implementation)
        }
    }
}
