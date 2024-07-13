package org.codegen.utils

import java.util.Optional

class EnvironmentUtils {
    companion object {

        fun getEnvVariable(name: String): Optional<String> {
            val value = System.getenv().getOrElse(name) { System.getProperties()[name] }
            return if (value == null || value.toString().isEmpty()) Optional.empty() else Optional.of(value.toString())
        }

        fun getRequiredEnvVariable(name: String): String = getEnvVariable(name).orElseThrow { RuntimeException("No value present for env variable ENTITY_NAME") }

        fun getEnvFlag(name: String): Boolean {
            val value = getEnvVariable(name)
            return if (value.isPresent) listOf("true", "True", "1").contains(value.get()) else false
        }

        /**
         * "text_${SHELL}" -> "text_/bin/bash/"
         */
        fun substituteEnvVariables(input: String, required: Boolean = true): String {
            var str = input
            System.getenv().forEach { (key, value) -> str = replacePlaceholder(str, key, value) }
            System.getProperties().forEach { (key, value) -> str = replacePlaceholder(str, key.toString(), value.toString()) }
            require(!required || !str.contains("\${")) { "One or many env variables are missing: $str" }
            return str
        }

        private fun replacePlaceholder(input: String, key: String, value: String): String = input.replace("\${$key}", value)
    }
}
