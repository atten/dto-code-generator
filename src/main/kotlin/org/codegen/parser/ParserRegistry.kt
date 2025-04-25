package org.codegen.parser

import kotlinx.serialization.SerializationException
import org.codegen.schema.Document
import java.io.File
import java.text.ParseException
import java.util.*

object ParserRegistry {
    private val parsers =
        listOf(
            SchemaJsonParser(),
            OpenApiJsonParser(),
            OpenApiYamlParser(),
        )

    fun parseFile(path: String): Document {
        try {
            return parse(File(path).readText())
        } catch (e: ParseException) {
            throw ParseException("Failed to parse '$path'.\n${e.message}", e.errorOffset)
        }
    }

    fun parse(content: String): Document {
        val exceptions = mutableMapOf<String, Exception>()

        for (parser in parsers) {
            try {
                return parser.parse(content)
            } catch (e: SerializationException) {
                exceptions[parser.javaClass.name] = e
            }
        }

        val parserNames = exceptions.keys.joinToString(", ")
        val errorText = StringJoiner("\n")
        errorText.add("Parsers tried: $parserNames. Corresponding exceptions below:")
        exceptions.forEach { (name, exception) -> errorText.add("$name: $exception") }
        throw ParseException(errorText.toString(), 0)
    }
}
