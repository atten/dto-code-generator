package org.codegen.generators

import org.codegen.format.*
import org.codegen.schema.Constants.Companion.EMPTY
import org.codegen.schema.Constants.Companion.UNSET
import org.codegen.schema.DataType
import org.codegen.schema.Entity
import org.codegen.schema.Property
import org.codegen.utils.EnvironmentUtils.Companion.getEnvVariable
import java.util.*
import kotlin.jvm.optionals.getOrNull

open class PyDataclassGenerator(includedEntityType: AllGeneratorsEnum = AllGeneratorsEnum.PY_DATACLASS, proxy: AbstractCodeGenerator? = null) : AbstractCodeGenerator(
    CodeFormatRules.PYTHON,
    includedEntityType,
    proxy,
) {
    // list of __all__ items
    protected val definedNames = mutableListOf<String>()

    override fun addCodePart(
        body: String,
        vararg names: String,
    ) {
        super.addCodePart(body, *names)
        // add missing names into __all__
        names
            .filter { it.isNotEmpty() && it !in definedNames }
            .forEach { definedNames.add(it) }
    }

    override fun renderEntityName(name: String) = name.camelCase()

    override fun renderEntity(entity: Entity): String {
        val preLines = mutableListOf<String>()
        val className = renderEntityName(entity.name)
        val lines = mutableListOf<String>()
        val decorator =
            getEnvVariable("DECORATOR_ARGS").getOrNull().let {
                if (it == null) "@dataclass" else "@dataclass($it)"
            }

        if (entity.parent == null) {
            lines.add(decorator)
            lines.add("class $className:")
        } else {
            val parentClassName = entity.parent.camelCase()
            lines.add(decorator)
            lines.add("class $className($parentClassName):")
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
            val fieldName = field.name.snakeCase()
            val attrs = dtypeProps.definitionArguments.toMutableMap()

            var definition = dtypeProps.definition

            if (field.many) {
                definition = "list[$definition]"
            }

            if (field.default != UNSET) {
                when {
                    field.default == EMPTY -> {
                        attrs["default_factory"] = if (field.many) "list" else definition
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

            field.description?.let {
                lines.add("    # $it")
            }

            field.longDescription?.let {
                lines.add("    # $it")
            }

            if (field.nullable) {
                headers.add("import typing as t")
                definition = "t.Optional[$definition]"
            }

            // if field contains metadata, make "arg1=..., arg2=..." notation and replace "{metadata}" placeholder with it.
            val metaString = field.metadata.map { entry -> "${entry.key.snakeCase()}=${entry.value}" }.joinToString()
            attrs.forEach { entry -> attrs[entry.key] = entry.value.replace("{metadata}", metaString) }

            // include metadata into definition
            field.metadata.forEach { (key, value) -> attrs[key.snakeCase()] = value }

            val expression =
                if (attrs.size == 1 && attrs.containsKey("default")) {
                    // = defaultValue
                    attrs.getValue("default")
                } else {
                    // = field(param1=..., param2=...)
                    val attrsString = attrs.map { entry -> "${entry.key}=${entry.value}" }.joinToString()
                    "field($attrsString)"
                }

            lines.add("    $fieldName: $definition = $expression")
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

    protected fun buildProperty(
        property: Property,
        entity: Entity,
    ): String {
        val methodName = property.name.snakeCase()
        val annotation = if (property.description.isEmpty()) "" else "\"\"\"${property.description}\"\"\"\n    "
        val dataType = getDtype(property.dtype)
        val returnName = dataType.definition
        val expression =
            buildExpression(property.expression, entity, dataType).let {
                if ("\n" in it) {
                    // contains if-else
                    it.replace(":\n   ", ":\n    return")
                } else {
                    // one-liner
                    "return $it"
                }
            }
                .replace("\n", "\n    ")
        return "@property\ndef $methodName(self) -> $returnName:\n    ${annotation}$expression"
    }

    protected fun buildExpression(
        primitives: List<String>,
        entity: Entity,
        dataType: DataType? = null,
    ) = primitives
        .joinToString("") { buildPrimitive(it, entity, dataType) }
        // remove redundant spaces
        .replace(" )", ")")
        .replace(" :", ":")
        .replace(" \n", "\n")
        .trim()

    protected open fun buildPrimitive(
        key: String,
        entity: Entity,
        dataType: DataType? = null,
    ): String =
        when {
            key == "IF" -> "if "
            key == "THEN" -> ":\n    "
            key == "ELSE" -> "\nelse:\n    "
            key == "OR" -> "or "
            key == "AND" -> "and "
            key == "NOT" -> "not "
            key == "NOW" -> "now()"
            key == "ABS" -> "abs"
            key.length == 1 && "-/*)".contains(key) -> "$key "
            key.length == 1 && "(".contains(key) -> key
            key.first().category in listOf(CharCategory.MATH_SYMBOL) -> "$key "
            // wrap positive and negative numbers with dataType template (if defined)
            key.trimStart('-').first().isDigit() -> (dataType?.toGeneratedValue(key) ?: key) + ' '
            key in entity.attributeNames -> "self.${key.snakeCase()} "
            else -> throw RuntimeException("Unrecognized primitive: $key (${key.first().category})")
        }

    override fun renderBodySuffix(): String {
        val suffix = StringJoiner(CodeFormatRules.PYTHON.entitiesSeparator, CodeFormatRules.PYTHON.entitiesSeparator, "\n")
        definedNames
            .sorted()
            .joinToString("\n", "__all__ = [\n", "\n]") { "    \"${it}\"," }
            .also { suffix.add(it) }
        return suffix.toString()
    }
}
