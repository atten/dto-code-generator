package org.codegen.schema

import kotlinx.serialization.Serializable
import org.codegen.schema.Constants.Companion.UNSET

@Serializable
data class Method(
    val name: String,
    val description: String? = null,
    val arguments: List<MethodArgument>,
    // return value dtype (does not return anything by default)
    val dtype: String = "void",
    // whether return value can be null
    val nullable: Boolean = false,
    // whether array of values is returned
    val many: Boolean = false,
) {
    private val argumentsWithoutDefaults = arguments.filter { it.default == UNSET }
    private val argumentsWithDefaults = arguments.filter { it.default != UNSET }
    val argumentsSortedByDefaults = argumentsWithoutDefaults + argumentsWithDefaults

    fun toEntity() = Entity(name = "$name request", fields = arguments.map { it.toField() })
}
