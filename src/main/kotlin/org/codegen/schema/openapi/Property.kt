package org.codegen.schema.openapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.codegen.schema.Constants.Companion.UNSET

@Serializable
internal data class Property(
    val title: String? = null,
    val description: String? = null,
    val type: String? = null,
    val format: String? = null,
    val enum: List<String>? = null,
    val default: String? = UNSET,
    @SerialName("\$ref")
    val ref: String? = null,
    val items: Property? = null,
    val oneOf: List<Property> = listOf(),
    // same as oneOf
    val anyOf: List<Property> = listOf(),
    val properties: Map<String, Property> = mapOf(),
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

        if (type == "object" && properties.keys.containsAll(listOf("seconds", "nano", "units"))) {
            return "java duration"
        }

        if (type != null) {
            return type
        }

        val unionType = oneOf + anyOf
        if (unionType.isNotEmpty()) {
            return unionType
                .map { it.definitionName() }
                .toSet()
                .let {
                    when (it) {
                        // reduce integer + string to just string
                        setOf("integer", "string") -> setOf("string")
                        else -> it
                    }
                }
                .sorted()
                .joinToString(separator = "Or")
        }

        TODO()
    }
}
