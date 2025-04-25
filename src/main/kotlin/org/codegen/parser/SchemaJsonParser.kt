package org.codegen.parser

import kotlinx.serialization.json.Json
import org.codegen.schema.Document

internal class SchemaJsonParser : AbstractParser() {
    override fun parse(content: String): Document {
        val format =
            Json {
                ignoreUnknownKeys = false
                isLenient = true
            }
        return format.decodeFromString<Document>(content)
    }
}
