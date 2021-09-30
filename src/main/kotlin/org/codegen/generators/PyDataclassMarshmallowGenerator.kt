package org.codegen.generators

import org.codegen.dto.*
import org.codegen.extensions.*

class PyDataclassMarshmallowGenerator(proxy: AbstractCodeGenerator? = null) : PyDataclassGenerator(proxy) {

    override fun buildEntity(entity: Entity): String {
        val preLines = mutableListOf<String>()
        val className = buildEntityName(entity.name)
        val lines = mutableListOf<String>()
        val decorator = "\${DECORATOR_ARGS}".substituteEnvVars().let {
            if (it.isEmpty()) "@dataclass" else "@dataclass($it)"
        }

        if (entity.parent == null) {
            lines.add(decorator)
            lines.add("class ${className}:")
        } else {
            val parentClassName = buildEntityName(entity.parent)
            lines.add(decorator)
            lines.add("class ${className}(${parentClassName}):")
        }

        headers.add("from dataclasses import dataclass, field")
        definedNames.add(className)

        entity.description?.also {
            lines.add("    \"\"\"")
            lines.add("    " + entity.description)
            lines.add("    \"\"\"")
        }

        for (field in entity.fields) {
            val dtypeProps = getDtype(field.dtype)
            val fieldName = field.name.normalize().snakeCase()
            val attrs = dtypeProps.definitionArguments.toMutableMap()

            var definition = dtypeProps.definition

            if (field.default != UNSET) {
                when {
                    field.default == EMPTY_PLACEHOLDER -> {
                        attrs["default_factory"] = definition
                    }
                    field.default == null -> {
                        attrs["default"] = "None"
                    }
                    field.default.isNotEmpty() && "[{".contains(field.default[0]) -> {
                        // complex value (list/map/etc) should be inserted via function above class
                        val callableName = "default_$fieldName"
                        preLines.add("def $callableName():")
                        preLines.add("    return ${field.default}\n\n")
                        attrs["default_factory"] = callableName
                    }
                    else -> {
                        // simple value -> insert inline
                        attrs["default"] = dtypeProps.toGeneratedValue(field.default)
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
            }

            field.serializedName?.let {
                field.metadata["data_key"] = "\"$it\""
            }

            if (field.excludeFromSerialization) {
                field.metadata["load_only"] = "True"
            }

            field.enum?.let { enum ->
                val choicesPrefix = (field.enumPrefix ?: fieldName).snakeCase().uppercase()
                val choicesName = "${choicesPrefix}S"
                val choices = enum.keys.associate { key -> choicesPrefix + "_" + key.normalize().snakeCase().uppercase() to dtypeProps.toGeneratedValue(key) }

                definedNames.add(choicesName)
                choices.keys.forEach { definedNames.add(it) }

                val choicesDefinition = choices.map { entry -> "${entry.key} = ${entry.value}" }.joinToString(separator = "\n")
                preLines.add("$choicesDefinition\n$choicesName = [${choices.keys.joinToString()}]\n\n")

                headers.add("from marshmallow import fields as marshmallow_fields")
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

    private fun buildValidator(validator: Validator, entity: Entity): String {
        headers.add("import marshmallow")
        return validator.conditions
            .map { buildExpression(it, entity) }
            .joinToString("\n  ") { "if not($it):\n    raise marshmallow.ValidationError(\"${validator.message}\")" }
    }

}
