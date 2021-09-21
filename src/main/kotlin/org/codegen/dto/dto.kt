package org.codegen.dto

import org.codegen.generators.*
import kotlinx.serialization.*

const val UNSET = "<UNSET>"

@Serializable
data class DataType(
    val definition: String,
    val definitionArguments: Map<String, String> = mapOf(),
    val includeMetadataIntoDefinition: Boolean = true,
    val valuesMapping: Map<String, String> = mapOf(),
    val valueWrapper: String? = null,
    val requiredHeaders: List<String> = listOf(),
    // list of related entity names. If entities aren't present in target file, they will be included (if found in included schemas)
    val requiredEntities: List<String> = listOf(),
) {
    fun toGeneratedValue(value: String): String {
        var result = valuesMapping.getOrDefault(value, value)
        valueWrapper?.also {
            result = it.replace("%s", result)
        }
        return result
    }
}

@Serializable
data class Extension(
    val dtype: String,
    val implementations: Map<AllGeneratorsEnum, DataType>
) {
    fun getForGenerator(type: AllGeneratorsEnum): DataType? {
        for (alias in type.dtypeAliases()) {
            if (implementations.containsKey(alias))
                return implementations[alias]
        }
        return null
    }
}

@Serializable
data class Field (
    val name: String,
    val dtype: String,
    val shortDescription: String? = null,
    val longDescription: String? = null,
    val metadata: MutableMap<String, String> = mutableMapOf(),
    val enum: Map<String, String>? = null,  // mappings <CONSTANT, REPRESENTATION> for field with fixed choices
    val enumPrefix: String? = null,
    val default: String? = UNSET,
    val nullable: Boolean = false,
    val multiple: Boolean = false,
    val serializedName: String? = null,     // serialization/deserialization key
    val excludeFromSerialization: Boolean = false,
) {
    val isEnum: Boolean = !enum.isNullOrEmpty()
}

@Serializable
data class MethodArgument (
    val name: String,
    val dtype: String,
    val description: String? = null,
    val enum: Map<String, String>? = null,  // mappings <CONSTANT, REPRESENTATION> for field with fixed choices
    val enumPrefix: String? = null,
    val default: String? = UNSET,
    val nullable: Boolean = false,
    val multiple: Boolean = false,   // whether more than one value can be provided
) {
    val isEnum: Boolean = !enum.isNullOrEmpty()

    fun toField() = Field(
        name = name,
        dtype = dtype,
        longDescription = description,
        enum = enum,
        enumPrefix = enumPrefix,
        nullable = nullable,
        multiple = multiple,
        default = default
    )
}

@Serializable
data class Validator (
    val message: String,    // error message
    val conditions: List<List<String>>,
)

@Serializable
data class Property (
    val name: String,
    val dtype: String,
    val expression: List<String>,
)

@Serializable
data class Method(
    val name: String,
    val description: String? = null,
    val arguments: List<MethodArgument>,
    val dtype: String = "void",     // return value dtype (does not return anything by default)
    val nullable: Boolean = false,  // whether return value can be null
    val multiple: Boolean = false,  // whether array of values is returned
) {
    fun toEntity() = Entity(name="$name request", fields = arguments.map { it.toField() })
}

enum class EndpointVerb {
    GET,
    POST,
}

@Serializable
data class Endpoint(
    val name: String,
    val description: String? = null,
    val arguments: List<MethodArgument>,
    val dtype: String,     // return value dtype
    val path: String,     // HTTP path (may include arguments in format: "/api/v1/path/{arg1}/{arg2}"
    val nullable: Boolean = false,  // whether return value can be null
    val multiple: Boolean = false,  // whether array of values is returned
    val verb: EndpointVerb = EndpointVerb.GET,
) {
    fun toMethod() = Method(name=name, description=description, arguments=arguments, dtype=dtype, nullable=nullable, multiple=multiple)
}

@Serializable
data class Entity(
    val name: String,
    val fields: List<Field> = listOf(),
    val validators: List<Validator> = listOf(),
    val properties: List<Property> = listOf(),
    val methods: MutableList<Method> = mutableListOf(),
    val endpoints: MutableList<Endpoint> = mutableListOf(),
    val parent: String? = null,
    val description: String? = null,
    val prefix: String? = null,
) {
    val actualPrefix = prefix ?: name
    val fieldNames = fields.map { it.name }

    fun toDataType() = DataType(definition = name, requiredEntities = listOf(name))

    fun prefixedFields() = this.copy(fields = fields.map { it.copy(name = "$actualPrefix ${it.name}") })

    fun methodsPlusEndpoints() = methods + endpoints.map { it.toMethod() }
}

@Serializable
data class Document(
    val extensions: List<Extension> = listOf(),
    val entities: List<Entity> = listOf(),
    // root-level methods will be inserted to default entity
    val methods: List<Method> = listOf(),
    // root-level endpoints will be inserted to default entity
    val endpoints: List<Endpoint> = listOf(),
)
