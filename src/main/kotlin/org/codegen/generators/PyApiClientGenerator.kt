package org.codegen.generators

import org.codegen.format.CodeFormatRules
import org.codegen.format.camelCase
import org.codegen.format.lowercaseFirst
import org.codegen.format.snakeCase
import org.codegen.schema.Constants.Companion.EMPTY
import org.codegen.schema.Constants.Companion.UNSET
import org.codegen.schema.Endpoint
import org.codegen.schema.EndpointVerb
import org.codegen.schema.Entity
import org.codegen.schema.MethodArgument
import java.io.File

open class PyApiClientGenerator(proxy: AbstractCodeGenerator? = null) : AbstractCodeGenerator(
    CodeFormatRules.PYTHON,
    AllGeneratorsEnum.PY_MARSHMALLOW_DATACLASS,
    proxy,
) {
    protected val atomicJsonTypes = listOf("str", "float", "int", "None", "bool")

    // list if __all__ items
    private val definedNames = mutableListOf<String>()

    private fun buildArgumentDefaultValue(argument: MethodArgument): String {
        val dtypeProps = getDtype(argument.dtype)
        return if (argument.default == UNSET) {
            ""
        } else if (argument.many) {
            // use immutable tuple for default value
            "()"
        } else {
            when (argument.default) {
                EMPTY -> "${dtypeProps.definition}()".replace("str()", "''")
                null -> "None"
                else -> dtypeProps.toGeneratedValue(argument.default)
            }
        }
    }

    override fun addDefinition(
        body: String,
        vararg names: String,
    ) {
        super.addDefinition(body, *names)
        // add missing names into __all__
        names
            .filter { it.isNotEmpty() && it !in definedNames }
            .forEach { definedNames.add(it) }
    }

    protected open fun buildMethodDefinition(
        name: String,
        arguments: List<String>,
        returnStatement: String,
        singleLine: Boolean? = null,
    ): String {
        when (singleLine) {
            true -> {
                val argumentsString = arguments.joinToString(separator = ", ")
                return "def $name($argumentsString)$returnStatement"
            }
            false -> {
                val argumentsString =
                    arguments.joinToString(separator = ",\n    ", prefix = "\n    ", postfix = ",\n") {
                        it.replace("\n", "\n    ")
                    }
                return "def $name($argumentsString)$returnStatement"
            }
            else -> {
                // auto-choice
                val oneLiner = buildMethodDefinition(name, arguments, returnStatement, singleLine = true)
                if (oneLiner.length > 120) {
                    return buildMethodDefinition(name, arguments, returnStatement, singleLine = false)
                }
                return oneLiner
            }
        }
    }

    protected open fun buildEndpointHeader(endpoint: Endpoint): String {
        val name = endpoint.name.snakeCase()
        val returnDtypeProps = getDtype(endpoint.dtype)
        val returnStatement =
            returnDtypeProps.definition
                .let {
                    if (!returnDtypeProps.isNative) {
                        // wrap into quotes if definition is listed below
                        "'$it'"
                    } else {
                        it
                    }
                }
                .let {
                    if (endpoint.many) {
                        headers.add("import typing as t")
                        if (endpoint.cacheable) "list[$it]" else "t.Iterator[$it]"
                    } else if (endpoint.nullable) {
                        headers.add("import typing as t")
                        "t.Optional[$it]"
                    } else {
                        it
                    }
                }
                .let { if (it == "None") ":" else " -> $it:" }
        val arguments = mutableListOf("self")

        for (argument in endpoint.argumentsSortedByDefaults) {
            val argName = argument.name.snakeCase()
            val dtypeProps = getDtype(argument.dtype)
            val argTypeName =
                dtypeProps.definition
                    .let {
                        if (!dtypeProps.isNative) {
                            // wrap into quotes if definition is listed below
                            "'$it'"
                        } else {
                            it
                        }
                    }
                    .let {
                        if (argument.many) {
                            headers.add("import typing as t")
                            "t.Sequence[$it]"
                        } else {
                            it
                        }
                    }
                    .let { if (argument.nullable) "t.Optional[$it]" else it }

            val argDefaultValue =
                buildArgumentDefaultValue(argument)
                    .let { if (it.isEmpty()) "" else "= $it" }

            val argDescription =
                if (!argument.description.isNullOrEmpty()) {
                    "# ${argument.description}".trim()
                } else {
                    ""
                }

            val argumentString = "$argDescription\n$argName: $argTypeName $argDefaultValue".trim()
            arguments.add(argumentString)
        }

        val lines = mutableListOf<String>()

        if (endpoint.cacheable) {
            headers.add("from funcy import memoize")
            lines.add("@memoize")
        }

        val singleLine =
            if (arguments.any { "\n" in it }) {
                false
            } else {
                null // auto-choice
            }

        lines.add(buildMethodDefinition(name, arguments, returnStatement, singleLine = singleLine))
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
        var endpointPath = endpoint.path

        for (argument in endpoint.argumentsSortedByDefaults) {
            val argName = argument.name.snakeCase()
            val dtypeProps = getDtype(argument.dtype)
            val argBaseType = dtypeProps.definition

            val isAtomicType = argBaseType in atomicJsonTypes
            val pathName = "{%s}".format(argument.name)
            val isPathVariable = pathName in endpointPath
            val isQueryVariable = !isPathVariable && (endpoint.verb == EndpointVerb.GET || isAtomicType)
            val isPayload = !isPathVariable && !isQueryVariable

            if (!isAtomicType) {
                if (isPayload) {
                    lines.add("$argName = self._serializer.serialize($argName, is_payload=True)")
                } else {
                    lines.add("$argName = self._serializer.serialize($argName)")
                }
            }

            if (isQueryVariable) {
                val queryParamName = argument.name.camelCase().lowercaseFirst()
                val defaultValue = if (argument.default != UNSET && isAtomicType) buildArgumentDefaultValue(argument) else null
                queryParams.add(Triple(queryParamName, argName, defaultValue))
            } else if (isPayload) {
                require(payloadVariableName.isEmpty()) { "Having multiple payload variables is unsupported yet" }
                payloadVariableName = argName
            }

            if (isPathVariable) {
                endpointPath = endpointPath.replace(pathName, "{%s}".format(argName))
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
                val expression =
                    when (defaultValue) {
                        "None" -> "$variable is not None"
                        "()" -> "$variable and len($variable)"
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
        if (returnType == "None") {
            lines.add("self._client.fetch(")
        } else {
            lines.add("raw_data = self._client.fetch(")
        }

        if (endpointPath.contains('{')) {
            lines.add("    url=f'$endpointPath',")
        } else {
            lines.add("    url='$endpointPath',")
        }

        if (endpoint.verb != EndpointVerb.GET) {
            lines.add("    method='${endpoint.verb}',")
        }

        if (queryParams.isNotEmpty()) {
            lines.add("    query_params=query_params,")
        }

        if (payloadVariableName.isNotEmpty()) {
            lines.add("    payload=$payloadVariableName,")
        }

        lines.add(")") // end of 'self._fetch('

        // prepare return statement
        if (endpoint.many) {
            if (endpoint.cacheable) {
                lines.add("return list(self._deserializer.deserialize(raw_data, $returnType, many=True))")
            } else {
                lines.add("yield from self._deserializer.deserialize(raw_data, $returnType, many=True)")
            }
        } else if (returnType != "None") {
            if (atomicJsonTypes.contains(returnType)) {
                lines.add("gen = self._deserializer.deserialize(raw_data)")
            } else {
                lines.add("gen = self._deserializer.deserialize(raw_data, $returnType)")
            }
            lines.add("return next(gen)")
        }
        return lines.joinToString(separator = "\n")
    }

    private fun buildEndpoint(endpoint: Endpoint) =
        buildEndpointHeader(endpoint) +
            buildEndpointBody(endpoint)
                .let { if (it.isNotEmpty()) "\n$it" else it }
                .replace("\n", "\n    ")

    override fun buildEntityName(name: String) = name.camelCase()

    override fun buildEntity(entity: Entity): String {
        if (entity.fields.isEmpty()) {
            return ""
        }
        return PyMarshmallowDataclassGenerator(this).buildEntity(entity)
    }

    override fun buildBodyPrefix(): String {
        requiredHeaders().forEach { headers.add(it) }

        listOf(
            "/templates/python/baseSchema.py",
        )
            .map { this.javaClass.getResource(it)!!.path }
            .map { File(it).readText() }
            .map { addDefinition(it) }

        return buildMainApiClass()
    }

    protected open fun requiredHeaders() =
        listOf(
            "import os",
            "import io",
            "import typing as t",
            "import logging",
            "import json",
            "import ijson",
            "import urllib3",
            "from urllib.parse import urljoin, urlencode",
            "from time import sleep",
            "import marshmallow",
            "import marshmallow_dataclass",
            "from dataclasses import is_dataclass",
            "from datetime import datetime",
            "from datetime import timedelta",
            "from datetime import timezone",
            "from decimal import Decimal",
            "from typeguard import typechecked",
        )

    protected open fun getMainApiClassBody() =
        this.javaClass.getResource("/templates/python/apiClientBody.py")!!.path.let {
            File(it).readText()
        }

    private fun buildMainApiClass(): String {
        val entity = defaultEntity
        val className = buildEntityName(entity.name)
        val classDefinition = "class $className:"
        val classBody = getMainApiClassBody()
        val classMethods = entity.endpoints.map { buildEndpoint(it) }
        return classMethods.joinToString(
            separator = "\n\n",
            prefix = "${classDefinition}\n${classBody}\n\n",
            postfix = CodeFormatRules.PYTHON.entitiesSeparator,
        ) { "    ${it.replace("\n", "\n    ")}" }
    }

    override fun buildBodyPostfix(): String {
        getIncludedIntoFooterPaths()
            .map { this.javaClass.getResource(it)!!.path }
            .map { File(it).readText() }
            .map { addDefinition(it) }

        definedNames
            .sorted()
            .joinToString("\n", "__all__ = [\n", "\n]") { "    \"${it}\"," }
            .also { addDefinition(it, "__all__") }
        return "\n"
    }

    protected open fun getIncludedIntoFooterPaths() =
        listOf(
            "/templates/python/baseJsonHttpClient.py",
            "/templates/python/baseSerializer.py",
            "/templates/python/baseDeserializer.py",
            "/templates/python/failsafeCall.py",
            "/templates/python/buildCurlCommand.py",
        )
}
