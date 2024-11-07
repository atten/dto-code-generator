package org.codegen.format

import org.codegen.utils.EnvironmentUtils.Companion.substituteEnvVariables

data class CodeFormatRules(
    val filePrefix: () -> String = { "" },
    val headersPostfix: String = "\n\n",
    val entitiesSeparator: String = "\n",
) {
    companion object {
        private const val HEADER = "Auto-generated by DTO-Codegen, do not edit"

        val KOTLIN = CodeFormatRules(
            filePrefix = { substituteEnvVariables("// ${HEADER}\npackage \${PACKAGE_NAME}\n\n") },
            headersPostfix = "\n\n",
            entitiesSeparator = "\n\n",
        )

        val PYTHON = CodeFormatRules(
            filePrefix = { "# ${HEADER}\n\n" },
            headersPostfix = "\n\n\n",
            entitiesSeparator = "\n\n\n",
        )
    }
}
