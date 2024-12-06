package org.codegen.generators

import org.codegen.format.snakeCase
import org.codegen.schema.Endpoint
import java.io.File

open class PyAmqpBlockingClientGenerator(proxy: AbstractCodeGenerator? = null) : PyApiClientGenerator(proxy) {
    override val baseClassName = "AmqpApiWithBlockingListener"

    override fun buildEndpointHeader(endpoint: Endpoint): String {
        // amqp client does not support streaming yet
        return super.buildEndpointHeader(endpoint).replace("Iterator", "List")
    }

    override fun buildEndpointBody(endpoint: Endpoint): String {
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
                    lines.add("$argName = self._serialize($argName)")
                } else {
                    lines.add("$argName = self._serialize($argName, is_payload=True)")
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
        "raw_data = self._mk_request($routingKey, '${endpoint.name.snakeCase()}', *args).get()".let {
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
            lines.add("return list(self._deserialize(raw_data, $returnType, many=True))")
        } else if (returnType != "None") {
            if (atomicJsonTypes.contains(returnType)) {
                lines.add("gen = self._deserialize(raw_data)")
            } else {
                lines.add("gen = self._deserialize(raw_data, $returnType)")
            }
            lines.add("return next(gen)")
        }

        return lines.joinToString(separator = "\n")
    }

    override fun buildBodyPrefix(): String {
        headers.add("import typing as t")
        headers.add("import io")
        headers.add("import json")
        headers.add("from dataclasses import is_dataclass")
        headers.add("from dataclasses import astuple")
        headers.add("from dataclasses import asdict")
        headers.add("from dataclasses import dataclass")
        headers.add("import marshmallow_dataclass")
        headers.add("from datetime import datetime")
        headers.add("from datetime import timedelta")
        headers.add("from datetime import timezone")
        headers.add("from decimal import Decimal")
        headers.add("import marshmallow")
        headers.add("import ijson")
        headers.add("import logging")
        headers.add("import time")
        headers.add("from funcy import memoize")
        headers.add("from uuid import uuid4")
        headers.add("from threading import Lock")
        headers.add("from urllib.parse import urlparse")
        headers.add("from amqp.exceptions import RecoverableConnectionError, ConnectionForced")
        headers.add("from kombu import Connection, Exchange, Queue, Message")
        headers.add("from socket import timeout as SocketTimeout")

        listOf(
            "/templates/python/baseSchema.py",
            "/templates/python/amqpBlockingClient.py",
            "/templates/python/serializationMethods.py",
            "/templates/python/failsafeCall.py",
        ).map { path ->
            this.javaClass.getResource(path)!!.path
                .let { File(it).readText() }
        }
            .joinToString(separator = "\n\n")
            .let { addDefinition(it, "FailedAmqpRequestError") }

        return ""
    }
}
