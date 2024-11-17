package org.codegen.schema

import kotlinx.serialization.Serializable
import org.codegen.schema.Constants.Companion.UNSET

@Serializable
data class MethodArgument(
    val name: String,
    val dtype: String,
    val description: String? = null,
    // mappings <CONSTANT, REPRESENTATION> for field with fixed choices
    val enum: Map<String, String>? = null,
    val enumPrefix: String? = null,
    val default: String? = UNSET,
    val nullable: Boolean = false,
    // whether more than one value can be provided
    val many: Boolean = false,
) {
    val isEnum: Boolean = !enum.isNullOrEmpty()

    fun toField() =
        Field(
            name = name,
            dtype = dtype,
            longDescription = description,
            enum = enum,
            enumPrefix = enumPrefix,
            nullable = nullable,
            many = many,
            default = default,
        )
}
