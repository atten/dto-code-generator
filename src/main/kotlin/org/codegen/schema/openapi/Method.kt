package org.codegen.schema.openapi

import kotlinx.serialization.Serializable

@Serializable
data class Method(
    val operationId: String,
    val summary: String? = null,
    val description: String,
    val parameters: List<Parameter>,
    val responses: Map<Int, Response>
)
