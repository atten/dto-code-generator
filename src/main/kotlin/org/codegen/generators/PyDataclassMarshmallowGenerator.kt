package org.codegen.generators

import org.codegen.dto.*
import org.codegen.extensions.*

class PyDataclassMarshmallowGenerator(proxy: AbstractCodeGenerator? = null) : PyDataclassGenerator(AllGeneratorsEnum.PY_MARSHMALLOW_DATACLASS, proxy) {

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

        headers.add("from dataclasses import dataclass")
        headers.add("from dataclasses import field")

        entity.description?.also {
            lines.add("    \"\"\"")
            lines.add("    " + entity.description)
            lines.add("    \"\"\"")
        }

        for (field in entity.fieldsSortedByDefaults) {
            val dtypeProps = getDtype(field.dtype)
            val fieldName = field.name.normalize().snakeCase()
            val attrs = dtypeProps.definitionArguments.toMutableMap()
            val fieldMetadata = field.metadata.toMutableMap()

            var definition = dtypeProps.definition

            if (field.default != UNSET) {
                when {
                    field.default == EMPTY_PLACEHOLDER -> {
                        attrs["default_factory"] = if (field.multiple) "list" else definition
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
                fieldMetadata["allow_none"] = "True"
            }

            field.serializedName?.let {
                fieldMetadata["data_key"] = "\"$it\""
            }

            if (field.excludeFromSerialization) {
                fieldMetadata["load_only"] = "True"
            }

            field.enum?.let { enum ->
                val choicesPrefix = (field.enumPrefix ?: fieldName).snakeCase().uppercase()
                val choicesName = "${choicesPrefix}S"
                val choices = enum.keys.associate { key -> buildChoiceVariableName(field, key) to dtypeProps.toGeneratedValue(key) }

                val choicesDefinition = choices.map { entry -> "${entry.key} = ${entry.value}" }.joinToString(separator = "\n")
                addDefinition("$choicesDefinition\n$choicesName = [${choices.keys.joinToString()}]", choicesName, *choices.keys.toTypedArray())

                fieldMetadata["validate"] = "[marshmallow.fields.validate.OneOf($choicesName)]"
            }

            if (field.multiple) {
                val metadata = attrs["metadata"]
                if (metadata != null && "marshmallow_field" in metadata) {
                    // redefine marshmallow field in metadata (preserve attributes of original nested element)
                    attrs["metadata"] = metadata.replace("marshmallow_field=", "marshmallow_field=marshmallow.fields.List(") + ")"
                } else if (fieldMetadata.isNotEmpty()) {
                    headers.add("import marshmallow_dataclass")
                    attrs["metadata"] = "dict(marshmallow_field=marshmallow.fields.List(marshmallow.fields.Nested(marshmallow_dataclass.class_schema($definition)), {metadata}))"
                }

                definition = "t.List[$definition]"
            }

            // if field contains metadata, make "arg1=..., arg2=..." notation and replace "{metadata}" placeholder with it.
            val metaString = fieldMetadata.map { entry -> "${entry.key.normalize().snakeCase()}=${entry.value}" }.joinToString()
            attrs.forEach { entry ->
                if ("{metadata}" in entry.value) {
                    attrs[entry.key] = entry.value.replace("{metadata}", metaString)
                }
            }

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
            .joinToString("\n  ") { "if not($it):\n    raise marshmallow.ValidationError('${validator.message.replace("'", "\\'")}')" }
    }

    private fun buildChoiceVariableName(field: Field, key: String): String {
        val choicesPrefix = (field.enumPrefix ?: field.name.normalize()).snakeCase().uppercase()
        return choicesPrefix + "_" + key.normalize().snakeCase().uppercase()
    }

    override fun buildPrimitive(key: String, entity: Entity, dataType: DataType?): String {
        // interpret "field|attribute" syntax
        if ("|" in key) {
            val (fieldName, attribute) = key.split('|', limit = 2)
            val field = entity.fields.find { it.name == fieldName }

            // detect field|enum_val and convert appropriately
            if (field?.enum?.contains(attribute) == true) {
                // add right indent
                return buildChoiceVariableName(field, attribute) + ' '
            } else if (field != null && attribute == "COUNT") {
                // array length detected
                val name = field.name.normalize().snakeCase()
                return "len(self.$name) "
            } else if (field != null && attribute == "UNIQUE_COUNT") {
                // set length detected
                val name = field.name.normalize().snakeCase()
                return "len(set(self.$name)) "
            } else if (field != null && attribute == "SORTED_ASC") {
                // sorted list detected
                val name = field.name.normalize().snakeCase()
                return "sorted(self.$name) "
            }
        }
        return super.buildPrimitive(key, entity, dataType)
    }

}
