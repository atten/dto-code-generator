package org.codegen.generators

import org.codegen.dto.*
import org.codegen.extensions.*

class KtDataclassSerializableGenerator(proxy: AbstractCodeGenerator? = null) : KtDataclassGenerator(AllGeneratorsEnum.KT_SERIALIZABLE_DATACLASS, proxy) {
    private val builtinSerializableTypes = listOf("String", "Int", "Float", "Double", "Boolean")

    private fun getSerialName(field: Field): String {
        // use camel_case (pythonic) serial names by default. This may be customized further
        return field.name.normalize().snakeCase()
    }

    override fun buildFieldDefinition(field: Field): String {
        var definition = super.buildFieldDefinition(field)
        val serialName = getSerialName(field)

        // add both kotlin serializer and jackson annotations
        if (serialName != field.name.normalize().camelCase()) {
            headers.add("import kotlinx.serialization.SerialName")
            headers.add("import com.fasterxml.jackson.annotation.JsonProperty")
            definition = "@SerialName(\"$serialName\")\n$definition"
            definition = "@JsonProperty(\"$serialName\")\n$definition"
        }

        if (!builtinSerializableTypes.contains(getDtype(field.dtype).definition)) {
            headers.add("import kotlinx.serialization.Contextual")
            definition = "@Contextual\n$definition"
        }

        return definition
    }

    override fun buildEntity(entity: Entity, annotations: List<String>): String {
        headers.add("import kotlinx.serialization.Serializable")
        return super.buildEntity(entity, annotations = annotations + listOf("@Serializable"))
    }

}