package org.codegen.parser.openapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class Schema(
    @SerialName("\$ref")
    val ref: String? = null,
    val type: String? = null,
    val items: Schema? = null,
    val enum: List<String> = listOf(),
) {
    fun definitionName(): String {
        if (ref != null) {
            return Definition.simplifyName(ref)
        }

        if (type == "array") {
            return items!!.definitionName()
        }

        if (type != null) {
            return type
        }

        // schema is empty, return most common type available
        return "json"
    }
}
