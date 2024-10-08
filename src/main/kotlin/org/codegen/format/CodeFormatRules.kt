package org.codegen.format

import org.codegen.utils.EnvironmentUtils.Companion.substituteEnvVariables

data class CodeFormatRules(
    val filePrefix: () -> String = { "" },
    val headersPostfix: String = "\n\n",
    val entitiesSeparator: String = "\n",
) {
    companion object {
        val KOTLIN = CodeFormatRules(
            filePrefix = { substituteEnvVariables("// Generated by DTO-Codegen\npackage \${PACKAGE_NAME}\n\n") },
            headersPostfix = "\n\n",
            entitiesSeparator = "\n\n",
        )

        val PYTHON = CodeFormatRules(
            filePrefix = { "# Generated by DTO-Codegen\n\n" },
            headersPostfix = "\n\n\n",
            entitiesSeparator = "\n\n\n",
        )
    }
}
