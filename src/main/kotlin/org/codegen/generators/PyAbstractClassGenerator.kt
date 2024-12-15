package org.codegen.generators

import org.codegen.schema.Endpoint

open class PyAbstractClassGenerator(proxy: AbstractCodeGenerator? = null) : PyApiClientGenerator(proxy) {
    override fun buildEndpointHeader(endpoint: Endpoint): String {
        return "@abstractmethod\n" + super.buildEndpointHeader(endpoint)
    }

    override fun buildEndpointBody(endpoint: Endpoint): String {
        return if (!endpoint.description.isNullOrEmpty()) {
            ""
        } else {
            "..."
        }
    }

    override fun buildBodyPrefix(): String {
        headers.add("from abc import ABC")
        headers.add("from abc import abstractmethod")
        return ""
    }
}
