package org.codegen.generators

import org.codegen.dto.*
import org.codegen.extensions.*

class KtKtormTableCodeGenerator: AbstractCodeGenerator() {
    private val headers = mutableListOf(
        "// Generated by DTO-Codegen",
        "package \${PACKAGE_NAME}".substituteEnvVars(),
        "",
        "import org.ktorm.dsl.QueryRowSet",
        "import org.ktorm.schema.*",
    )

    private fun addHeader(str: String) {
        if (!headers.contains(str))
            headers.add(str)
    }

    private fun buildEntity(entity: Entity): String {
        val preLines = mutableListOf<String>()
        val baseClassName = entity.name.camelCase().capitalize()
        val tableName = entity.name.camelCase() + "s"

        // include parent class fields (because data class inheritance is not allowed)
        val includedFields = entity.parent?.let { getIncludedEntity(it).fields } ?: listOf()
        val definition = "object ${baseClassName}s : BaseTable<$baseClassName>(\"${tableName}\") {"
        val lines = mutableListOf(definition)
        val createEntityLines = mutableListOf<String>()

        entity.description?.also {
            preLines.add("/**")
            preLines.add(" * " + entity.description)
            preLines.add("*/")
        }

        for (field in entity.fields + includedFields) {
            val dtypeProps = getDtype(field.dtype)
            val definitionKeyword = "val"
            val fieldName = field.name.normalize().camelCase()
            val columnName = field.name.normalize().snakeCase()

            val typeName = dtypeProps.definition.let { if (field.nullable) "$it?" else it }

            val fullDefinition = "$definitionKeyword $fieldName = $typeName(\"${columnName}\")"

            field.shortDescription?.let {
                lines.add("    // $it")
            }

            field.longDescription?.let {
                lines.add("    // $it")
            }

            lines.add("    $fullDefinition")

            if (field.default == UNSET) {
                // add field mapper in case field is required
                val mapper = if (field.nullable) {
                    "$fieldName = row[$fieldName]"
                } else {
                    val valueIfNull = dtypeProps.definitionArguments["valueIfNull"]?.let { dtypeProps.toGeneratedValue(it) }
                    requireNotNull(valueIfNull)  { "Value definitionArguments.valueIfNull for dtype \"${field.dtype}\" is missing" }
                    "$fieldName = row[$fieldName] ?: $valueIfNull"
                }
                createEntityLines.add(mapper)
            }

            // include headers
            dtypeProps.requiredHeaders.forEach { addHeader(it.substituteEnvVars()) }
        }

        // add required overridden method
        lines.add("\n    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = $baseClassName(")
        lines.add(createEntityLines.joinToString(separator = "\n") { "        $it," })
        lines.add("    )")

        // closing class brace
        lines.add("}")

        return (preLines + lines).joinToString("\n")
    }

    override fun build(): String {
        val builtEntities = getEntities().map { buildEntity(it) }

        return headers.joinToString("\n", postfix = "\n\n") +
                builtEntities.joinToString("\n\n\n", postfix = "\n")
    }
}