package org.codegen.schema.openapi

import org.codegen.schema.*
import org.codegen.schema.Constants.Companion.UNSET
import java.util.StringJoiner

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
        verb: String,
        path: String,
        document: Document,
    ) {
        val fullPath = spec.basePath + path
        val pathBody = spec.paths[path]!!
        val description =
            listOf(
                method.summary.orEmpty(),
                method.description,
            )
                .filter { it.isNotEmpty() }
                .joinToString("\n")

        val endpointParameters = (pathBody.parameters + method.parameters).filter { it.source != "header" }
        val endpointArguments = endpointParameters.flatMap { convertAnyParameter(it, document) }.toMutableList()

        // if requestBody is defined, then add it as method argument
        method.requestBody?.content?.get("application/json")?.schema?.let {
            MethodArgument(
                name = "value",
                dtype = it.definitionName(),
            )
        }?.let {
            endpointArguments.add(it)
        }

        val successResponseCode = method.responses.keys.first { it in 200..299 }
        val successResponse = method.responses[successResponseCode]
        val successResponseSchema =
            if (successResponse?.schema != null) {
                successResponse.schema
            } else if (successResponse?.content?.isNotEmpty() == true) {
                successResponse.content.filterKeys {
                        encoding ->
                    encoding in listOf("application/json", "*/*")
                }.values.map { it.schema }.first()
            } else {
                null
            }
        val successResponseDefinitionName = (successResponseSchema?.definitionName() ?: "void").let { dtypeMapping.getOrDefault(it, it) }

        val endpoint =
            Endpoint(
                name = getEndpointName(path, verb),
                description = description.ifEmpty { null },
                dtype = successResponseDefinitionName,
                path = fullPath,
                verb = EndpointVerb.valueOf(verb.uppercase()),
                arguments = endpointArguments,
                many = successResponseSchema?.type == "array",
            )

        document.endpoints.add(endpoint)
    }

    private fun getEndpointName(
        path: String,
        verb: String,
    ): String {
        val builder = StringJoiner(" ")
        builder.add(verb.lowercase())
        path.split("/").map { it.replace("{", "by ").replace("}", "") }.map { builder.add(it) }
        return builder.toString()
    }

    private fun convertAnyParameter(
        parameter: Parameter,
        document: Document,
    ): List<MethodArgument> {
        val definitionName = parameter.schema?.definitionName()
        return if (parameter.source == "query" && definitionName != null && definitionName !in dtypeMapping) {
            // replace complex query parameter with nested fields
            convertComplexQueryParameter(parameter, document)
        } else {
            listOf(convertGenericParameter(parameter))
        }
    }

    private fun convertComplexQueryParameter(
        parameter: Parameter,
        document: Document,
    ): List<MethodArgument> {
        val entityName = parameter.schema?.definitionName()
        val entity = document.entities.first { it.name == entityName }
        return entity.fields.map { it.toMethodArgument() }
    }

    private fun convertGenericParameter(parameter: Parameter): MethodArgument {
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
            serializedName = name,
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
