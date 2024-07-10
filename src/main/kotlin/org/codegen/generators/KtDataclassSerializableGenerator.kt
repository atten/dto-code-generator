package org.codegen.generators

import org.codegen.format.*
import org.codegen.schema.Entity
import org.codegen.schema.Field
import org.codegen.utils.EnvironmentUtils.Companion.getEnvFlag

/**
 * Supports kotlin serializer and jackson annotations
 */
class KtDataclassSerializableGenerator(proxy: AbstractCodeGenerator? = null) : KtDataclassGenerator(AllGeneratorsEnum.KT_SERIALIZABLE_DATACLASS, proxy) {
    private val builtinSerializableTypes = listOf("String", "Int", "Float", "Double", "Boolean")
    private val useJackson = getEnvFlag("USE_JACKSON")
    private val useKotlinX = true

    private fun getSerialName(field: Field): String {
        // use camel_case (pythonic) serial names by default. This may be customized further
        return field.name.snakeCase()
    }

    override fun buildFieldDefinition(field: Field): String {
        var definition = super.buildFieldDefinition(field)
        val serialName = getSerialName(field)

        if (serialName != field.name.camelCase().lowercaseFirst()) {
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

            definition = if (field.many) {
                definition.replace("List<", "List<@Contextual ")
            } else {
                "@Contextual\n$definition"
            }
        }

        return definition
    }

    override fun buildEnumItem(key: String): String {
        return super.buildEnumItem(key).let {
            if (buildEnumLiteral(key) == key)
                it
            else {
                var annotated = it
                if (useKotlinX) {
                    headers.add("import kotlinx.serialization.SerialName")
                    annotated = "@SerialName(\"$key\")\n$it"
                }
                annotated
            }
        }
    }

    override fun buildEntity(entity: Entity, annotations: List<String>): String {
        val updatedAnnotations = annotations.toMutableList()
        if (useKotlinX) {
            headers.add("import kotlinx.serialization.Serializable")
            updatedAnnotations.add("@Serializable")
        }
        return super.buildEntity(entity, annotations = updatedAnnotations)
    }

    override fun addDefinition(body: String, vararg names: String) {
        if ("enum class" in body && "@SerialName" in body && useKotlinX) {
            // detect enum with serializable items and wrap it with annotation
            headers.add("import kotlinx.serialization.Serializable")
            super.addDefinition("@Serializable\n$body", *names)
        } else {
            super.addDefinition(body, *names)
        }
    }
}
