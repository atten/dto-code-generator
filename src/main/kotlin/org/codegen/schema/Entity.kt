package org.codegen.schema

import kotlinx.serialization.Serializable
import org.codegen.schema.Constants.Companion.UNSET

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

    private val fieldNames = fields.map { it.name }
    private val propertyNames = properties.map { it.name }
    val attributeNames = fieldNames + propertyNames

    private val fieldsWithoutDefaults = fields.filter { it.default == UNSET }
    private val fieldsWithDefaults = fields.filter { it.default != UNSET }
    val fieldsSortedByDefaults = fieldsWithoutDefaults + fieldsWithDefaults

    fun toDataType() = DataType(definition = name, requiredEntities = listOf(name))

    fun prefixedFields() = this.copy(fields = fields.map { it.copy(name = "$actualPrefix ${it.name}") })

    fun methodsPlusEndpoints() = methods + endpoints.map { it.toMethod() }
}
