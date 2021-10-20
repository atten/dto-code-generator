package org.codegen.generators

import org.codegen.dto.*
import org.codegen.extensions.*

/**
 * Supports kotlin serializer and jackson annotations
 */
class KtDataclassSerializableGenerator(proxy: AbstractCodeGenerator? = null) : KtDataclassGenerator(AllGeneratorsEnum.KT_SERIALIZABLE_DATACLASS, proxy) {
    private val builtinSerializableTypes = listOf("String", "Int", "Float", "Double", "Boolean")
    private val useJackson = System.getenv()["USE_JACKSON"] == "1"
    private val useKotlinX = true

    private fun getSerialName(field: Field): String {
        // use camel_case (pythonic) serial names by default. This may be customized further
        return field.name.normalize().snakeCase()
    }

    override fun buildFieldDefinition(field: Field): String {
        var definition = super.buildFieldDefinition(field)
        val serialName = getSerialName(field)

        if (serialName != field.name.normalize().camelCase()) {
            if (useKotlinX) {
                headers.add("import kotlinx.serialization.SerialName")
                definition = "@SerialName(\"$serialName\")\n$definition"
            }

            if (useJackson) {
                headers.add("import com.fasterxml.jackson.annotation.JsonProperty")
                definition = "@JsonProperty(\"$serialName\")\n$definition"
            }
        }

        if (useKotlinX && !builtinSerializableTypes.contains(getDtype(field.dtype).definition)) {
            headers.add("import kotlinx.serialization.Contextual")
            definition = "@Contextual\n$definition"
        }

        return definition
    }

    override fun buildEntity(entity: Entity, annotations: List<String>): String {
        val updatedAnnotations = annotations.toMutableList()
        if (useKotlinX) {
            headers.add("import kotlinx.serialization.Serializable")
            updatedAnnotations.add("@Serializable")
        }
        return super.buildEntity(entity, annotations = updatedAnnotations)
    }

}