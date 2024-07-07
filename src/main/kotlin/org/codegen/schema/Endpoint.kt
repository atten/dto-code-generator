package org.codegen.schema

import kotlinx.serialization.Serializable
import org.codegen.schema.Constants.Companion.UNSET

@Serializable
data class Endpoint(
    val name: String,
    val description: String? = null,
    val dtype: String, // return value dtype
    val path: String, // HTTP path (may include arguments in format: "/api/v1/path/{arg1}/{arg2}"
    val arguments: List<MethodArgument> = listOf(),
    val nullable: Boolean = false, // whether return value can be null
    val multiple: Boolean = false, // whether array of values is returned
    val cacheable: Boolean = false, // whether return data can be memoized
    val verb: EndpointVerb = EndpointVerb.GET,
) {
    private val argumentsWithoutDefaults = arguments.filter { it.default == UNSET }
    private val argumentsWithDefaults = arguments.filter { it.default != UNSET }
    val argumentsSortedByDefaults = argumentsWithoutDefaults + argumentsWithDefaults

    fun toMethod() = Method(name = name, description = description, arguments = arguments, dtype = dtype, nullable = nullable, multiple = multiple)
}
