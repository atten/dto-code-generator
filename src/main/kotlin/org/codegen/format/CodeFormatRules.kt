package org.codegen.format

import org.codegen.AppConfiguration.version
import org.codegen.utils.EnvironmentUtils.Companion.substituteEnvVariables

data class CodeFormatRules(
    val filePrefix: () -> String,
    val headersPostfix: String,
    val entitiesSeparator: String,
) {
    companion object {
        private val HEADER = "Auto-generated by DTO-Codegen $version, do not edit"

        val KOTLIN =
            CodeFormatRules(
                filePrefix = { substituteEnvVariables("// ${HEADER}\npackage \${PACKAGE_NAME}\n\n") },
                headersPostfix = "\n\n",
                entitiesSeparator = "\n\n",
            )

        val PYTHON =
            CodeFormatRules(
                filePrefix = { "# ${HEADER}\n\n" },
                headersPostfix = "\n\n\n",
                entitiesSeparator = "\n\n\n",
            )
    }
}
