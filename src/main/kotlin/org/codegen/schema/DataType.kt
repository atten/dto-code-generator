package org.codegen.schema

import kotlinx.serialization.Serializable
import org.codegen.utils.Reader

@Serializable
data class DataType(
    val definition: String,
    val definitionArguments: Map<String, String> = mapOf(),
    val valuesMapping: Map<String, String> = mapOf(),
    val valueWrapper: String? = null,
    val requiredHeaders: List<String> = listOf(),
    // list of related entity names. If entities aren't present in target file, they will be included (if found in included schemas)
    val requiredEntities: List<String> = listOf(),
    val includeFiles: List<String> = listOf(),
    // Original file path. Should be filled during initialization
    val sourcePath: String = "",
) {
    val isNative = includeFiles.isEmpty() && requiredEntities.isEmpty()

    fun toGeneratedValue(value: String): String {
        var result = valuesMapping.getOrDefault(value, value)
        valueWrapper?.also {
            result = it.replace("%s", result)
        }
        return result
    }

    fun loadIncludedFiles(): List<String> = includeFiles.map { Reader.readFileOrResourceOrUrl(it, sourcePath) }
}
