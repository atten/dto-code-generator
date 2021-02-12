package generators

import org.codegen.common.dto.DtypeAttributesMapping
import org.codegen.common.dto.Entity
import org.codegen.common.dto.toCamelCase
import org.codegen.common.dto.toSnakeCase

class PyDataclassCodeGenerator : CodeGeneratorInterface {
    private val dtypeAttrs = mutableMapOf<String, DtypeAttributesMapping>()
    private val entities = mutableListOf<Entity>()

    private fun buildEntity(entity: Entity): String {
        val lines = mutableListOf(
            "@dataclass",
            "class ${entity.name.toCamelCase()}:",
        )

        entity.description?.also {
            lines.add("    \"\"\"")
            lines.add("    " + entity.description)
            lines.add("    \"\"\"")
        }

        for (field in entity.fields) {
            val dtypeProps = dtypeAttrs[field.dtype]
            val fieldName = field.name.toSnakeCase()
            val attrs = mutableMapOf<String, String>()

            requireNotNull(dtypeProps) {"Missing extension for dtype '${field.dtype}'"}

            var definition = dtypeProps.definition

            field.default?.let { raw ->
                dtypeProps.toGeneratedValue(raw).also {
                    if ("[".contains(it[0])) {
                        // complex value (list/map/etc) should be inserted via function above class
                        val callableName = "default_$fieldName"
                        lines.add(0, "def $callableName():")
                        lines.add(1, "    return $it\n\n")
                        attrs["default_factory"] = callableName
                    } else {
                        // simple value -> insert inline
                        attrs["default"] = it
                    }
                }
            }

            field.longDescription?.let {
                lines.add("    # $it")
            }

            if (field.nullable)
                definition = "t.Optional[$definition]"
                if (field.default == null)
                    attrs["default"] = "None"

            val expression = if (attrs.size == 1 && attrs.containsKey("default")) {
                // = defaultValue
                attrs.getValue("default")
            } else {
                // = field(param1=..., param2=...)
                val attrsString = attrs.map { entry -> "${entry.key}=${entry.value}" }.joinToString()
                "field($attrsString)"
            }

            lines.add("    ${field.name.toSnakeCase()}: $definition = $expression")
        }

        return lines.joinToString("\n")
    }

    override fun addEntity(entity: Entity) {
        if (!entities.contains(entity))
            entities.add(entity)
    }

    override fun addDtypeExtension(dtype: String, attrs: DtypeAttributesMapping) {
        dtypeAttrs[dtype] = attrs
    }

    override fun build(): String {
        val includes = mutableListOf(
            "from dataclasses import dataclass, field",
            "import typing as t",
        )

        return includes.joinToString("\n", postfix = "\n\n\n") +
                entities.joinToString("\n\n\n", postfix = "\n", transform = { buildEntity(it)})
    }
}