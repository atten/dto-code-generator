package org.codegen.schema

import kotlinx.serialization.Serializable

@Serializable
data class Validator(
    // error message
    val message: String,
    val conditions: List<List<String>>,
)
