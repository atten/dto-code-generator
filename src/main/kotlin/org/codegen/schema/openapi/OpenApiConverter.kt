package org.codegen.schema.openapi

import org.codegen.schema.*
import org.codegen.schema.Constants.Companion.UNSET

internal class OpenApiConverter(
    private val spec: Root,
) {
    private val dtypeMapping =
        mapOf(
            "integer" to "int",
            "number" to "float",
            "string" to "str",
            "boolean" to "bool",
            "object" to "json",
        )

    fun convertToDocument(): Document {
        val document = Document()
        spec.definitions.forEach { addDefinitionToDocument(it.key, it.value, document) }
        spec.components.schemas.forEach { addDefinitionToDocument(it.key, it.value, document) }
        spec.paths.forEach { addPathToDocument(it.key, it.value, document) }
        return document
    }

    private fun addDefinitionToDocument(
        name: String,
        definition: Definition,
        document: Document,
    ) {
        val entity = convertDefinition(definition, name)
        document.entities.add(entity)
    }

    private fun addPathToDocument(
        path: String,
        pathBody: Path,
        document: Document,
    ) {
        pathBody.get?.also { addMethodToDocument(it, "get", path, document) }
        pathBody.post?.also { addMethodToDocument(it, "post", path, document) }
        pathBody.put?.also { addMethodToDocument(it, "put", path, document) }
        pathBody.patch?.also { addMethodToDocument(it, "patch", path, document) }
        pathBody.delete?.also { addMethodToDocument(it, "delete", path, document) }
    }

    private fun addMethodToDocument(
        method: Method,
        methodName: String,
        path: String,
        document: Document,
    ) {
        val fullPath = spec.basePath + path
        val pathBody = spec.paths[path]!!
        val description =
            listOf(method.summary.orEmpty(), method.description)
                .filter { it.isNotEmpty() }
                .joinToString("\n")

        val successResponseCode = method.responses.keys.first { it in 200..299 }
        val successResponse = method.responses[successResponseCode]
        val successResponseSchema =
            if (successResponse?.schema != null) {
                successResponse.schema
            } else if (successResponse?.content?.keys?.contains("application/json") == true) {
                successResponse.content["application/json"]?.schema
            } else {
                null
            }
        val successResponseDefinitionName = successResponseSchema?.definitionName() ?: "void"
        val endpointParameters = (pathBody.parameters + method.parameters).filter { it.source != "header" }
        val endpointArguments = endpointParameters.map { convertParameter(it) }.toMutableList()

        // if requestBody is defined, then add it as method argument
        method.requestBody?.content?.get("application/json")?.schema?.let {
            MethodArgument(
                name = "value",
                dtype = it.definitionName(),
            )
        }?.let {
            endpointArguments.add(it)
        }

        val endpoint =
            Endpoint(
                name = method.operationId,
                description = description.ifEmpty { null },
                dtype = dtypeMapping.getOrDefault(successResponseDefinitionName, successResponseDefinitionName),
                path = fullPath,
                verb = EndpointVerb.valueOf(methodName.uppercase()),
                arguments = endpointArguments,
            )

        document.endpoints.add(endpoint)
    }

    private fun convertParameter(parameter: Parameter): MethodArgument {
        return MethodArgument(
            name = parameter.name,
            description = parameter.description,
            dtype =
                if (!parameter.type.isNullOrEmpty()) {
                    dtypeMapping[parameter.type]!!
                } else if (parameter.schema?.definitionName() != null) {
                    parameter.schema.definitionName().let { dtypeMapping.getOrDefault(it, it) }
                } else {
                    // parameter is implied to be string by default
                    "str"
                },
            nullable = !parameter.required,
            default = if (parameter.required) UNSET else null,
            many = parameter.type == "array" || parameter.schema?.type == "array",
        )
    }

    private fun convertDefinition(
        definition: Definition,
        name: String,
    ): Entity {
        return Entity(
            name = name,
            fields = definition.properties.map { convertProperty(it.value, it.key, definition) },
        )
    }

    private fun convertProperty(
        property: Property,
        name: String,
        definition: Definition,
    ): Field {
        val required = definition.required.contains(name)
        return Field(
            name = name,
            description = property.description,
            dtype = property.definitionName().let { dtypeMapping.getOrDefault(it, it) },
            many = property.type == "array",
            nullable = !required,
            default = if (required) UNSET else null,
            enum = property.enum?.associateBy { it },
            enumPrefix = property.title,
        )
    }
}
