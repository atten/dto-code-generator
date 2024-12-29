package org.codegen

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import org.codegen.generators.AllGeneratorsEnum

@Parameters(commandDescription = "Console tool which generates language-specific data classes and validators from JSON-like schema")
class Args {
    @Parameter(description = "files/directories to parse", required = true)
    lateinit var inputFiles: List<String>

    @Parameter(names = ["-t", "--target"], required = true, description = "Target implementation. Available choices are:")
    lateinit var target: AllGeneratorsEnum

    @Parameter(names = ["-p", "--prefixed"], description = "If enabled, add prefix to all fields")
    var usePrefixed = false

    @Parameter(
        names = ["--include-path"],
        description = "Include only paths containing given strings",
        variableArity = true,
    )
    var indludePaths: List<String> = listOf()

    @Parameter(
        names = ["--exclude-path"],
        description = "Do not include paths containing given strings",
        variableArity = true,
    )
    var excludePaths: List<String> = listOf()

    @Parameter(
        names = ["-n", "--name"],
        description = "Generated class name (inferred from input files if not specified)",
    )
    var name = ""

    @Parameter(names = ["--help"], help = true)
    var help: Boolean = false

    @Parameter(names = ["--version", "-v"], description = "Display version and exit", help = true)
    var version: Boolean = false
}
