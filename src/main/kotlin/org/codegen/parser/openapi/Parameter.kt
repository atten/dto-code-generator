package org.codegen.parser.openapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class Parameter(
    val name: String,
    @SerialName("in")
    val source: String,
    val description: String? = null,
    val required: Boolean = false,
    val type: String? = null,
    val format: String? = null,
    val schema: Schema? = null,
)
