package org.codegen.generators

import org.codegen.dto.Endpoint

open class PyAbstractClassGenerator(proxy: AbstractCodeGenerator? = null) : PyApiClientGenerator(proxy) {
    override val baseClassName = "ABC"

    override fun buildEndpointHeader(endpoint: Endpoint): String {
        return "@abstractmethod\n" + super.buildEndpointHeader(endpoint)
    }

    override fun buildEndpointBody(endpoint: Endpoint): String {
        return if (endpoint.description != null && endpoint.description.isNotEmpty()) {
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