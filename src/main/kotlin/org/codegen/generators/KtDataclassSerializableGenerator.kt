package org.codegen.generators

import org.codegen.dto.*
import org.codegen.extensions.*

class KtDataclassSerializableGenerator(proxy: AbstractCodeGenerator? = null) : KtDataclassGenerator(proxy) {
    private val builtinSerializableTypes = listOf("String", "Int", "Float", "Double", "Boolean")

    private fun getSerialName(field: Field): String {
        // use camel_case (pythonic) serial names by default. This may be customized further
        return field.name.normalize().snakeCase()
    }

    override fun buildFieldDefinition(field: Field): String {
        val str = super.buildFieldDefinition(field).let {
            val serialName = getSerialName(field)
            if (serialName != field.name.normalize().camelCase())
                "@SerialName(\"$serialName\")\n$it"
            else
                it
        }
        if (!builtinSerializableTypes.contains(getDtype(field.dtype).definition))
            return "@Contextual\n$str"
        return str
    }

    override fun buildEntity(entity: Entity, annotations: List<String>): String {
        addHeader("import kotlinx.serialization.*")
        return super.buildEntity(entity, annotations = annotations + listOf("@Serializable"))
    }

}