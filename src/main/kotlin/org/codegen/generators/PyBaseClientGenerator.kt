package org.codegen.generators

import org.codegen.format.CodeFormatRules
import org.codegen.format.camelCase
import org.codegen.format.snakeCase
import org.codegen.schema.Constants.Companion.EMPTY
import org.codegen.schema.Constants.Companion.UNSET
import org.codegen.schema.Endpoint
import org.codegen.schema.Entity
import org.codegen.schema.MethodArgument
import java.io.File
import java.util.*

abstract class PyBaseClientGenerator(proxy: AbstractCodeGenerator? = null) : AbstractCodeGenerator(
    CodeFormatRules.PYTHON,
    AllGeneratorsEnum.PY_MARSHMALLOW_DATACLASS,
    proxy,
) {
    protected val atomicJsonTypes = listOf("str", "float", "int", "None", "bool")

    // list if __all__ items
    protected val definedNames = mutableListOf<String>()

    private fun buildArgumentDefaultValue(argument: MethodArgument): String {
        val dtypeProps = getDtype(argument.dtype)
        return if (argument.default == UNSET) {
            ""
        } else if (argument.many) {
            // use immutable tuple for default value
            "()"
        } else {
            when (argument.default) {
                EMPTY -> "${dtypeProps.definition}()".replace("str()", "''")
                null -> "None"
                else -> dtypeProps.toGeneratedValue(argument.default)
            }
        }
    }

    override fun addCodePart(
        body: String,
        vararg names: String,
    ) {
        super.addCodePart(body, *names)
        // add missing names into __all__
        names
            .filter { it.isNotEmpty() && it !in definedNames }
            .forEach { definedNames.add(it) }
    }

    protected open fun buildMethodDefinition(
        name: String,
        arguments: List<String>,
        returnStatement: String,
        singleLine: Boolean? = null,
    ): String {
        when (singleLine) {
            true -> {
                val argumentsString = arguments.joinToString(separator = ", ")
                return "def $name($argumentsString)$returnStatement"
            }
            false -> {
                val argumentsString =
                    arguments.joinToString(separator = ",\n    ", prefix = "\n    ", postfix = ",\n") {
                        it.replace("\n", "\n    ")
                    }
                return "def $name($argumentsString)$returnStatement"
            }
            else -> {
                // auto-choice
                val oneLiner = buildMethodDefinition(name, arguments, returnStatement, singleLine = true)
                if (oneLiner.length > 120) {
                    return buildMethodDefinition(name, arguments, returnStatement, singleLine = false)
                }
                return oneLiner
            }
        }
    }

    protected open fun renderEndpointHeader(endpoint: Endpoint): String {
        val name = endpoint.name.snakeCase()
        val returnDtypeProps = getDtype(endpoint.dtype)
        val returnStatement =
            returnDtypeProps.definition
                .let {
                    if (!returnDtypeProps.isNative) {
                        // wrap into quotes if definition is listed below
                        "'$it'"
                    } else {
                        it
                    }
                }
                .let {
                    if (endpoint.many) {
                        headers.add("import typing as t")
                        if (endpoint.cacheable) "list[$it]" else "t.Iterator[$it]"
                    } else if (endpoint.nullable) {
                        headers.add("import typing as t")
                        "t.Optional[$it]"
                    } else {
                        it
                    }
                }
                .let { if (it == "None") ":" else " -> $it:" }
        val arguments = mutableListOf("self")

        for (argument in endpoint.argumentsSortedByDefaults) {
            val argName = argument.name.snakeCase()
            val dtypeProps = getDtype(argument.dtype)
            val argTypeName =
                dtypeProps.definition
                    .let {
                        if (!dtypeProps.isNative) {
                            // wrap into quotes if definition is listed below
                            "'$it'"
                        } else {
                            it
                        }
                    }
                    .let {
                        if (argument.many) {
                            headers.add("import typing as t")
                            "t.Sequence[$it]"
                        } else {
                            it
                        }
                    }
                    .let { if (argument.nullable) "t.Optional[$it]" else it }

            val argDefaultValue =
                buildArgumentDefaultValue(argument)
                    .let { if (it.isEmpty()) "" else "= $it" }

            val argDescription =
                if (!argument.description.isNullOrEmpty()) {
                    "# ${argument.description}".trim()
                } else {
                    ""
                }

            val argumentString = "$argDescription\n$argName: $argTypeName $argDefaultValue".trim()
            arguments.add(argumentString)
        }

        val lines = mutableListOf<String>()

        if (endpoint.cacheable) {
            headers.add("from funcy import memoize")
            lines.add("@memoize")
        }

        val singleLine =
            if (arguments.any { "\n" in it }) {
                false
            } else {
                null // auto-choice
            }

        lines.add(buildMethodDefinition(name, arguments, returnStatement, singleLine = singleLine))
        endpoint.description
            ?.replace("\n", "\n    ")
            ?.let {
                lines.add("    \"\"\"")
                lines.add("    $it")
                lines.add("    \"\"\"")
            }
        return lines.joinToString(separator = "\n")
    }

    abstract fun renderEndpointBody(endpoint: Endpoint): String

    private fun renderEndpoint(endpoint: Endpoint) =
        renderEndpointHeader(endpoint) +
            renderEndpointBody(endpoint)
                .let { if (it.isNotEmpty()) "\n$it" else it }
                .replace("\n", "\n    ")

    override fun renderEntityName(name: String) = name.camelCase()

    override fun renderEntity(entity: Entity): String {
        if (entity.fields.isNotEmpty()) {
            return PyMarshmallowDataclassGenerator(this).renderEntity(entity)
        }

        // client class with endpoints
        val className = renderEntityName(entity.name)
        val classDefinition = "class $className:"
        val classBody = getMainApiClassBody()
        val classMethods = entity.endpoints.map { renderEndpoint(it) }
        return classMethods.joinToString(
            separator = "\n\n",
            prefix = "${classDefinition}\n${classBody}\n\n",
            postfix = CodeFormatRules.PYTHON.entitiesSeparator,
        ) { "    ${it.replace("\n", "\n    ")}" }
    }

    override fun renderBodyPrefix(): String {
        listOf(
            "/templates/python/baseSchema.py",
        )
            .map { this.javaClass.getResource(it)!!.path }
            .map { File(it).readText() }
            .map { addCodePart(it) }

        // put main client class on top of the file
        val clientEntity = entities.first { it.endpoints.isNotEmpty() }
        val clientBody = renderEntity(clientEntity)
        definedNames.add(renderEntityName(clientEntity.name))

        // do not render entities not included into client
        entities.clear()
        return clientBody
    }

    override fun renderBody(): String {
        getBodyIncludedFiles()
            .map { this.javaClass.getResource(it)!!.path }
            .map { File(it).readText() }
            .map { addCodePart(it) }

        return super.renderBody()
    }

    abstract fun getMainApiClassBody(): String

    abstract fun getBodyIncludedFiles(): List<String>

    override fun renderBodySuffix(): String {
        val suffix = StringJoiner(CodeFormatRules.PYTHON.entitiesSeparator, CodeFormatRules.PYTHON.entitiesSeparator, "\n")
        definedNames
            .sorted()
            .joinToString("\n", "__all__ = [\n", "\n]") { "    \"${it}\"," }
            .also { suffix.add(it) }
        return suffix.toString()
    }
}
