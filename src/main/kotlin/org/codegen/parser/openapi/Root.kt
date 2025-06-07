package org.codegen.parser.openapi

import kotlinx.serialization.Serializable

@Serializable
internal data class Root(
    val info: Info = Info(),
    val basePath: String = "",
    val paths: Map<String, Path>,
    val definitions: Map<String, Definition> = mapOf(),
    val components: Component = Component(),
) {
    fun getDefinition(ref: String): Definition {
        val name =
            ref
                .replace("#/definitions/", "")
                .replace("#/components/schemas/", "")

        return (definitions[name] ?: components.schemas[name]!!).copy(title = name)
    }
}
