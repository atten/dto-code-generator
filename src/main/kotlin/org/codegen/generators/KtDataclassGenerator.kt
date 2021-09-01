package org.codegen.generators

import org.codegen.dto.*
import org.codegen.extensions.*

open class KtDataclassGenerator(parent: AbstractCodeGenerator? = null) : AbstractCodeGenerator(
    KT_FORMAT_RULE,
    AllGeneratorsEnum.KT_DATACLASS,
    parent
) {
    private val kotlinKeywords = setOf("open", "fun")  // incomplete and contains only known

    protected open fun buildFieldDefinition(field: Field): String {
        val dataType = getDtype(field.dtype)
        val definitionKeyword = "val"
        val fieldName = field.name.normalize().camelCase()
        val assignmentExpression = if (field.default == UNSET) {
            ""
        } else {
            dataType.toGeneratedValue(field.default ?: "null")
        }
            .let { if (field.multiple) "listOf($it)" else it}
            .let { if (it.isEmpty()) "" else "= $it" }

        val typeName = dataType.definition
            .let { if (field.isEnum) (field.enumPrefix ?: field.name).camelCase().capitalize() else it }
            .let { if (field.nullable) "$it?" else it }
            .let { if (field.multiple) "List<$it>" else it }
        return "$definitionKeyword $fieldName: $typeName $assignmentExpression".trim()
    }

    override fun buildEntityName(name: String) = name.camelCase().capitalize()

    override fun buildEntity(entity: Entity) = buildEntity(entity, listOf())

    open fun buildEntity(entity: Entity, annotations: List<String>): String {
        val preLines = mutableListOf<String>()
        val className = buildEntityName(entity.name)
        val definition = "data class $className("
        val lines = annotations.toMutableList().also { it.add(definition) }

        entity.description?.also {
            preLines.add("/**")
            preLines.add(" * " + entity.description)
            preLines.add("*/")
        }

        // include parent class fields (because data class inheritance is not allowed)
        val includedFields = entity.parent?.let { findEntity(it)?.fields } ?: listOf()

        for (field in entity.fields + includedFields) {
            val fullDefinition = buildFieldDefinition(field)

            field.enum?.let { enum ->
                val enumName = (field.enumPrefix ?: field.name).camelCase().capitalize()
                val enumBody = enum.map { row -> "    ${row.key.camelCase().let { if (it in kotlinKeywords) "`$it`" else it }}(\"${row.key}\")," }.joinToString(
                    separator = "\n",
                    prefix = "enum class $enumName(val value: String) {\n",
                    postfix = "\n}\n"
                )
                addDefinition(enumBody)
            }

            field.shortDescription?.let { lines.add("    // $it") }
            field.longDescription?.let { lines.add("    // $it") }
            lines.add("    ${fullDefinition.replace("\n", "\n    ")},")
        }

        lines.add(")")
        return (preLines + lines).joinToString("\n", postfix = "\n")
    }
}