package org.codegen

import org.codegen.generators.AbstractCodeGenerator
import org.codegen.parser.ParserRegistry
import org.codegen.schema.Entity
import org.codegen.schema.Extension
import kotlin.reflect.full.createInstance

class Builder(
    private val params: Args,
) {
    fun build(): String {
        val defaultInputFile = "resource:/builtinExtensions.json"
        val inputFiles =
            params.inputPaths
                .toMutableList()
                .also {
                    // prepend default extensions (might be overridden by custom extensions)
                    it.add(0, defaultInputFile)
                }

        val documents = inputFiles.associateWith { ParserRegistry.parseFileOrResourceOrUrl(it) }
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
                .filter { endpoint -> params.excludeUrlPaths.none { it in endpoint.path } }
                .filter { endpoint -> params.includeUrlPaths.isEmpty() || params.includeUrlPaths.any { it in endpoint.path } }
                .sortedBy { it.path }
                .forEach { rootEntity.endpoints.add(it) }
        }

        if (rootEntity.methods.isNotEmpty() || rootEntity.endpoints.isNotEmpty()) {
            generator.addEntity(rootEntity)
        }
        return generator.render()
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
