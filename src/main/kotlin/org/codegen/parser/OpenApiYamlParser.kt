package org.codegen.parser

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.decodeFromString
import org.codegen.parser.openapi.OpenApiConverter
import org.codegen.parser.openapi.Root
import org.codegen.schema.Document

internal class OpenApiYamlParser : AbstractParser() {
    override fun parse(content: String): Document {
        val format =
            Yaml(
                configuration =
                    YamlConfiguration(
                        strictMode = false,
                    ),
            )
        val root = format.decodeFromString<Root>(content)
        return OpenApiConverter(root).convertToDocument()
    }
}
