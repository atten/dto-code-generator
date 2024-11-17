package org.codegen.schema.openapi

import kotlinx.serialization.Serializable

@Serializable
data class Definition(
    val properties: Map<String, Property>,
    val required: List<String> = listOf(),
)
