package org.codegen.schema.openapi

import kotlinx.serialization.Serializable

@Serializable
internal data class Component(
    val schemas: Map<String, Definition> = mapOf(),
)
