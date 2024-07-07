package org.codegen.generators

import org.codegen.dto.*
import org.codegen.extensions.*

open class KtDataclassGenerator(includedEntityType: AllGeneratorsEnum, parent: AbstractCodeGenerator? = null) : AbstractCodeGenerator(
    KT_FORMAT_RULE,
    includedEntityType,
    parent
) {
    private val kotlinKeywords = setOf("open", "fun")  // incomplete and contains only known

    protected open fun buildFieldDefinition(field: Field): String {
        val dataType = getDtype(field.dtype)
        val definitionKeyword = "val"
        val fieldName = field.name.normalize().camelCase()
        val assignmentExpression = when (field.default) {
            UNSET -> ""
            null -> "null"
            else -> if (field.many) {
                if (field.default == EMPTY_PLACEHOLDER)
                    "listOf()"
                else
                    "listOf(${dataType.toGeneratedValue(field.default)})"
            } else {
                dataType.toGeneratedValue(field.default)
            }
        }
            .let { if (it.isEmpty()) "" else "= $it" }

        val typeName = dataType.definition
            .let { if (field.isEnum) (field.enumPrefix ?: field.name).camelCase().capitalize() else it }
            .let { if (field.nullable) "$it?" else it }
            .let { if (field.many) "List<$it>" else it }
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
            preLines.add(" */")
        }

        // include parent class fields (because data class inheritance is not allowed)
        val includedFields = entity.parent?.let { findEntity(it)?.fields } ?: listOf()

        for (field in entity.fieldsSortedByDefaults + includedFields) {
            val fullDefinition = buildFieldDefinition(field)

            field.enum?.let { enum ->
                val enumName = (field.enumPrefix ?: field.name).camelCase().capitalize()
                val enumBody = enum.map { row -> buildEnumItem(row.key) }.joinToString(
                    separator = "\n",
                    prefix = "enum class $enumName(val value: String) {\n",
                    postfix = "\n}\n"
                ) { "    ${it.replace("\n", "\n    ")}," }
                addDefinition(enumBody, enumName)
            }

            field.shortDescription?.let { lines.add("    // $it") }
            field.longDescription?.let { lines.add("    // $it") }
            lines.add("    ${fullDefinition.replace("\n", "\n    ")},")
        }

        lines.add(")")
        return (preLines + lines).joinToString("\n", postfix = "\n")
    }

    protected fun buildEnumLiteral(key: String) = key.snakeCase().uppercase()

    protected open fun buildEnumItem(key: String): String {
        val itemName = buildEnumLiteral(key)
        val literal = itemName.let { if (it in kotlinKeywords) "`$it`" else it }
        return "$literal(\"${key}\")"
    }
}