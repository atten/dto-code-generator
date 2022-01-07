package org.codegen.generators

import org.codegen.dto.*
import org.codegen.extensions.*

class PyModelDjangoGenerator(proxy: AbstractCodeGenerator? = null) : AbstractCodeGenerator(PY_FORMAT_RULE, AllGeneratorsEnum.PY_DJANGO_MODEL, proxy) {
    private val plainDataTypes = listOf("bool", "int", "float", "str")

    override fun buildEntityName(name: String) = name.camelCase().capitalize()

    override fun buildEntity(entity: Entity): String {
        val preLines = mutableListOf<String>()
        val className = buildEntityName(entity.name)
        val lines = mutableListOf("class ${className}(models.Model):")

        entity.description?.also {
            lines.add("    \"\"\"")
            lines.add("    " + entity.description)
            lines.add("    \"\"\"")
        }

        for (field in entity.fields) {
            val dtypeProps = getDtype(field.dtype)
            val fieldName = field.name.normalize().snakeCase()
            val attrs = dtypeProps.definitionArguments.toMutableMap()

            if (field.default != UNSET) {
                dtypeProps.toGeneratedValue(field.default ?: "None").also {
                    if ("[{".contains(it[0])) {
                        // complex value (list/map/etc) should be inserted via function above class
                        val callableName = "default_$fieldName"
                        preLines.add("def $callableName():")
                        preLines.add("    return $it\n\n")
                        attrs["default"] = callableName
                    } else {
                        // simple value -> insert inline
                        attrs["default"] = it
                    }
                }

                // for complex data types with default value, 'blank' flag is required
                if (!plainDataTypes.contains(field.dtype))
                    attrs["blank"] = "True"
            }

            if (field.nullable) {
                attrs["blank"] = "True"
                attrs["null"] = "True"
                // redundant default argument
                if (field.default == null)
                    attrs.remove("default")
            }

            field.enum?.let {
                val subclass = "models.TextChoices"
                val enumName = (field.enumPrefix ?: field.name).camelCase().capitalize()
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
            lines.add("    $fieldName = ${dtypeProps.definition}($attrsString)")
        }

        if (entity.prefix != null && entity.prefix.isNotEmpty())
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
