package org.codegen.parser.openapi

import kotlinx.serialization.Serializable

@Serializable
internal data class Response(
    val description: String? = null,
    // key is content-encoding, e.g. application/json
    val content: Map<String, Response> = mapOf(),
    val schema: Schema? = null,
)
