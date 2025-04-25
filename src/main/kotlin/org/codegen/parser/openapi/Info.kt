package org.codegen.parser.openapi

import kotlinx.serialization.Serializable

@Serializable
internal data class Info(
    val title: String = "",
    val description: String = "",
    val version: String = "",
)
