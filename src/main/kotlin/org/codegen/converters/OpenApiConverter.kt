package org.codegen.converters

import org.codegen.schema.*
import org.codegen.schema.Constants.Companion.UNSET
import org.codegen.schema.openapi.Definition
import org.codegen.schema.openapi.Method
import org.codegen.schema.openapi.Parameter
import org.codegen.schema.openapi.Property
import org.codegen.schema.openapi.Root

class OpenApiConverter(
    private val spec: Root
) {
    private val dtypeMapping = mapOf(
        "integer" to "int",
        "number" to "float",
        "string" to "str",
        "boolean" to "bool",
        "object" to "json"
    )

    fun convertToDocument(): Document {
        val document = Document()
        spec.definitions.keys.forEach { addDefinitionToDocument(it, document) }
        spec.paths.keys.forEach { addPathToDocument(it, document) }
        return document
    }

    private fun addDefinitionToDocument(name: String, document: Document) {
        val definitionBody = spec.definitions[name]!!
        val entity = convertDefinition(definitionBody, name)
        document.entities.add(entity)
    }

    private fun addPathToDocument(path: String, document: Document) {
        val pathBody = spec.paths[path]!!

        pathBody.get?.also { addMethodToDocument(it, "get", path, document) }
        pathBody.post?.also { addMethodToDocument(it, "post", path, document) }
        pathBody.put?.also { addMethodToDocument(it, "put", path, document) }
        pathBody.patch?.also { addMethodToDocument(it, "patch", path, document) }
        pathBody.delete?.also { addMethodToDocument(it, "delete", path, document) }
    }

    private fun addMethodToDocument(method: Method, methodName: String, path: String, document: Document) {
        val fullPath = spec.basePath + path
        val pathBody = spec.paths[path]!!
        val description = listOf(method.summary.orEmpty(), method.description)
            .filter { it.isNotEmpty() }
            .joinToString("\n")

        val successResponseCode = method.responses.keys.first { it in 200..299 }
        val successResponse = method.responses[successResponseCode]
        val successResponseDefinitionName = successResponse?.schema?.definitionName() ?: "void"

        val endpoint = Endpoint(
            name = method.operationId,
            description = description.ifEmpty { null },
            dtype = dtypeMapping.getOrDefault(successResponseDefinitionName, successResponseDefinitionName),
            path = fullPath,
            verb = EndpointVerb.valueOf(methodName.uppercase()),
            arguments = pathBody.parameters.map { convertParameter(it) } + method.parameters.map { convertParameter(it) }
        )

        document.endpoints.add(endpoint)
    }

    private fun convertParameter(parameter: Parameter): MethodArgument {
        return MethodArgument(
            name = parameter.name,
            description = parameter.description,
            dtype = dtypeMapping.getOrElse(parameter.type.orEmpty()) { parameter.schema?.definitionName()!! },
            nullable = !parameter.required,
            default = if (parameter.required) UNSET else null,
            many = parameter.type == "array" || parameter.schema?.type == "array"
        )
    }

    private fun convertDefinition(definition: Definition, name: String): Entity {
        return Entity(
            name = name,
            fields = definition.properties.map { convertProperty(it.value, it.key, definition) }
        )
    }

    private fun convertProperty(property: Property, name: String, definition: Definition): Field {
        val required = definition.required.contains(name)
        return Field(
            name = name,
            description = property.description,
            dtype = dtypeMapping.getOrDefault(property.definitionName(), property.definitionName()),
            many = property.type == "array",
            nullable = !required,
            default = if (required) UNSET else null,
            enum = property.enum?.associateBy { it },
            enumPrefix = property.title,
        )
    }
}
