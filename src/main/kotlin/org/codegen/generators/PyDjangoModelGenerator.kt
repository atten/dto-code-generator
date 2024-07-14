package org.codegen.generators

import org.codegen.format.CodeFormatRules
import org.codegen.format.camelCase
import org.codegen.format.snakeCase
import org.codegen.schema.Constants.Companion.EMPTY
import org.codegen.schema.Constants.Companion.UNSET
import org.codegen.schema.Entity

class PyDjangoModelGenerator(proxy: AbstractCodeGenerator? = null) : AbstractCodeGenerator(CodeFormatRules.PYTHON, AllGeneratorsEnum.PY_DJANGO_MODEL, proxy) {
    private val plainDataTypes = listOf("bool", "int", "float", "str")

    override fun buildEntityName(name: String) = name.camelCase()

    override fun buildEntity(entity: Entity): String {
        val preLines = mutableListOf<String>()
        val className = buildEntityName(entity.name)
        val lines = mutableListOf("class $className(models.Model):")

        entity.description?.also {
            lines.add("    \"\"\"")
            lines.add("    " + entity.description)
            lines.add("    \"\"\"")
        }

        for (field in entity.fields) {
            val dtypeProps = getDtype(field.dtype)
            val fieldName = field.name.snakeCase()
            val attrs = dtypeProps.definitionArguments.toMutableMap()
            val isScalar = plainDataTypes.contains(field.dtype)

            if (field.default != UNSET) {
                when {
                    field.default == EMPTY -> {
                        attrs["default"] = if (field.many) "list" else dtypeProps.definition
                    }
                    field.default == null -> {
                        attrs["default"] = "None"
                    }
                    field.default.isNotEmpty() && "[{".contains(field.default[0]) -> {
                        // complex value (list/map/etc) should be inserted via function above class
                        val callableName = "default_$fieldName"
                        val defaultValue = dtypeProps.toGeneratedValue(field.default)
                        preLines.add("def $callableName():")
                        preLines.add("    return $defaultValue\n\n")
                        attrs["default"] = callableName
                    }
                    else -> {
                        // simple value -> insert inline
                        attrs["default"] = dtypeProps.toGeneratedValue(field.default)
                    }
                }

                // 'blank' flag is required for non-scalar data types with default value
                if (!isScalar || field.many)
                    attrs["blank"] = "True"
            }

            if (field.nullable) {
                attrs["blank"] = "True"
                attrs["null"] = "True"
                // redundant default argument
                if (field.default == null)
                    attrs.remove("default")
            }

            val fieldClass = when {
                field.many && isScalar -> "ArrayField".also {
                    headers.add("from django.contrib.postgres.fields import ArrayField")
                    attrs["base_field"] = dtypeProps.definition + "()"
                }
                field.many && !isScalar -> "models.JSONField" // use json field for multiple non-scalar values
                else -> dtypeProps.definition
            }

            field.enum?.let {
                val subclass = "models.TextChoices"
                val enumName = (field.enumPrefix ?: field.name).camelCase()
                val choicesDefinition = it.map { entry -> "    ${entry.key.snakeCase().uppercase()} = '${entry.key}', _('${entry.value}')" }.joinToString(
                    separator = "\n",
                    prefix = "class $enumName($subclass):\n",
                    postfix = "\n\n"
                )
                preLines.add(choicesDefinition)

                attrs["choices"] = "$enumName.choices"
                attrs["max_length"] = it.keys.map { s -> s.length }.maxOrNull().toString()
            }

            field.shortDescription?.let {
                attrs["verbose_name"] = "_('${it.replace("'", "\\'")}')"
            }

            field.longDescription?.let {
                attrs["help_text"] = "_('${it.replace("'", "\\'")}')"
            }

            val attrsString = attrs.map { entry -> "${entry.key}=${entry.value}" }.joinToString()
            lines.add("    $fieldName = $fieldClass($attrsString)")
        }

        if (!entity.prefix.isNullOrEmpty())
            lines.add("\n    PREFIX = '${entity.actualPrefix.snakeCase()}'")

        // add Meta section
        lines.add("")
        lines.add("    class Meta:")
        lines.add("        abstract = True")

        return (preLines + lines).joinToString("\n")
    }

    override fun buildBodyPrefix(): String {
        headers.add("from django.db import models")
        headers.add("from django.utils.translation import gettext_lazy as _")
        return ""
    }

    override fun buildBodyPostfix(): String {
        entities
            .map { buildEntityName(it.name) }
            .sorted()
            .joinToString(",\n", "GENERATED_MODELS = [\n", "\n]") { "    $it" }
            .also { addDefinition(it, "GENERATED_MODELS") }
        return "\n"
    }
}
