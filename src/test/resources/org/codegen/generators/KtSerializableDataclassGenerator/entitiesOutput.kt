// Generated by DTO-Codegen
package org.codegen.generators

import java.time.Duration
import java.time.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class EnumValue(val value: String) {
    @SerialName("value 1")
    VALUE_1("value 1"),
    @SerialName("value 2")
    VALUE_2("value 2"),
    @SerialName("value 3")
    VALUE_3("value 3"),
}

@Serializable
data class BasicDTO(
    @Contextual
    val timestamp: Instant,
    @Contextual
    val duration: Duration,
    @SerialName("enum_value")
    val enumValue: EnumValue,
    // short description
    // very long description lol
    @SerialName("customName")
    val documentedValue: Double,
    @SerialName("list_value")
    val listValue: List<Int>,
    @SerialName("optional_value")
    val optionalValue: Double = 0.0,
    @SerialName("nullable_value")
    val nullableValue: Boolean? = null,
    @SerialName("optional_list_value")
    val optionalListValue: List<Int> = listOf(),
)

/**
 * entity with all-singing all-dancing properties
 */
@Serializable
data class AdvancedDTO(
    val a: Int,
    val b: Int,
)

/**
 * entity with containers
 */
@Serializable
data class ContainerDTO(
    @Contextual
    @SerialName("basic")
    val basicSingle: BasicDTO,
    @SerialName("basics")
    val basicList: List<@Contextual BasicDTO?>,
)
