package org.codegen.generators

import org.codegen.dto.Endpoint
import java.io.File

class PyApiClientGeneratorAsync(proxy: AbstractCodeGenerator? = null) : PyApiClientGenerator(proxy) {
    override val baseClassName = "BaseJsonApiClientAsync"

    override fun buildMethodDefinition(name: String, arguments: List<String>, returnStatement: String, singleLine: Boolean?): String {
        val ret = super.buildMethodDefinition(name, arguments, returnStatement, singleLine).let { if (it.startsWith("async")) it else "async $it" }
        if (singleLine == null && ret.length > 120)
            return buildMethodDefinition(name, arguments, returnStatement, singleLine = false)
        return ret
    }

    override fun buildEndpointBody(endpoint: Endpoint): String {
        return super.buildEndpointBody(endpoint)
            .replace("self._fetch", "await self._fetch")
    }

    override fun buildBodyPrefix(): String {
        addHeader("import asyncio")
        addHeader("import aiohttp")
        addHeader("from urllib.parse import urljoin, urlencode")
        addHeader("import marshmallow_dataclass")

        this.javaClass.getResource("/restApiClientAsync.py")!!.path
            .let { File(it).readText() }
            .let { addDefinition(it, "") }

        return ""
    }

}