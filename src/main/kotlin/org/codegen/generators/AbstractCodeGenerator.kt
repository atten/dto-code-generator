package org.codegen.generators

import org.codegen.dto.*
import org.codegen.extensions.*

const val EMPTY_PLACEHOLDER = "EMPTY"

abstract class AbstractCodeGenerator {
    // mapping of data types by name
    private val dataTypes = mutableMapOf<String, DataType>()
    // entities included into output
    private val entities = mutableListOf<Entity>()
    // available entities to refer to, but not include into output
    private val includedEntities = mutableListOf<Entity>()

    /**
     * construct implied entity with name from env
     */
    val defaultEntity: Entity by lazy {
        val namePlaceholder = "\${ENTITY_NAME}"
        Entity(
            name = namePlaceholder.substituteEnvVars().let { if (it == namePlaceholder) "untitled" else it }
        ).also { addEntity(it) }
    }

    fun addDataType(dtype: String, attrs: DataType) {
        dataTypes[dtype] = attrs
    }

    fun copyDataTypesFrom(generator: AbstractCodeGenerator) {
        generator.dataTypes.forEach { (name, dataType) -> addDataType(name, dataType) }
    }

    fun addEntity(entity: Entity, output: Boolean = true) {
        val destination = if (output) entities else includedEntities
        if (!destination.contains(entity))
            destination.add(entity)
    }

    protected fun getEntities(includeMethods: Boolean = false) =
        if (includeMethods) {
            entities + defaultEntity.methods.map { it.toEntity() } - defaultEntity
        } else {
            entities
        }

    protected fun getDtype(name: String): DataType {
        return requireNotNull(dataTypes[name]) { "${this}: Missing extension for dtype '${name}'" }
    }

    protected fun getIncludedEntity(name: String): Entity {
        val parent =  includedEntities.firstOrNull { it.name == name }
        requireNotNull(parent) { "Missing parent entity '$name'. Probably you forgot to include corresponding file: --include=<file>" }
        return parent
    }

    abstract fun build(): String
}
