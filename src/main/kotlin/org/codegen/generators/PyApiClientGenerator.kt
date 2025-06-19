package org.codegen.generators

import org.codegen.schema.Constants.Companion.EMPTY
import org.codegen.schema.Constants.Companion.UNSET
import org.codegen.schema.Endpoint
import org.codegen.schema.EndpointVerb
import org.codegen.schema.MethodArgument
import org.codegen.utils.Reader
import org.codegen.utils.snakeCase

open class PyApiClientGenerator(proxy: AbstractCodeGenerator? = null) : PyBaseClientGenerator(proxy) {
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

    override fun renderEndpointBody(endpoint: Endpoint): String {
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
                val queryParamName = argument.name
                val defaultValue = if (argument.default != UNSET && isAtomicType) buildArgumentDefaultValue(argument) else null
                queryParams.add(Triple(queryParamName, argName, defaultValue))
            } else if (isPayload) {
                require(payloadVariableName.isEmpty()) { "Having multiple payload variables is not unsupported" }
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
            when (endpoint.encoding) {
                Endpoint.EndpointEncoding.FORM -> lines.add("    form_fields=$payloadVariableName,")
                else -> lines.add("    json_body=$payloadVariableName,")
            }
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

    override fun renderHeaders(): String {
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
        ).forEach { headers.add(it) }
        return super.renderHeaders()
    }

    override fun getBodyIncludedFiles() =
        listOf(
            "resource:/templates/python/baseJsonHttpClient.py",
            "resource:/templates/python/baseSerializer.py",
            "resource:/templates/python/baseDeserializer.py",
            "resource:/templates/python/failsafeCall.py",
            "resource:/templates/python/buildCurlCommand.py",
        )

    override fun getMainApiClassBody() = Reader.readFileOrResourceOrUrl("resource:/templates/python/apiClientBody.py").trimEnd()
}
