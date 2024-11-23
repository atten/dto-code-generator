package org.codegen.schema

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class SchemaParser {
    fun parse(content: String): Document {
        val format =
            Json {
                ignoreUnknownKeys = false
                isLenient = true
            }
        return format.decodeFromString<Document>(content)
    }
}
