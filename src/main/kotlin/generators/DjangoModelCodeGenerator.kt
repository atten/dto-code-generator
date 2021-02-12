package generators

import org.codegen.common.dto.*


class DjangoModelCodeGenerator: CodeGeneratorInterface {
    private val dtypeAttrs = mutableMapOf<String, DtypeAttributesMapping>()
    private val entities = mutableListOf<Entity>()

    private fun buildEntity(entity: Entity): String {
        val lines = mutableListOf("class ${entity.name.toCamelCase()}(models.Model):")

        entity.description?.also {
            lines.add("    \"\"\"")
            lines.add("    " + entity.description)
            lines.add("    \"\"\"")
        }

        for (field in entity.fields) {
            val dtypeProps = dtypeAttrs[field.dtype]
            val attrs = mutableMapOf<String, String>()
            val fieldName = field.name.toSnakeCase()

            requireNotNull(dtypeProps) {"Missing extension for dtype '${field.dtype}'"}

            field.default?.let { raw ->
                dtypeProps.toGeneratedValue(raw).also {
                    if ("[".contains(it[0])) {
                        // complex value (list/map/etc) should be inserted via function above class
                        val callableName = "default_$fieldName"
                        lines.add(0, "def $callableName():")
                        lines.add(1, "    return $it\n\n")
                        attrs["default"] = callableName
                    } else {
                        // simple value -> insert inline
                        attrs["default"] = it
                    }
                }

                if (field.dtype != "bool")
                    attrs["blank"] = "True"
            }

            if (field.nullable) {
                attrs["blank"] = "True"
                attrs["null"] = "True"
            }

            if (field.shortDescription != null)
                attrs["verbose_name"] = "_('${field.shortDescription})'"

            if (field.longDescription != null)
                attrs["help_text"] = "_('${field.longDescription})'"

            field.metadata?.forEach { (key, value) -> attrs[key.toSnakeCase()] = value  }

            val attrsString = attrs.map { entry -> "${entry.key}=${entry.value}" }.joinToString()
            lines.add("    $fieldName = ${dtypeProps.definition}($attrsString)")
        }

        // add Meta section
        lines.add("")
        lines.add("    class Meta:")
        lines.add("        abstract=True")
        entity.prefix?.let { lines.add("        prefix='${entity.actualPrefix}'") }

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
            "from django.db import models",
            "from django.utils.translation import gettext_lazy as _",
        )

        return includes.joinToString("\n", postfix = "\n\n\n") +
                entities.joinToString("\n\n\n", postfix = "\n", transform = { buildEntity(it)})
    }
}