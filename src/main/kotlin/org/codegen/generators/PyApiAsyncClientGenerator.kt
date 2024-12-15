package org.codegen.generators

import org.codegen.schema.Endpoint

class PyApiAsyncClientGenerator(proxy: AbstractCodeGenerator? = null) : PyApiClientGenerator(proxy) {
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
            .replace("self._client.fetch", "await self._client.fetch")
            .let {
                if ("yield from" in it) {
                    it.replace("yield from", "for item in") + ":\n    yield item"
                } else {
                    it
                }
            }
    }

    override fun requiredHeaders() =
        listOf(
            "import os",
            "import typing as t",
            "import logging",
            "import asyncio",
            "import aiohttp",
            "from urllib.parse import urljoin, urlencode",
            "import marshmallow",
            "import marshmallow_dataclass",
            "from dataclasses import is_dataclass",
            "from datetime import datetime",
            "from datetime import timedelta",
            "from datetime import timezone",
            "from decimal import Decimal",
            "from typeguard import typechecked",
        )

    override fun buildBodyPrefix(): String = super.buildBodyPrefix().replace("BaseJsonHttpClient", "BaseJsonHttpAsyncClient")

    override fun getIncludedIntoFooterPaths() =
        listOf(
            "/templates/python/baseJsonHttpAsyncClient.py",
            "/templates/python/baseSerializer.py",
            "/templates/python/baseDeserializer.py",
            "/templates/python/failsafeCallAsync.py",
            "/templates/python/buildCurlCommand.py",
        )
}
