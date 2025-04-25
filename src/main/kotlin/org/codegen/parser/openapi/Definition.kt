package org.codegen.parser.openapi

import kotlinx.serialization.Serializable

@Serializable
internal data class Definition(
    val properties: Map<String, Property> = mapOf(),
    val required: List<String> = listOf(),
) {
    companion object {
        fun simplifyName(name: String) =
            name
                .replace("#/definitions/", "")
                .replace("#/components/schemas/", "")
    }
}
