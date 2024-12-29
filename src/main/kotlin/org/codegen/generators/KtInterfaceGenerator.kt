package org.codegen.generators

import org.codegen.format.*
import org.codegen.schema.Constants.Companion.UNSET
import org.codegen.schema.Entity
import org.codegen.schema.Method

class KtInterfaceGenerator(proxy: AbstractCodeGenerator? = null) : AbstractCodeGenerator(
    CodeFormatRules.KOTLIN,
    AllGeneratorsEnum.KT_SERIALIZABLE_DATACLASS,
    proxy,
) {
    private fun buildMethod(method: Method): String {
        val name = method.name.camelCase().lowercaseFirst()
        val returnDtypeProps = getDtype(method.dtype)
        val returnStatement =
            returnDtypeProps.definition
                .let { if (method.many) "Iterable<$it>" else it }
                .let { if (method.nullable) "$it?" else it }
                .let { if (it == "Unit") "" else ": $it" }
        val arguments = mutableListOf<String>()

        for (argument in method.argumentsSortedByDefaults) {
            val argName = argument.name.camelCase().lowercaseFirst()
            val dtypeProps = getDtype(argument.dtype)
            val argTypeName =
                dtypeProps.definition
                    .let { if (argument.isEnum) argument.name.snakeCase().capitalizeFirst() else it }
                    .let { if (argument.many) "List<$it>" else it }
                    .let { if (argument.nullable) "$it?" else it }
            val argDefaultValue =
                if (argument.default == UNSET) {
                    ""
                } else {
                    dtypeProps.toGeneratedValue(argument.default ?: "null")
                }
                    .let { if (it.isEmpty()) "" else "= $it" }

            val argumentString = "$argName: $argTypeName $argDefaultValue".trim()
            arguments.add(argumentString)
        }

        // include enums (if present among method arguments)
        includedEntityGenerator.renderEntity(method.toEntity())

        val argumentsString = arguments.joinToString(separator = ", ")
        val definition = "fun $name($argumentsString)$returnStatement"
        val lines = mutableListOf<String>()

        method.description?.let {
            lines.add("/**\n * ${it.replace("\n", "\n * ")}\n */")
        }
        lines.add(definition)
        return lines.joinToString(separator = "\n")
    }

    override fun renderEntityName(name: String) = name.camelCase()

    override fun renderEntity(entity: Entity): String {
        // either build an interface or regular DTO
        if (entity.fields.isEmpty()) {
            return buildInterfaceEntity(entity)
        }
        return KtDataclassSerializableGenerator(this).renderEntity(entity)
    }

    private fun buildInterfaceEntity(entity: Entity): String {
        val entityName = renderEntityName(entity.name)
        val interfaceDefinition = "interface $entityName {"
        val builtMethods = entity.methodsPlusEndpoints().map { buildMethod(it) }
        return builtMethods.joinToString(separator = "\n\n", prefix = "${interfaceDefinition}\n", postfix = "\n}\n") {
            "    ${it.replace("\n", "\n    ")}"
        }
    }
}
