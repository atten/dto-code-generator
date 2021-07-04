package org.codegen.generators

import org.codegen.dto.*

const val EMPTY_PLACEHOLDER = "EMPTY"

abstract class AbstractCodeGenerator {
    // mapping of data types by name
    private val dtypeAttrs = mutableMapOf<String, DtypeAttributesMapping>()
    // entities included into output
    protected val entities = mutableListOf<Entity>()
    // available entities to refer to, but not include into output
    private val includedEntities = mutableListOf<Entity>()

    fun addEntity(entity: Entity, output: Boolean = true) {
        val destination = if (output) entities else includedEntities
        if (!destination.contains(entity))
            destination.add(entity)
    }

    fun addDtypeExtension(dtype: String, attrs: DtypeAttributesMapping) {
        dtypeAttrs[dtype] = attrs
    }

    protected fun getDtype(name: String): DtypeAttributesMapping {
        return requireNotNull(dtypeAttrs[name]) { "Missing extension for dtype '${name}'" }
    }

    protected fun getIncludedEntity(name: String): Entity {
        val parent =  includedEntities.firstOrNull { it.name == name }
        requireNotNull(parent) { "Missing parent entity '$name'. Probably you forgot to include corresponding file: --include=<file>" }
        return parent
    }

    abstract fun build(): String
}
