package org.codegen.schema

import kotlinx.serialization.Serializable
import org.codegen.generators.AllGeneratorsEnum

@Serializable
data class Extension(
    val dtype: String,
    val implementations: Map<AllGeneratorsEnum, DataType>,
)
