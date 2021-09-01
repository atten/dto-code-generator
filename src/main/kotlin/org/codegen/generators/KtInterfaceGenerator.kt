package org.codegen.generators

import org.codegen.dto.*
import org.codegen.extensions.*

class KtInterfaceGenerator(proxy: AbstractCodeGenerator? = null) : AbstractCodeGenerator(KT_FORMAT_RULE, AllGeneratorsEnum.KT_SERIALIZABLE_DATACLASS, proxy) {

    private fun buildMethod(method: Method): String {
        val name = method.name.camelCase()
        val returnDtypeProps = getDtype(method.dtype)
        val returnStatement = returnDtypeProps.definition
            .let { if (method.multiple) "List<$it>" else it }
            .let { if (method.nullable) "$it?" else it }
            .let { if (it == "Unit") "" else ": $it" }
        val arguments = mutableListOf<String>()

        for (argument in method.arguments) {
            val argName = argument.name.camelCase()
            val dtypeProps = getDtype(argument.dtype)
            val argTypeName = dtypeProps.definition
                .let { if (argument.isEnum) argument.name.snakeCase().capitalize() else it }
                .let { if (argument.multiple) "List<$it>" else it }
                .let { if (argument.nullable) "$it?" else it }
            val argDefaultValue = if (argument.default == UNSET) {
                ""
            } else {
                dtypeProps.toGeneratedValue(argument.default ?: "null")
            }
                .let { if (it.isEmpty()) "" else "= $it" }

            val argumentString = "${argName}: $argTypeName $argDefaultValue".trim()
            arguments.add(argumentString)
        }

        // include enums (if present among method arguments)
        KtDataclassGenerator(this).buildEntity(method.toEntity())

        val argumentsString = arguments.joinToString(separator = ", ")
        val definition = "fun ${name}($argumentsString)$returnStatement"
        val lines = mutableListOf<String>()

        method.description?.let {
            lines.add("/**\n * ${it.replace("\n", "\n * ")}\n */")
        }
        lines.add(definition)
        return lines.joinToString(separator = "\n")
    }

    override fun buildEntityName(name: String) = name.camelCase().capitalize()

    override fun buildEntity(entity: Entity): String {
        // either build an interface or regular DTO
        if (entity.fields.isEmpty())
            return buildInterfaceEntity(entity)
        return KtDataclassSerializableGenerator(this).buildEntity(entity)
    }

    private fun buildInterfaceEntity(entity: Entity): String {
        val entityName = buildEntityName(entity.name)
        val interfaceDefinition = "interface $entityName {"
        val builtMethods = entity.methodsPlusEndpoints().map { buildMethod(it) }
        return builtMethods.joinToString(separator = "\n\n", prefix = "${interfaceDefinition}\n", postfix = "\n}\n") {"    ${it.replace("\n", "\n    ")}"}
    }
}