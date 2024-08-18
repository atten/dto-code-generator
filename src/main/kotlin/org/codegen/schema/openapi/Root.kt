package org.codegen.schema.openapi

import kotlinx.serialization.Serializable

@Serializable
data class Root(
    val basePath: String,
    val paths: Map<String, Path>,
    val definitions: Map<String, Definition>
)
