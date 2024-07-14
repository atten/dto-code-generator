package org.codegen.generators

import kotlin.reflect.KClass

@Suppress("unused")
enum class AllGeneratorsEnum(val generatorClass: KClass<out AbstractCodeGenerator>) {
    KT_DATACLASS(KtDataclassGenerator::class),
    KT_SERIALIZABLE_DATACLASS(KtDataclassSerializableGenerator::class),
    KT_INTERFACE(KtInterfaceGenerator::class),
    PY_ABSTRACT_CLASS(PyAbstractClassGenerator::class),
    PY_DJANGO_MODEL(PyDjangoModelGenerator::class),
    PY_API_CLIENT(PyApiClientGenerator::class),
    PY_API_ASYNC_CLIENT(PyApiAsyncClientGenerator::class),
    PY_AMQP_BLOCKING_CLIENT(PyAmqpBlockingClientGenerator::class),
    PY_AMQP_GEVENT_CLIENT(PyAmqpGeventClientGenerator::class),
    PY_MARSHMALLOW_DATACLASS(PyDataclassMarshmallowGenerator::class),
    PY_DATACLASS(PyDataclassGenerator::class);

    /**
     * define generator aliases for dtype implementation
     */
    private fun dtypeAlias(): AllGeneratorsEnum? = when (this) {
        KT_INTERFACE -> KT_DATACLASS
        KT_SERIALIZABLE_DATACLASS -> KT_DATACLASS
        PY_ABSTRACT_CLASS -> PY_MARSHMALLOW_DATACLASS
        PY_API_CLIENT -> PY_MARSHMALLOW_DATACLASS
        PY_API_ASYNC_CLIENT -> PY_API_CLIENT
        PY_AMQP_BLOCKING_CLIENT -> PY_MARSHMALLOW_DATACLASS
        PY_AMQP_GEVENT_CLIENT -> PY_AMQP_BLOCKING_CLIENT
        PY_MARSHMALLOW_DATACLASS -> PY_DATACLASS
        else -> null
    }

    /**
     * List of all chained aliases.
     */
    fun dtypeAliases(): List<AllGeneratorsEnum> {
        val candidates = mutableListOf(this)
        dtypeAlias() ?.let {
            candidates += it.dtypeAliases()
        }
        return candidates
    }
}
