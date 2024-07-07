package org.codegen

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.codegen.schema.Document
import java.io.File
import kotlin.reflect.full.createInstance

class Builder(
    private val params: Args
) {
    fun build(): String {
        val format = Json { ignoreUnknownKeys = true; isLenient = true }
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
                    .forEach { generator.addEntity(it, output = false) }
            }
        }

        params.inputFiles.forEach { filePath ->
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
}
