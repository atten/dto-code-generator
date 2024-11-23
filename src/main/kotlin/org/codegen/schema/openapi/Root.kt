package org.codegen.schema.openapi

import kotlinx.serialization.Serializable

@Serializable
internal data class Root(
    val paths: Map<String, Path>,
    val basePath: String = "",
    val definitions: Map<String, Definition> = mapOf(),
    val components: Component = Component(),
)
