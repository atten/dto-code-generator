package org.codegen.parser

import kotlinx.serialization.SerializationException
import org.codegen.schema.Document
import org.codegen.utils.Reader
import java.text.ParseException
import java.util.*

object ParserRegistry {
    private val parsers =
        listOf(
            SchemaJsonParser(),
            OpenApiJsonParser(),
            OpenApiYamlParser(),
        )

    fun parseFileOrResourceOrUrl(pathOrUrl: String): Document {
        try {
            return parseContent(Reader.readFileOrResourceOrUrl(pathOrUrl))
        } catch (e: ParseException) {
            throw ParseException("Failed to parse '$pathOrUrl'.\n${e.message}", e.errorOffset)
        }
    }

    private fun parseContent(content: String): Document {
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
