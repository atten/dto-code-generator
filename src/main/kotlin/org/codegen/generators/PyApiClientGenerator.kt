package org.codegen.generators

import org.codegen.dto.Endpoint
import org.codegen.dto.Entity
import org.codegen.dto.UNSET
import org.codegen.extensions.camelCase
import org.codegen.extensions.capitalize
import org.codegen.extensions.snakeCase
import java.io.File

open class PyApiClientGenerator(proxy: AbstractCodeGenerator? = null) : AbstractCodeGenerator(PY_FORMAT_RULE, AllGeneratorsEnum.PY_MARSHMALLOW_DATACLASS, proxy) {
    protected open val baseClassName = "BaseJsonApiClient"
    private val atomicJsonTypes = listOf("str", "float", "int", "None", "bool")
    // list if __all__ items
    private val definedNames = mutableListOf<String>()

    protected open fun buildMethodDefinition(name: String, arguments: List<String>, returnStatement: String, singleLine: Boolean?): String {
        when (singleLine) {
            true -> {
                val argumentsString = arguments.joinToString(separator = ", ")
                return "def ${name}($argumentsString)$returnStatement"
            }
            false -> {
                val argumentsString = arguments.joinToString(separator = ",\n    ", prefix = "\n    ", postfix = ",\n")
                return "def ${name}($argumentsString)$returnStatement"
            }
            else -> {
                // auto-choice
                val oneLiner = buildMethodDefinition(name, arguments, returnStatement, singleLine = true)
                if (oneLiner.length > 120)
                    return buildMethodDefinition(name, arguments, returnStatement, singleLine = false)
                return oneLiner
            }
        }
    }

    private fun buildEndpointHeader(endpoint: Endpoint): String {
        val name = endpoint.name.snakeCase()
        val returnDtypeProps = getDtype(endpoint.dtype)
        val returnStatement = returnDtypeProps.definition
            .let {
                if (endpoint.multiple) {
                    addHeader("import typing as t")
                    "t.List[$it]"
                }
                else if (endpoint.nullable) {
                    addHeader("import typing as t")
                    "t.Optional[$it]"
                }
                else it
            }
            .let { if (it == "None") ":" else " -> $it:" }
        val arguments = mutableListOf("self")

        for (argument in endpoint.arguments) {
            val argName = argument.name.snakeCase()
            val dtypeProps = getDtype(argument.dtype)
            val argTypeName = dtypeProps.definition
                .let {
                    if (argument.multiple) {
                        addHeader("import typing as t")
                        "t.Sequence[$it]"
                    }
                    else it
                }
                .let { if (argument.nullable) "t.Optional[$it]" else it }
            val argDefaultValue = if (argument.default == UNSET) {
                ""
            } else {
                dtypeProps.toGeneratedValue(argument.default ?: "None")
            }
                .let { if (it.isEmpty()) "" else "= $it" }

            val argumentString = "${argName}: $argTypeName $argDefaultValue".trim()
            arguments.add(argumentString)
        }

        val lines = mutableListOf<String>()

        lines.add(buildMethodDefinition(name, arguments, returnStatement, singleLine = null))
        endpoint.description?.let {
            lines.add("    \"\"\"")
            lines.add("    $it")
            lines.add("    \"\"\"")
        }
        return lines.joinToString(separator = "\n")
    }

    protected open fun buildEndpointBody(endpoint: Endpoint): String {
        val returnDtypeProps = getDtype(endpoint.dtype)
        val returnType = returnDtypeProps.definition
        val queryParams = mutableMapOf<String, String>()
        val lines = mutableListOf<String>()

        for (argument in endpoint.arguments) {
            val argName = argument.name.snakeCase()
            val dtypeProps = getDtype(argument.dtype)
            val argBaseType = dtypeProps.definition

            if (!atomicJsonTypes.contains(argBaseType)) {
                if (argument.multiple)
                    lines.add("$argName = [self._serialize(item) for item in $argName]")
                else
                    lines.add("$argName = self._serialize($argName)")
            }

            if (!endpoint.path.contains("{$argName}"))
                queryParams[argument.name.camelCase()] = argName
        }

        if (returnType == "None")
            lines.add("self._fetch(")
        else if (atomicJsonTypes.contains(returnType))
            lines.add("return self._fetch(")
        else
            lines.add("raw_data = self._fetch(")

        lines.add("    url=f'${endpoint.path}',")

        if (queryParams.isNotEmpty()) {
            lines.add("    query_params=dict(")
            queryParams
                .map { "        ${it.key}=${it.value}," }
                .forEach { lines.add(it) }
            lines.add("    ),")
        }

        lines.add(")")  // end of 'self._fetch('

        if (!atomicJsonTypes.contains(returnType)) {
            if (endpoint.multiple)
                lines.add("return self._deserialize(raw_data, $returnType, many=True)")
            else
                lines.add("return self._deserialize(raw_data, $returnType)")
        }
        return lines.joinToString(separator = "\n")
    }

    private fun buildEndpoint(endpoint: Endpoint) = buildEndpointHeader(endpoint) + buildEndpointBody(endpoint).let { "\n$it" }.replace("\n", "\n    ")

    override fun buildEntityName(name: String) = name.camelCase().capitalize()

    override fun buildEntity(entity: Entity): String {
        definedNames.add(buildEntityName(entity.name))
        // either build an interface or regular DTO
        if (entity.fields.isEmpty())
            return buildClass(entity)
        return PyDataclassMarshmallowGenerator(this).buildEntity(entity)
    }

    private fun buildClass(entity: Entity): String {
        val className = buildEntityName(entity.name)
        val classDefinition = "class $className($baseClassName):"
        val builtMethods = entity.endpoints.map { buildEndpoint(it) }
        return builtMethods.joinToString(separator = "\n\n", prefix = "${classDefinition}\n") {"    ${it.replace("\n", "\n    ")}"}
    }

    override fun buildBodyPrefix(): String {
        addHeader("import json")
        addHeader("from urllib.error import URLError, HTTPError")
        addHeader("from urllib.parse import urljoin, urlencode")
        addHeader("from time import sleep")
        addHeader("from urllib.request import Request, urlopen")
        addHeader("import marshmallow_dataclass")

        this.javaClass.getResource("/restApiClient.py")!!.path
            .let { File(it).readText() }
            .let { addDefinition(it) }

        return ""
    }

    override fun buildBodyPostfix(): String {
        definedNames
            .joinToString("\n", "__all__ = [\n", "\n]") { "    \"${it}\"," }
            .also { addDefinition(it) }
        return "\n"
    }
}