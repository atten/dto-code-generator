package org.codegen.common.dto

import generators.*
import kotlinx.serialization.*

@Serializable
data class DtypeAttributesMapping(
    val definition: String,
    val definitionArguments: Map<String, String> = mapOf(),
    val includeMetadataIntoDefinition: Boolean = true,
    val valuesMapping: Map<String, String> = mapOf(),
    val valueWrapper: String? = null,
    val requiredHeaders: List<String> = listOf(),
) {
    fun toGeneratedValue(value: String): String {
        var result = valuesMapping.getOrDefault(value, value)
        valueWrapper?.also {
            result = it.replace("%s", result)
        }
        return result
    }
}

@Serializable
data class Extension(
    val dtype: String,
    val implementations: Map<AllGeneratorsEnum, DtypeAttributesMapping>
)

@Serializable
data class Field (
    val name: String,
    val dtype: String,
    val shortDescription: String? = null,
    val longDescription: String? = null,
    val metadata: MutableMap<String, String> = mutableMapOf(),
    val enum: Map<String, String>? = null,  // mappings <CONSTANT, REPRESENTATION> for dtype='enum'
    val enumPrefix: String? = null,
    val default: String? = null,
    val nullable: Boolean = false,
)

@Serializable
data class Validator (
    val message: String,    // error message
    val conditions: List<List<String>>,
)

@Serializable
data class Property (
    val name: String,
    val dtype: String,
    val expression: List<String>,
)

@Serializable
data class Entity(
    val name: String,
    val fields: List<Field>,
    val validators: List<Validator> = listOf(),
    val properties: List<Property> = listOf(),
    val parent: String? = null,
    val description: String? = null,
    val prefix: String? = null,
) {
    val actualPrefix = prefix ?: name
    val fieldNames = fields.map { it.name }

    fun prefixedFields(): Entity {
        return this.copy(fields = fields.map { it.copy(name = "$actualPrefix ${it.name}") })
    }
}

@Serializable
data class Document(
    val extensions: List<Extension> = listOf(),
    val entities: List<Entity> = listOf(),
)

fun String.normalize(): String {
    require(this.isNotEmpty()) { "String can't be empty" }
    require(this[0].isLetter() or this[0].isWhitespace()) { "String must start with letter: '$this'" }

    var cleaned = this.map { if (it.isLetterOrDigit()) it else ' '}.joinToString("")
    while (cleaned.contains("  "))
        cleaned = cleaned.replace("  ", " ")

    cleaned = cleaned.trim()
    require(cleaned.isNotEmpty()) { "Normalized string can't be empty: $this" }

    // prepend uppercase char with space:
    // minValue -> min Value
    // MyDTO -> My D T O
    cleaned = cleaned.zipWithNext { a, b -> if (b.isLetter() && !b.isLowerCase()) "$a " else a.lowercase() }.joinToString("") + cleaned.last()
//    cleaned = cleaned.lowercase()
    return cleaned
}

fun String.snakeCase() = this.zipWithNext {
        a, b -> when {
            (a.isLetter() && a.isLowerCase() && b == ' ') -> "${a}_"
            a == ' ' -> ""
            else -> a
        }
}.joinToString("") + this.last()

fun String.camelCase() = this.normalize().split(' ').map { it.replaceFirstChar { c -> c.uppercase() } }.joinToString("")

/**
 * "text_${SHELL}" -> "text_/bin/bash/"
 */
fun String.substituteEnvVars(): String {
    var str = this
    System.getenv().forEach { (key, value) -> str = str.replace("\${${key}}", value) }
    return str
}
