package org.codegen.extensions

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

fun String.camelCase() = this.normalize().split(' ').joinToString("") { it.capitalize() }.replaceFirstChar { it.lowercase() }

fun String.capitalize() = this.replaceFirstChar { it.uppercase() }

/**
 * "text_${SHELL}" -> "text_/bin/bash/"
 */
fun String.substituteEnvVars(): String {
    var str = this
    System.getenv().forEach { (key, value) -> str = str.replace("\${${key}}", value) }
    return str
}
