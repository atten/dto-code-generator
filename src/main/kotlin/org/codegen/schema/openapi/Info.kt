package org.codegen.schema.openapi

import kotlinx.serialization.Serializable

@Serializable
internal data class Info(
    val title: String = "",
    val description: String = "",
    val version: String = "",
)
