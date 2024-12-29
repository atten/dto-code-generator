package org.codegen.format

internal fun String.normalize(): String {
    require(this.isNotEmpty()) { "String can't be empty" }
    require(this[0].isLetter() or this[0].isWhitespace()) { "String must start with letter: '$this'" }

    var cleaned = this.map { if (it.isLetterOrDigit()) it else ' ' }.joinToString("")
    cleaned = cleaned.trim()
    require(cleaned.isNotEmpty()) { "Normalized string can't be empty: $this" }

    while (cleaned.contains("  ")) {
        cleaned = cleaned.replace("  ", " ")
    }

    // Apply more complex rule for string with mixed case. Prepend uppercase char with space:
    // minValue -> min Value
    // MyDTO -> My dto
    cleaned = cleaned.zipWithNext { a, b ->
        if (a.isDigit() != b.isDigit()) {
            a.lowercase()
        } else if (a.isLowerCase() && !b.isLowerCase()) {
            "$a "
        } else {
            a.lowercase()
        }
    }.joinToString("") + cleaned.last().lowercase()

    while (cleaned.contains("  ")) {
        cleaned = cleaned.replace("  ", " ")
    }
    return cleaned
}

fun String.snakeCase() =
    this.normalize().let {
        it.zipWithNext {
                a, b ->
            when {
                (!a.isUpperCase() && b == ' ') -> "${a}_"
                a == ' ' -> ""
                else -> a
            }
        }.joinToString("") + it.last()
    }

fun String.camelCase() = this.normalize().split(' ').joinToString("") { it.capitalizeFirst() }

fun String.capitalizeFirst() = this.replaceFirstChar { it.uppercase() }

fun String.lowercaseFirst() = this.replaceFirstChar { it.lowercase() }

fun String.pluralize() =
    when {
        this.endsWith("ES") -> this
        this.endsWith("es") -> this
        this.endsWith("S") -> "${this}ES"
        this.endsWith("s") -> "${this}es"
        this.endsWith("X") -> "${this}ES"
        this.endsWith("x") -> "${this}es"
        this.endsWith("y") -> this.substring(0, this.length - 1) + "ies"
        this.endsWith("Y") -> this.substring(0, this.length - 1) + "IES"
        this.last().isUpperCase() -> "${this}S"
        else -> "${this}s"
    }
