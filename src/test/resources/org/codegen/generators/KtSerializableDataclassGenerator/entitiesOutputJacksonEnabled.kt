// Auto-generated by ez-codegen TEST_VERSION, do not edit
package org.codegen.generators

import com.fasterxml.jackson.annotation.JsonProperty
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
data class BasicDto(
    @Contextual
    val timestamp: Instant,
    @Contextual
    val duration: Duration,
    @JsonProperty("enum_value")
    @SerialName("enum_value")
    val enumValue: EnumValue,
    // short description
    // very long description lol
    @JsonProperty("customName")
    @SerialName("customName")
    val documentedValue: Double,
    @JsonProperty("list_value")
    @SerialName("list_value")
    val listValue: List<Int>,
    @JsonProperty("optional_value")
    @SerialName("optional_value")
    val optionalValue: Double = 0.0,
    @JsonProperty("nullable_value")
    @SerialName("nullable_value")
    val nullableValue: Boolean? = null,
    @JsonProperty("optional_list_value")
    @SerialName("optional_list_value")
    val optionalListValue: List<Int> = listOf(),
)

/**
 * entity with all-singing all-dancing properties
 */
@Serializable
data class AdvancedDto(
    val a: Int,
    val b: Int,
)

/**
 * entity with containers
 */
@Serializable
data class ContainerDto(
    @Contextual
    @JsonProperty("basic")
    @SerialName("basic")
    val basicSingle: BasicDto,
    @JsonProperty("basics")
    @SerialName("basics")
    val basicList: List<@Contextual BasicDto?>,
)
