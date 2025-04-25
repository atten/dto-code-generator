package org.codegen

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import org.codegen.generators.AllGeneratorsEnum

@Parameters(commandDescription = "Console tool which generates language-specific data classes and validators from OpenApi schema")
class Args {
    @Parameter(
        description = "<files/directories/urls...>",
        required = true,
    )
    lateinit var inputPaths: List<String>

    @Parameter(
        names = ["-t", "--target"],
        required = true,
        description = "Target implementation",
        order = 0,
    )
    lateinit var target: AllGeneratorsEnum

    @Parameter(
        names = ["-n", "--name"],
        description = "Generated class name (inferred from input files if not specified)",
        order = 1,
    )
    var name = ""

    @Parameter(
        names = ["--include-url-path"],
        description = "Include only paths containing given strings",
        variableArity = true,
        order = 10,
    )
    var includeUrlPaths: List<String> = listOf()

    @Parameter(
        names = ["--exclude-url-path"],
        description = "Do not include paths containing given strings",
        variableArity = true,
        order = 11,
    )
    var excludeUrlPaths: List<String> = listOf()

    @Parameter(
        names = ["-p", "--prefixed"],
        description = "If enabled, add prefix to all fields",
        order = 12,
    )
    var usePrefixed = false

    @Parameter(
        names = ["--help"],
        description = "Show help usage and exit",
        help = true,
        order = 998,
    )
    var help: Boolean = false

    @Parameter(
        names = ["--version", "-v"],
        description = "Display version and exit",
        help = true,
        order = 999,
    )
    var version: Boolean = false
}
