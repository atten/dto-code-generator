package org.codegen.schema

import kotlinx.serialization.Serializable
import org.codegen.generators.AllGeneratorsEnum

@Serializable
data class Extension(
    val dtype: String,
    val implementations: Map<AllGeneratorsEnum, DataType>,
) {
    fun getForGenerator(type: AllGeneratorsEnum): DataType? {
        for (alias in type.dtypeAliases()) {
            if (implementations.containsKey(alias)) {
                return implementations[alias]
            }
        }
        return null
    }
}
