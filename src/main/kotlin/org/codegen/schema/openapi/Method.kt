package org.codegen.schema.openapi

import kotlinx.serialization.Serializable

@Serializable
internal data class Method(
    val operationId: String,
    val summary: String? = null,
    val description: String,
    val parameters: List<Parameter>,
    val requestBody: Response? = null,
    val responses: Map<Int, Response>,
)
