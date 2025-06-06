package org.codegen.parser.openapi

import kotlinx.serialization.Serializable

@Serializable
internal data class Method(
    val summary: String = "",
    val description: String = "",
    val parameters: List<Parameter> = listOf(),
    val requestBody: Response? = null,
    val responses: Map<Int, Response>,
)
