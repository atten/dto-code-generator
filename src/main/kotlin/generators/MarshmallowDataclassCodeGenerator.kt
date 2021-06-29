package generators

import org.codegen.common.dto.*
import java.lang.RuntimeException
import java.util.*

class MarshmallowDataclassCodeGenerator : CodeGeneratorInterface {
    private val dtypeAttrs = mutableMapOf<String, DtypeAttributesMapping>()
    private val entities = mutableListOf<Entity>()
    private val headers = mutableListOf(
        "# Generated by DTO-Codegen on ${Date()}\n",
        "from dataclasses import dataclass, field",
        "from marshmallow import fields as marshmallow_fields",
        "import typing as t",
    )
    private val definedNames = mutableListOf<String>()

    override fun addEntity(entity: Entity) {
        if (!entities.contains(entity))
            entities.add(entity)
    }

    override fun addDtypeExtension(dtype: String, attrs: DtypeAttributesMapping) {
        dtypeAttrs[dtype] = attrs
    }

    private fun addHeader(str: String) {
        if (!headers.contains(str))
            headers.add(str)
    }

    private fun buildEntity(entity: Entity): String {
        val preLines = mutableListOf<String>()
        val className = entity.name.camelCase()
        val lines = mutableListOf<String>()

        if (entity.parent == null) {
            lines.add("@dataclass")
            lines.add("class ${className}:")
        } else {
            val parentClassName = entity.parent.camelCase()
            lines.add("@dataclass")
            lines.add("class ${className}(${parentClassName}):")
        }

        definedNames.add(className)

        entity.description?.also {
            lines.add("    \"\"\"")
            lines.add("    " + entity.description)
            lines.add("    \"\"\"")
        }

        for (field in entity.fields) {
            val dtypeProps = dtypeAttrs[field.dtype]
            val fieldName = field.name.normalize().snakeCase()

            requireNotNull(dtypeProps) {"Missing extension for dtype '${field.dtype}'"}

            val attrs = dtypeProps.definitionArguments.toMutableMap()

            dtypeProps.requiredHeaders.forEach { addHeader(it.substituteEnvVars()) }

            var definition = dtypeProps.definition

            field.default?.let { raw ->
                dtypeProps.toGeneratedValue(raw).also {
                    if ("[".contains(it[0])) {
                        // complex value (list/map/etc) should be inserted via function above class
                        val callableName = "default_$fieldName"
                        preLines.add("def $callableName():")
                        preLines.add("    return $it\n\n")
                        attrs["default_factory"] = callableName
                    } else {
                        // simple value -> insert inline
                        attrs["default"] = it
                    }
                }
            }

            field.shortDescription?.let {
                lines.add("    # $it")
            }

            field.longDescription?.let {
                lines.add("    # $it")
            }

            if (field.nullable) {
                definition = "t.Optional[$definition]"

                field.metadata["allow_none"] = "True"

                if (field.default == null)
                    attrs["default"] = "None"
            }

            field.enum?.let { enum ->
                val choicesPrefix = (field.enumPrefix ?: fieldName).snakeCase().uppercase()
                val choicesName = "${choicesPrefix}S"
                val choices = enum.keys.associate { key -> choicesPrefix + "_" + key.normalize().snakeCase().uppercase() to dtypeProps.toGeneratedValue(key) }

                definedNames.add(choicesName)
                choices.keys.forEach { definedNames.add(it) }

                val choicesDefinition = choices.map { entry -> "${entry.key} = ${entry.value}" }.joinToString(separator = "\n")
                preLines.add("$choicesDefinition\n$choicesName = [${choices.keys.joinToString()}]\n\n")

                field.metadata["validate"] = "[marshmallow_fields.validate.OneOf($choicesName)]"
            }

            // if field contains metadata, make "arg1=..., arg2=..." notation and replace "{metadata}" placeholder with it.
            val metaString = field.metadata.map { entry -> "${entry.key.normalize().snakeCase()}=${entry.value}" }.joinToString()
            attrs.forEach { entry -> attrs[entry.key] = entry.value.replace("{metadata}", metaString) }

            // include metadata into definition if needed
            if (dtypeProps.includeMetadataIntoDefinition)
                field.metadata.forEach { (key, value) -> attrs[key.snakeCase()] = value  }

            val expression = if (attrs.size == 1 && attrs.containsKey("default")) {
                // = defaultValue
                attrs.getValue("default")
            } else {
                // = field(param1=..., param2=...)
                val attrsString = attrs.map { entry -> "${entry.key}=${entry.value}" }.joinToString()
                "field($attrsString)"
            }

            lines.add("    ${fieldName}: $definition = $expression")
        }

        if (entity.validators.isNotEmpty()) {
            lines.add("")
            lines.add("    def __post_init__(self):")
        }

        entity.validators
            .map { buildValidator(it, entity)}
            .map { it.replace("\n", "\n        ") }
            .forEach { lines.add("        $it") }

        entity.properties
            .map { buildProperty(it, entity) }
            .map { it.replace("\n", "\n    ") }
            .forEach {
                lines.add("")
                lines.add("    $it")
            }

        return (preLines + lines).joinToString("\n")
    }

    private fun buildProperty(property: Property, entity: Entity): String {
        val methodName = property.name.normalize().snakeCase()
        val returnName = dtypeAttrs.getValue(property.dtype).definition
        val expression = buildExpression(property.expression, entity)
        return "@property\ndef ${methodName}(self) -> $returnName:\n    return $expression"
    }

    private fun buildValidator(validator: Validator, entity: Entity): String {
        addHeader("import marshmallow")
        return validator.conditions
            .map { buildExpression(it, entity) }
            .joinToString("\n  ") { "if not($it):\n    raise marshmallow.ValidationError(\"${validator.message}\")" }
    }

    private fun buildExpression(primitives: List<String>, entity: Entity) = primitives.joinToString(" ") { buildPrimitive(it, entity) }

    private fun buildPrimitive(key: String, entity: Entity): String = when {
        key == "OR" -> "or"
        key == "AND" -> "and"
        key == "NOT" -> "not"
        key == "NOW" -> "now()"
        key == "-" -> "-"
        key.first().category == CharCategory.MATH_SYMBOL -> key
        key.first().isDigit() -> key
        key in entity.fieldNames -> "self.${key.normalize().snakeCase()}"
        else -> throw RuntimeException("Unrecognized primitive: $key")
    }

    override fun build(): String {
        val builtEntities = entities.map { buildEntity(it) }
        val allDefinitions = definedNames.joinToString(",\n", "\n\n__all__ = [\n", "\n]") { "    \"${it}\"" }

        return headers.sorted().joinToString("\n", postfix = "\n\n\n") +
                builtEntities.joinToString("\n\n\n", postfix = "\n") + allDefinitions
    }
}
