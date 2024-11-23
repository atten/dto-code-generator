package org.codegen.generators

import org.codegen.format.CodeFormatRules
import org.codegen.schema.DataType
import org.codegen.schema.Entity
import org.codegen.utils.EnvironmentUtils.Companion.getRequiredEnvVariable
import org.codegen.utils.EnvironmentUtils.Companion.substituteEnvVariables
import kotlin.reflect.full.primaryConstructor

abstract class AbstractCodeGenerator(
    private val codeFormatRules: CodeFormatRules,
    private val includedEntityType: AllGeneratorsEnum,
    private val parent: AbstractCodeGenerator? = null,
) {
    // mapping of data types by name
    private val dataTypes = mutableMapOf<String, DataType>()

    // entities included into output (own entities without parent)
    protected val entities = mutableListOf<Entity>()

    // available entities to refer to, but not include into output
    private val includedEntities = mutableListOf<Entity>()

    // collected headers at the top of the output file
    protected val headers = mutableSetOf<String>()
        get() = parent?.headers ?: field

    // built classes/enums (included entities)
    private val includedDefinitions = mutableListOf<String>()
        get() = parent?.includedDefinitions ?: field

    /**
     * construct implied entity with name from env
     */
    val defaultEntity: Entity by lazy {
        val name = getRequiredEnvVariable("ENTITY_NAME")
        Entity(name = name).also { addEntity(it) }
    }

    /**
     * Pick primary corresponding enum value and it's aliases.
     */
    private val target: AllGeneratorsEnum by lazy {
        AllGeneratorsEnum.entries.forEach { enum ->
            if (this::class == enum.generatorClass) {
                return@lazy enum
            }
        }
        throw RuntimeException("enum value is not assigned")
    }

    protected val includedEntityGenerator: AbstractCodeGenerator by lazy {
        if (target == includedEntityType) {
            return@lazy this
        } else {
            includedEntityType.generatorClass.primaryConstructor!!.call(this)
        }
    }

    protected open fun addDefinition(
        body: String,
        vararg names: String,
    ) {
        if (parent != null) {
            parent.addDefinition(body, *names)
        } else {
            body.trim().let { trimmed ->
                if (!includedDefinitions.contains(trimmed)) {
                    includedDefinitions.add(trimmed)
                }
            }
        }
    }

    fun addDataType(
        dtype: String,
        attrs: DataType,
    ) {
        if (parent != null) {
            parent.addDataType(dtype, attrs)
        } else {
            dataTypes[dtype] = attrs
        }
    }

    fun addEntity(
        entity: Entity,
        output: Boolean = true,
    ) {
        val destination = if (output) entities else includedEntities
        if (destination.contains(entity)) {
            // found exact match
            return
        }
        if (findEntity(entity.name) != null) {
            // found entity with same name
            throw IllegalArgumentException("Found different entities with same name: ${entity.name}")
        }
        destination.add(entity)
    }

    protected fun findEntity(name: String): Entity? {
        val allEntities = entities + includedEntities
        return allEntities.find { it.name == name } ?: parent?.findEntity(name)
    }

    protected fun getDtype(name: String): DataType {
        val dtype = parent?.getDtype(name) ?: dataTypes[name] ?: findEntity(name)?.toDataType()?.copy(definition = buildEntityName(name))
        requireNotNull(dtype) {
            "${this::class.simpleName}: Missing extension for dtype '$name'. Define it in any of following sections: ${target.dtypeAliases()}."
        }
        useDataType(dtype)
        return dtype
    }

    private fun useDataType(type: DataType) {
        // include headers
        type.requiredHeaders.forEach { headers.add(substituteEnvVariables(it)) }

        // include files
        type.loadIncludedFiles().forEach { addDefinition(it) }

        // add missing entities (if required)
        type.requiredEntities
            .forEach { name ->
                val entity = findEntity(name)
                requireNotNull(entity) {
                    "Missing required entity '$name'. Probably you forgot to include corresponding file: --include=<file>"
                }

                includedEntityGenerator
                    .buildEntity(entity)
                    .also { addDefinition(it, buildEntityName(name)) }
            }
    }

    /**
     * whether function/method/variable/class name is presented in code
     */
    private fun containsEntity(name: String): Boolean = includedDefinitions.find { it.contains(name) } != null

    abstract fun buildEntityName(name: String): String

    abstract fun buildEntity(entity: Entity): String

    private fun buildHeaders(): String {
        // sort alphabetically, exclude unused headers (keep ones with wildcard import)
        return headers
            .filter { header ->
                '*' in header || getEntityNamesFromHeader(header).map { containsEntity(it) }.any { it }
            }
            .sorted()
            .joinToString(
                "\n",
                prefix = codeFormatRules.filePrefix(),
                postfix = codeFormatRules.headersPostfix,
            )
    }

    protected open fun buildBodyPrefix() = ""

    protected open fun buildBodyPostfix() = "\n" // newline at the end by default

    protected open fun getEntityNamesFromHeader(header: String): List<String> {
        return listOf(header.split(" ", ".").last())
    }

    fun build(): String {
        val prefix = buildBodyPrefix()

        if (entities.isEmpty()) {
            // if document contains no explicitly added entities, then build all included entities
            entities.addAll(includedEntities)
        }

        for (entity in entities) {
            val body = buildEntity(entity)
            addDefinition(body, buildEntityName(entity.name))
        }

        val postfix = buildBodyPostfix()
        return buildHeaders() +
            prefix +
            includedDefinitions.joinToString(separator = codeFormatRules.entitiesSeparator) +
            postfix
    }
}
