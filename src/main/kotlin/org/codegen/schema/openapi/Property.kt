package org.codegen.schema.openapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class Property(
    val title: String? = null,
    val description: String? = null,
    val type: String? = null,
    val format: String? = null,
    val enum: List<String>? = null,
    @SerialName("\$ref")
    val ref: String? = null,
    val items: Property? = null,
    val oneOf: List<Property> = listOf(),
) {
    fun definitionName(): String {
        if (ref != null) {
            return Definition.simplifyName(ref)
        }

        if (type == "array") {
            return items!!.definitionName()
        }

        if (type == "string" && format == "date-time") {
            return "datetime"
        }

        if (type != null) {
            return type
        }

        if (oneOf.isNotEmpty()) {
            return oneOf.joinToString(separator = "Or") { it.definitionName() }
        }

        TODO()
    }
}
