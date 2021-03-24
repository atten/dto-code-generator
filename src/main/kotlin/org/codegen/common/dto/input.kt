package org.codegen.common.dto

import generators.*
import kotlinx.serialization.*

@Serializable
data class DtypeAttributesMapping(
    val definition: String,
    val definitionArguments: Map<String, String> = mapOf(),
    val valuesMapping: Map<String, String> = mapOf(),
    val valueWrapper: String? = null,
    val requiredHeader: String? = null,
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
    val default: String? = null,
    val nullable: Boolean = false,
//    validators
)

@Serializable
data class Entity(
    val name: String,
    val fields: List<Field>,
    val description: String? = null,
    val prefix: String? = null,
) {
    val actualPrefix = prefix ?: name

    fun prefixedFields(): Entity {
        return this.copy(fields = fields.map { it.copy(name = "$actualPrefix ${it.name}") })
    }
}

@Serializable
data class Document(
    val extensions: List<Extension> = listOf(),
    val entities: List<Entity> = listOf(),
) {
//    fun merge(another: Document): Document {
//        return Document(
//            extensions=extensions + another.extensions,
//            entities=entities + another.entities,
//        )
//    }
}

fun String.normalize(): String {
    require(this.isNotEmpty()) { "String can't be empty" }
    require(this[0].isLetter() or this[0].isWhitespace()) { "String must start with letter: '$this'" }

    var cleaned = this.map { if (it.isLetterOrDigit()) it else ' '}.joinToString("")
    while (cleaned.contains("  "))
        cleaned = cleaned.replace("  ", " ")

    cleaned = cleaned.trim()
    require(cleaned.isNotEmpty()) { "Normalized string can't be empty: $this" }

    // divide lowercase char and following uppercase with space:
    // minValue -> min value
    val lastChar = cleaned[cleaned.lastIndex]
    cleaned = cleaned.zipWithNext { a, b -> if (a.isLowerCase() && (b.isLetter() && !b.isLowerCase())) "$a " else a }.joinToString("") + lastChar
    cleaned = cleaned.toLowerCase()
    return cleaned
}

fun String.toSnakeCase() = this.normalize().replace(" ", "_")

fun String.toCamelCase() = this.normalize().split(' ').map { it.capitalize() }.joinToString("")
