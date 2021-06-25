package generators

import org.codegen.common.dto.*
import java.util.*

class DjangoModelCodeGenerator: CodeGeneratorInterface {
    private val dtypeAttrs = mutableMapOf<String, DtypeAttributesMapping>()
    private val entities = mutableListOf<Entity>()
    private val headers = mutableListOf(
        "# Generated by DTO-Codegen on ${Date()}",
        "",
        "from django.db import models",
        "from django.utils.translation import gettext_lazy as _",
    )
    private val plainDataTypes = listOf("bool", "int", "float", "str")


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
        val lines = mutableListOf("class ${entity.name.camelCase()}(models.Model):")

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

            dtypeProps.requiredHeaders.forEach { addHeader(it) }

            field.default?.let { raw ->
                dtypeProps.toGeneratedValue(raw).also {
                    if ("[".contains(it[0])) {
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

    override fun build(): String {
        val builtEntities = entities.map { buildEntity(it) }
        val entitiesList = entities.joinToString(",\n", "\n\nGENERATED_MODELS = [\n", "\n]") { "    " + it.name.camelCase() }

        return headers.joinToString("\n", postfix = "\n\n\n") +
                builtEntities.joinToString("\n\n\n", postfix = "\n") + entitiesList
    }
}