package org.codegen.schema.openapi

import kotlinx.serialization.Serializable

@Serializable
data class Response(
    val description: String? = null,
    val schema: Schema? = null
)
