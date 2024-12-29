package org.codegen.schema

import kotlinx.serialization.Serializable

@Serializable
data class Document(
    val name: String = "",
    val entities: MutableList<Entity> = mutableListOf(),
    // root-level methods will be inserted to default entity
    val methods: List<Method> = listOf(),
    // root-level endpoints will be inserted to default entity
    val endpoints: MutableList<Endpoint> = mutableListOf(),
    val extensions: List<Extension> = listOf(),
)
