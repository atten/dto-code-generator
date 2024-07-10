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

    cleaned = if (cleaned.all { it.isUpperCase() }) {
        // transform uppercase string to lowercase
        cleaned.lowercase()
    } else {
        // Apply more complex rule for string with mixed case. Prepend uppercase char with space:
        // minValue -> min Value
        // MyDTO -> My D T O
        cleaned.zipWithNext { a, b ->
            if (b.isLetter() && !b.isLowerCase())
                "$a "
            else
                a.lowercase()
        }.joinToString("") + cleaned.last()
    }

    while (cleaned.contains("  ")) {
        cleaned = cleaned.replace("  ", " ")
    }
    return cleaned
}

fun String.snakeCase() = this.normalize().let {
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
