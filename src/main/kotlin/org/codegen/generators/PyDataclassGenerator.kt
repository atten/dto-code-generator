package org.codegen.generators

import org.codegen.dto.*
import org.codegen.extensions.*
import java.lang.RuntimeException

open class PyDataclassGenerator(proxy: AbstractCodeGenerator? = null) : AbstractCodeGenerator(PY_FORMAT_RULE, AllGeneratorsEnum.PY_DATACLASS, proxy) {
    // list if __all__ items
    protected val definedNames = mutableListOf<String>()

    override fun addDefinition(body: String, name: String) {
        super.addDefinition(body, name)
        if (name.isNotEmpty() && name !in definedNames)
            definedNames.add(name)
    }

    override fun buildEntityName(name: String) = name.camelCase().capitalize()

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
            val parentClassName = entity.parent.camelCase().capitalize()
            lines.add(decorator)
            lines.add("class ${className}(${parentClassName}):")
        }

        addHeader("from dataclasses import dataclass, field")

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

        entity.properties
            .map { buildProperty(it, entity) }
            .map { it.replace("\n", "\n    ") }
            .forEach {
                lines.add("")
                lines.add("    $it")
            }

        return (preLines + lines).joinToString("\n")
    }

    protected fun buildProperty(property: Property, entity: Entity): String {
        val methodName = property.name.normalize().snakeCase()
        val returnName = getDtype(property.dtype).definition
        val expression = buildExpression(property.expression, entity)
        return "@property\ndef ${methodName}(self) -> $returnName:\n    return $expression"
    }

    protected fun buildExpression(primitives: List<String>, entity: Entity) = primitives.joinToString(" ") { buildPrimitive(it, entity) }

    private fun buildPrimitive(key: String, entity: Entity): String = when {
        key == "OR" -> "or"
        key == "AND" -> "and"
        key == "NOT" -> "not"
        key == "NOW" -> "now()"
        key.length == 1 && "-/*".contains(key) -> key
        key.first().category == CharCategory.MATH_SYMBOL -> key
        key.first().isDigit() -> key
        key in entity.fieldNames -> "self.${key.normalize().snakeCase()}"
        else -> throw RuntimeException("Unrecognized primitive: $key")
    }

    override fun buildBodyPrefix(): String {
        addHeader("import typing as t")
        return ""
    }

    override fun buildBodyPostfix(): String {
        definedNames
            .joinToString("\n", "__all__ = [\n", "\n]") { "    \"${it}\"," }
            .also { addDefinition(it, "__all__") }
        return "\n"
    }

}