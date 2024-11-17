package org.codegen.schema.openapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Parameter(
    val name: String,
    @SerialName("in")
    val source: String,
    val description: String? = null,
    val required: Boolean,
    val type: String? = null,
    val format: String? = null,
    val schema: Schema? = null,
)
