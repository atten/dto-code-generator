package org.codegen.parser.openapi

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
        val document = Document(name = spec.info.title)
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
    ) = mapOf(
        "get" to pathBody.get,
        "post" to pathBody.post,
        "put" to pathBody.put,
        "patch" to pathBody.patch,
        "delete" to pathBody.delete,
    )
        .filterValues { it != null }
        .map { getEndpoint(it.value!!, it.key, path, document) }
        .map { document.endpoints.add(it) }

    private fun getEndpoint(
        method: Method,
        verb: String,
        path: String,
        document: Document,
    ): Endpoint {
        val fullPath = spec.basePath + path
        val pathBody = spec.paths[path]!!
        val description =
            setOf(
                method.summary,
                method.description,
            )
                .filter { it.isNotEmpty() }
                .sortedBy { it.length }
                .joinToString("\n")

        // use request body of any supported format
        val body =
            mapOf(
                Endpoint.EndpointEncoding.JSON to "application/json",
                Endpoint.EndpointEncoding.FORM to "application/x-www-form-urlencoded",
            )
                .mapValues { method.requestBody?.content?.get(it.value)?.schema }
                .filterValues { it != null }
                .mapValues { it.value!! }
                .mapValues {
                    val many = it.value.type == "array"
                    MethodArgument(
                        name = if (many) "values" else "value",
                        dtype = it.value.definitionName(),
                        many = many,
                    )
                }
                .toList()
                .firstOrNull()

        val endpointArguments =
            (pathBody.parameters + method.parameters)
                .filter { it.source != "header" }
                .flatMap { convertAnyParameter(it, document) }
                .toMutableList()
                .also { list -> body?.second?.let { list.add(it) } }
                .sortedBy { it.name }

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

        return Endpoint(
            name = getEndpointName(path, verb),
            description = description.ifEmpty { null },
            dtype = successResponseDefinitionName,
            path = fullPath,
            verb = EndpointVerb.valueOf(verb.uppercase()),
            arguments = endpointArguments,
            many = successResponseSchema?.type == "array",
            encoding = body?.first,
        )
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
        val description = StringJoiner("\n")
        parameter.description?.let { description.add(it) }
        parameter.schema?.enum?.let { description.add(it.joinToString(separator = " | ")) }

        return MethodArgument(
            name = parameter.name,
            description = description.toString(),
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

        val default =
            when {
                property.default == UNSET && !required -> null
                else -> property.default
            }

        return Field(
            name = name,
            serializedName = name,
            description = property.description,
            dtype = property.definitionName().let { dtypeMapping.getOrDefault(it, it) },
            many = property.type == "array",
            nullable = !required,
            default = default,
            enum = property.enum?.associateBy { it },
            enumPrefix = property.title,
        )
    }
}
