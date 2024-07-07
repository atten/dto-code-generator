package org.codegen.schema

import kotlinx.serialization.Serializable

@Serializable
data class Validator(
    val message: String, // error message
    val conditions: List<List<String>>,
)
