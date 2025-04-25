package org.codegen.schema.openapi

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.codegen.schema.Document

class OpenApiJsonParser {
    fun parse(content: String): Document {
        val format =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
        val root = format.decodeFromString<Root>(content)
        return OpenApiConverter(root).convertToDocument()
    }
}
