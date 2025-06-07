package org.codegen.parser.openapi

import kotlinx.serialization.Serializable

@Serializable
internal data class Definition(
    val properties: Map<String, Property> = mapOf(),
    val required: List<String> = listOf(),
    // some definitions act as properties
    val title: String? = null,
    val description: String? = null,
    val type: String? = null,
    val enum: List<String>? = null,
) {
    fun getName(): String {
        if (!enum.isNullOrEmpty()) {
            return type ?: "string"
        }
        return title!!
    }
}
