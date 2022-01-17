package org.codegen.generators

import org.codegen.dto.*
import org.codegen.extensions.camelCase
import org.codegen.extensions.capitalize
import org.codegen.extensions.snakeCase
import java.io.File

open class PyApiClientGenerator(proxy: AbstractCodeGenerator? = null) : AbstractCodeGenerator(PY_FORMAT_RULE, AllGeneratorsEnum.PY_MARSHMALLOW_DATACLASS, proxy) {
    protected open val baseClassName = "BaseJsonApiClient"
    protected val atomicJsonTypes = listOf("str", "float", "int", "None", "bool")
    // list if __all__ items
    private val definedNames = mutableListOf<String>()

    private fun buildArgumentDefaultValue(argument: MethodArgument): String {
        val dtypeProps = getDtype(argument.dtype)
        return if (argument.default == UNSET) {
            ""
        } else if (argument.multiple) {
            // use immutable tuple for default value
            if (argument.default == EMPTY_PLACEHOLDER) {
                "()"
            } else {
                "(${dtypeProps.toGeneratedValue(argument.default ?: "None")})"
            }
        } else {
            when (argument.default) {
                EMPTY_PLACEHOLDER -> "${dtypeProps.definition}()".replace("str()", "''")
                null -> "None"
                else -> dtypeProps.toGeneratedValue(argument.default)
            }
        }
    }

    override fun addDefinition(body: String, vararg names: String) {
        super.addDefinition(body, *names)
        // add missing names into __all__
        names
            .filter { it.isNotEmpty() && it !in definedNames }
            .forEach { definedNames.add(it) }
    }

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

    protected open fun buildEndpointHeader(endpoint: Endpoint): String {
        val name = endpoint.name.snakeCase()
        val returnDtypeProps = getDtype(endpoint.dtype)
        val returnStatement = returnDtypeProps.definition
            .let {
                if (endpoint.multiple) {
                    headers.add("import typing as t")
                    "t.List[$it]"
                }
                else if (endpoint.nullable) {
                    headers.add("import typing as t")
                    "t.Optional[$it]"
                }
                else it
            }
            .let { if (it == "None") ":" else " -> $it:" }
        val arguments = mutableListOf("self")

        for (argument in endpoint.argumentsSortedByDefaults) {
            val argName = argument.name.snakeCase()
            val dtypeProps = getDtype(argument.dtype)
            val argTypeName = dtypeProps.definition
                .let {
                    if (argument.multiple) {
                        headers.add("import typing as t")
                        "t.Sequence[$it]"
                    }
                    else it
                }
                .let { if (argument.nullable) "t.Optional[$it]" else it }

            val argDefaultValue = buildArgumentDefaultValue(argument)
                .let { if (it.isEmpty()) "" else "= $it" }

            val argumentString = "${argName}: $argTypeName $argDefaultValue".trim()
            arguments.add(argumentString)
        }

        val lines = mutableListOf<String>()

        if (endpoint.cacheable) {
            headers.add("from funcy import memoize")
            lines.add("@memoize")
        }

        lines.add(buildMethodDefinition(name, arguments, returnStatement, singleLine = null))
        endpoint.description
            ?.replace("\n", "\n    ")
            ?.let {
                lines.add("    \"\"\"")
                lines.add("    $it")
                lines.add("    \"\"\"")
            }
        return lines.joinToString(separator = "\n")
    }

    protected open fun buildEndpointBody(endpoint: Endpoint): String {
        val returnDtypeProps = getDtype(endpoint.dtype)
        val returnType = returnDtypeProps.definition
        // uri name, variable name, default value (if present and is atomic)
        val queryParams = mutableListOf<Triple<String, String, String?>>()
        var payloadVariableName = ""
        val lines = mutableListOf<String>()

        for (argument in endpoint.argumentsSortedByDefaults) {
            val argName = argument.name.snakeCase()
            val dtypeProps = getDtype(argument.dtype)
            val argBaseType = dtypeProps.definition

            val isAtomicType = argBaseType in atomicJsonTypes
            val pathName = "{" + argument.name + "}"
            val isPathVariable = pathName in endpoint.path
            val isQueryVariable = !isPathVariable && (endpoint.verb == EndpointVerb.GET || isAtomicType)
            val isPayload = !isPathVariable && !isQueryVariable

            if (!isAtomicType) {
                if (isPayload)
                    lines.add("$argName = self._serialize($argName, is_payload=True)")
                else
                    lines.add("$argName = self._serialize($argName)")
            }

            if (isQueryVariable) {
                val queryParamName = argument.name.camelCase()
                val defaultValue = if (argument.default != UNSET && isAtomicType) buildArgumentDefaultValue(argument) else null
                queryParams.add(Triple(queryParamName, argName, defaultValue))
            } else if (isPayload) {
                require(payloadVariableName.isEmpty()) { "Having multiple payload variables is unsupported yet" }
                payloadVariableName = argName
            }
        }

        // prepare dict of query params
        if (queryParams.isNotEmpty()) {
            lines.add("query_params = dict()")
        }

        // include query params only if provided value != default
        queryParams.forEach { (queryParam, variable, defaultValue) ->
            val useCondition = defaultValue != null
            if (useCondition) {
                val expression = when (defaultValue) {
                    "None" -> "$variable is not None"
                    "()" -> "len($variable)"
                    "False" -> variable
                    "True" -> "not $variable"
                    else -> "$variable != $defaultValue"
                }

                lines.add("if $expression:")
                lines.add("    query_params['$queryParam'] = $variable")
            } else {
                lines.add("query_params['$queryParam'] = $variable")
            }
        }

        // prepare 'fetch' method call
        if (returnType == "None")
            lines.add("self._fetch(")
        else if (atomicJsonTypes.contains(returnType))
            lines.add("return self._fetch(")
        else
            lines.add("raw_data = self._fetch(")

        lines.add("    url=f'${endpoint.path}',")

        if (endpoint.verb != EndpointVerb.GET) {
            lines.add("    method='${endpoint.verb}',")
        }

        if (queryParams.isNotEmpty()) {
            lines.add("    query_params=query_params,")
        }

        if (payloadVariableName.isNotEmpty()) {
            lines.add("    payload=$payloadVariableName,")
        }

        lines.add(")")  // end of 'self._fetch('

        // prepare return statement
        if (!atomicJsonTypes.contains(returnType)) {
            if (endpoint.multiple)
                lines.add("return self._deserialize(raw_data, $returnType, many=True)")
            else
                lines.add("return self._deserialize(raw_data, $returnType)")
        }
        return lines.joinToString(separator = "\n")
    }

    private fun buildEndpoint(endpoint: Endpoint) =
        buildEndpointHeader(endpoint) + buildEndpointBody(endpoint)
            .let { if (it.isNotEmpty()) "\n$it" else it }
            .replace("\n", "\n    ")

    override fun buildEntityName(name: String) = name.camelCase().capitalize()

    override fun buildEntity(entity: Entity): String {
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
        headers.add("import os")
        headers.add("import typing as t")
        headers.add("import json")
        headers.add("import urllib3")
        headers.add("from urllib.parse import urljoin, urlencode")
        headers.add("from time import sleep")
        headers.add("import marshmallow")
        headers.add("import marshmallow_dataclass")
        headers.add("from dataclasses import is_dataclass")
        headers.add("from datetime import datetime")
        headers.add("from datetime import timedelta")
        headers.add("from datetime import timezone")
        headers.add("from decimal import Decimal")

        listOf("/restApiClient.py", "/serializationMethods.py").map { path ->
            this.javaClass.getResource(path)!!.path
                .let { File(it).readText() }
        }
            .joinToString(separator = "\n\n")
            .let { addDefinition(it) }

        return ""
    }

    override fun buildBodyPostfix(): String {
        definedNames
            .sorted()
            .joinToString("\n", "__all__ = [\n", "\n]") { "    \"${it}\"," }
            .also { addDefinition(it, "__all__") }
        return "\n"
    }
}