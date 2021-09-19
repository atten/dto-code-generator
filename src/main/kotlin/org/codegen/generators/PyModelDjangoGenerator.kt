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
                addHeader("from model_utils import Choices")

                val choicesName = (field.enumPrefix ?: fieldName).snakeCase().uppercase() + "_CHOICES"
                val choicesDefinition = it.map { entry -> "    ('${entry.key}', _('${entry.value}'))," }.joinToString(separator = "\n", prefix = "$choicesName = Choices(\n", postfix = "\n)\n\n")
                preLines.add(choicesDefinition)

                attrs["choices"] = choicesName
                attrs["max_length"] = it.keys.map { s -> s.length }.maxOrNull().toString()
            }

            field.shortDescription?.let {
                attrs["verbose_name"] = "_('${it.replace("'", "\\'")}')"
            }

            field.longDescription?.let {
                attrs["help_text"] = "_('${it.replace("'", "\\'")}')"
            }

            // include metadata into definition if needed
            if (dtypeProps.includeMetadataIntoDefinition)
                field.metadata.forEach { (key, value) -> attrs[key.normalize().snakeCase()] = value  }

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
        addHeader("from django.db import models")
        addHeader("from django.utils.translation import gettext_lazy as _")
        return ""
    }

    override fun buildBodyPostfix(): String {
        getEntities()
            .map { buildEntityName(it.name) }
            .joinToString(",\n", "GENERATED_MODELS = [\n", "\n]") { "    $it" }
            .also { addDefinition(it, "GENERATED_MODELS") }
        return "\n"
    }
}
