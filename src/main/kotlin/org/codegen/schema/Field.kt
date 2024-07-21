package org.codegen.schema

import kotlinx.serialization.Serializable
import org.codegen.schema.Constants.Companion.UNSET

@Serializable
data class Field(
    val name: String,
    val dtype: String,
    val description: String? = null,
    val longDescription: String? = null,
    val metadata: Map<String, String> = mapOf(),
    val enum: Map<String, String>? = null, // mappings <CONSTANT, REPRESENTATION> for field with fixed choices
    val enumPrefix: String? = null,
    val default: String? = UNSET,
    val nullable: Boolean = false,
    val many: Boolean = false,
    val serializedName: String? = null, // serialization/deserialization key
    val excludeFromSerialization: Boolean = false,
) {
    val isEnum: Boolean = !enum.isNullOrEmpty()
}
