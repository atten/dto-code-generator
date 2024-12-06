package org.codegen.generators

import org.codegen.schema.Endpoint
import java.io.File

class PyApiAsyncClientGenerator(proxy: AbstractCodeGenerator? = null) : PyApiClientGenerator(proxy) {
    override val baseClassName = "BaseJsonApiClientAsync"

    override fun buildMethodDefinition(
        name: String,
        arguments: List<String>,
        returnStatement: String,
        singleLine: Boolean?,
    ): String {
        val newReturnStatement =
            if (returnStatement.contains("AsyncIterator")) {
                returnStatement
            } else {
                returnStatement.replace("Iterator", "AsyncIterator")
            }
        val ret =
            super.buildMethodDefinition(name, arguments, newReturnStatement, singleLine)
                .let { if (it.startsWith("async")) it else "async $it" }

        if (singleLine == null && ret.length > 120) {
            return buildMethodDefinition(name, arguments, returnStatement, singleLine = false)
        }
        return ret
    }

    override fun buildEndpointBody(endpoint: Endpoint): String {
        return super.buildEndpointBody(endpoint)
            .replace("self._fetch", "await self._fetch")
            .let {
                if ("yield from" in it) {
                    it.replace("yield from", "for item in") + ":\n    yield item"
                } else {
                    it
                }
            }
    }

    override fun buildBodyPrefix(): String {
        headers.add("import os")
        headers.add("import typing as t")
        headers.add("import logging")
        headers.add("import asyncio")
        headers.add("import aiohttp")
        headers.add("from urllib.parse import urljoin, urlencode")
        headers.add("import marshmallow")
        headers.add("import marshmallow_dataclass")
        headers.add("from dataclasses import is_dataclass")
        headers.add("from datetime import datetime")
        headers.add("from datetime import timedelta")
        headers.add("from datetime import timezone")
        headers.add("from decimal import Decimal")
        headers.add("from typeguard import typechecked")

        listOf(
            "/templates/python/baseSchema.py",
            "/templates/python/restApiClientAsync.py",
            "/templates/python/serializationMethods.py",
            "/templates/python/failsafeCallAsync.py",
        ).map { path ->
            this.javaClass.getResource(path)!!.path
                .let { File(it).readText() }
        }
            .joinToString(separator = "\n\n")
            .let { addDefinition(it) }

        return ""
    }
}
