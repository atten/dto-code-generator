package org.codegen.generators

import org.codegen.dto.*
import org.codegen.extensions.*

open class KtDataclassCodeGenerator: AbstractCodeGenerator() {
    private val headers = mutableListOf<String>()

    protected fun addHeader(str: String) {
        if (!headers.contains(str))
            headers.add(str)
    }

    fun getHeaders() = headers

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
            .let { if (field.nullable) "$it?" else it }
            .let { if (field.multiple) "List<$it>" else it }
        return "$definitionKeyword $fieldName: $typeName $assignmentExpression".trim()
    }

    open fun buildEntity(entity: Entity, annotations: List<String> = listOf()): String {
        val preLines = mutableListOf<String>()
        val className = entity.name.camelCase().capitalize()
        val definition = "data class $className ("
        val lines = annotations.toMutableList().also { it.add(definition) }

        entity.description?.also {
            preLines.add("/**")
            preLines.add(" * " + entity.description)
            preLines.add("*/")
        }

        // include parent class fields (because data class inheritance is not allowed)
        val includedFields = entity.parent?.let { getIncludedEntity(it).fields } ?: listOf()

        for (field in entity.fields + includedFields) {
            val fullDefinition = buildFieldDefinition(field)

            field.shortDescription?.let { lines.add("    // $it") }
            field.longDescription?.let { lines.add("    // $it") }
            lines.add("    ${fullDefinition.replace("\n", "\n    ")},")

            // include headers
            getDtype(field.dtype).requiredHeaders.forEach { addHeader(it.substituteEnvVars()) }
        }

        lines.add(")")
        return (preLines + lines).joinToString("\n")
    }

    override fun build(): String {
        val builtEntities = getEntities(includeMethods = true).map { buildEntity(it) }
        val topHeader = "// Generated by DTO-Codegen\npackage \${PACKAGE_NAME}\n\n".substituteEnvVars()
        return headers.sorted().joinToString("\n", prefix = topHeader, postfix = "\n\n") +
                builtEntities.joinToString("\n\n\n", postfix = "\n")
    }
}