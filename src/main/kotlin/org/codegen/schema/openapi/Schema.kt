package org.codegen.schema.openapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Schema(
    @SerialName("\$ref")
    val ref: String? = null,
    val type: String? = null,
    val items: Schema? = null,
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
