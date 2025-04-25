package org.codegen.parser

import kotlinx.serialization.json.Json
import org.codegen.parser.openapi.OpenApiConverter
import org.codegen.parser.openapi.Root
import org.codegen.schema.Document

internal class OpenApiJsonParser : AbstractParser() {
    override fun parse(content: String): Document {
        val format =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
        val root = format.decodeFromString<Root>(content)
        return OpenApiConverter(root).convertToDocument()
    }
}
