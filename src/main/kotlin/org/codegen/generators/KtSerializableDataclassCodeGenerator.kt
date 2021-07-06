package org.codegen.generators

import org.codegen.dto.*

class KtSerializableDataclassCodeGenerator: KtDataclassCodeGenerator() {
    private val builtinSerializableTypes = listOf("String", "Int", "Float", "Boolean")

    override fun buildFieldDefinition(field: Field): String {
        val str = super.buildFieldDefinition(field)
        if (!builtinSerializableTypes.contains(getDtype(field.dtype).definition))
            return "@Contextual\n$str"
        return str
    }

    override fun buildEntity(entity: Entity, annotations: List<String>): String {
        addHeader("import kotlinx.serialization.*")
        return super.buildEntity(entity, annotations = annotations + listOf("@Serializable"))
    }

}