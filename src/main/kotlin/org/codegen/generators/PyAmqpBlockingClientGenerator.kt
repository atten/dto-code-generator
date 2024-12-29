package org.codegen.generators

import org.codegen.format.snakeCase
import org.codegen.schema.Endpoint
import java.io.File

open class PyAmqpBlockingClientGenerator(proxy: AbstractCodeGenerator? = null) : PyBaseClientGenerator(proxy) {
    override fun renderEndpointHeader(endpoint: Endpoint): String {
        // amqp client does not support streaming yet
        return super.renderEndpointHeader(endpoint).replace("Iterator", "List")
    }

    override fun renderEndpointBody(endpoint: Endpoint): String {
        val returnDtypeProps = getDtype(endpoint.dtype)
        val returnType = returnDtypeProps.definition
        val lines = mutableListOf<String>()
        val payloadArgs = mutableListOf<String>()
        val routingKeyArgs = mutableListOf<String>()

        for (argument in endpoint.arguments) {
            val pathName = "{" + argument.name + "}"
            val argName = argument.name.snakeCase()
            val isPathVariable = pathName in endpoint.path
            val dtypeProps = getDtype(argument.dtype)
            val argBaseType = dtypeProps.definition
            val isAtomicType = argBaseType in atomicJsonTypes

            if (!isAtomicType) {
                if (isPathVariable) {
                    lines.add("$argName = self._serializer.serialize($argName)")
                } else {
                    lines.add("$argName = self._serializer.serialize($argName, is_payload=True)")
                }
            }

            if (isPathVariable) {
                routingKeyArgs.add(argName)
            } else {
                payloadArgs.add(argName)
            }
        }

        val routingKey =
            "f'${endpoint.path}'".let {
                // optimize string expression if it contains nothing but single string-like parameter
                if (routingKeyArgs.count() == 1 && it == "f'{${routingKeyArgs.first()}}'") {
                    routingKeyArgs.first()
                } else {
                    it
                }
            }

        if (payloadArgs.isNotEmpty()) {
            lines.add(payloadArgs.joinToString(", ", prefix = "args = (", postfix = ",)"))
        }

        // prepare '_mk_request' method call
        "raw_data = self._client.mk_request($routingKey, '${endpoint.name.snakeCase()}', *args).get()".let {
            if (payloadArgs.isEmpty()) {
                // exclude empty args
                it.replace(", *args", "")
            } else {
                it
            }
        }.let {
            if (returnType == "None") {
                it.replace("raw_data = ", "")
            } else {
                it
            }
        }.let {
            lines.add(it)
        }

        // prepare return statement
        if (endpoint.many) {
            lines.add("return list(self._deserializer.deserialize(raw_data, $returnType, many=True))")
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
            "import typing as t",
            "import io",
            "import os",
            "import json",
            "from dataclasses import is_dataclass",
            "from dataclasses import astuple",
            "from dataclasses import dataclass",
            "from dataclasses import field",
            "import marshmallow_dataclass",
            "from datetime import datetime",
            "from datetime import timedelta",
            "from datetime import timezone",
            "from decimal import Decimal",
            "import marshmallow",
            "import ijson",
            "import logging",
            "import time",
            "from funcy import memoize",
            "from uuid import uuid4",
            "from threading import Lock",
            "from urllib.parse import urlparse",
            "from amqp.exceptions import RecoverableConnectionError, ConnectionForced",
            "from kombu import Connection, Exchange, Queue, Message",
            "from socket import timeout as SocketTimeout",
            "from typeguard import typechecked",
        ).forEach { headers.add(it) }
        return super.renderHeaders()
    }

    override fun getMainApiClassBody() =
        this.javaClass.getResource(
            "/templates/python/amqpClientBody.py",
        )!!.path.let { File(it).readText() }

    override fun getBodyIncludedFiles() =
        listOf(
            "/templates/python/baseJsonAmqpBlockingClient.py",
            "/templates/python/baseSerializer.py",
            "/templates/python/baseDeserializer.py",
            "/templates/python/failsafeCall.py",
        )

    override fun renderBodySuffix(): String {
        definedNames.add("FailedAmqpRequestError") // make error class accessible from outside
        return super.renderBodySuffix()
    }
}
