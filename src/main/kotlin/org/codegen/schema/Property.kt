package org.codegen.schema

import kotlinx.serialization.Serializable

@Serializable
data class Property(
    val name: String,
    val dtype: String,
    val expression: List<String>,
    val description: String = "",
)
