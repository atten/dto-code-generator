package org.codegen.schema.openapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Property(
    val title: String? = null,
    val description: String? = null,
    val type: String? = null,
    val enum: List<String>? = null,
    @SerialName("\$ref")
    val ref: String? = null,
    val items: Property? = null,
) {
    fun definitionName(): String {
        if (ref != null) {
            return ref.replace("#/definitions/", "")
        }

        if (type == "array") {
            return items!!.definitionName()
        }

        if (type != null) {
            return type
        }

        TODO()
    }
}
