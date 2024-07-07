package org.codegen.format

fun String.normalize(): String {
    require(this.isNotEmpty()) { "String can't be empty" }
    require(this[0].isLetter() or this[0].isWhitespace()) { "String must start with letter: '$this'" }

    var cleaned = this.map { if (it.isLetterOrDigit()) it else ' ' }.joinToString("")
    while (cleaned.contains("  "))
        cleaned = cleaned.replace("  ", " ")

    cleaned = cleaned.trim()
    require(cleaned.isNotEmpty()) { "Normalized string can't be empty: $this" }

    cleaned = if (cleaned.all { it.isUpperCase() })
    // transform uppercase string to lowercase
        cleaned.lowercase()
    else
    // Apply more complex rule for string with mixed case. Prepend uppercase char with space:
    // minValue -> min Value
    // MyDTO -> My D T O
        cleaned.zipWithNext { a, b -> if (b.isLetter() && !b.isLowerCase()) "$a " else a.lowercase() }.joinToString("") + cleaned.last()
//
    return cleaned
}

fun String.snakeCase() = this.zipWithNext {
    a, b ->
    when {
        (a.isLetter() && a.isLowerCase() && b == ' ') -> "${a}_"
        a == ' ' -> ""
        else -> a
    }
}.joinToString("") + this.last()

fun String.camelCase() = this.normalize().split(' ').joinToString("") { it.capitalizeFirst() }

fun String.capitalizeFirst() = this.replaceFirstChar { it.uppercase() }

fun String.lowercaseFirst() = this.replaceFirstChar { it.lowercase() }

/**
 * "text_${SHELL}" -> "text_/bin/bash/"
 */
fun String.substituteEnvVars(required: Boolean = true): String {
    var str = this
    System.getenv().forEach { (key, value) -> str = str.substituteEnvVariable(key, value) }
    System.getProperties().forEach { (key, value) -> str = str.substituteEnvVariable(key.toString(), value.toString()) }
    require(!required || !str.contains("\${")) { "One or many env variables are missing: $str" }
    return str
}

private fun String.substituteEnvVariable(key: String, value: String): String = this.replace("\${$key}", value)
