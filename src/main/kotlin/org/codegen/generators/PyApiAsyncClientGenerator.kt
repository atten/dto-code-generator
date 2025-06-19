package org.codegen.generators

import org.codegen.schema.Endpoint
import org.codegen.utils.Reader

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

    override fun renderEndpointBody(endpoint: Endpoint): String {
        return super.renderEndpointBody(endpoint)
            .replace("self._client.fetch", "await self._client.fetch")
            .let {
                if ("yield from" in it) {
                    it.replace("yield from", "for item in") + ":\n    yield item"
                } else {
                    it
                }
            }
    }

    override fun renderHeaders(): String {
        listOf(
            "import asyncio",
            "import aiohttp",
        ).forEach { headers.add(it) }

        return super.renderHeaders()
            .replace("\nimport ijson", "")
            .replace("\nimport urllib3", "")
            .replace("\nfrom time import sleep", "")
            .replace("\nimport io", "")
            .replace("\nimport json", "")
    }

    override fun getMainApiClassBody() =
        Reader.readFileOrResourceOrUrl("resource:/templates/python/apiClientBody.py")
            .replace("BaseJsonHttpClient", "BaseJsonHttpAsyncClient")
            .trimEnd()

    override fun getBodyIncludedFiles(): List<String> {
        val original = super.getBodyIncludedFiles().toMutableList()
        original.replaceAll {
            mapOf(
                "resource:/templates/python/baseJsonHttpClient.py" to "resource:/templates/python/baseJsonHttpAsyncClient.py",
                "resource:/templates/python/failsafeCall.py" to "resource:/templates/python/failsafeCallAsync.py",
            ).getOrDefault(it, it)
        }
        return original
    }
}
