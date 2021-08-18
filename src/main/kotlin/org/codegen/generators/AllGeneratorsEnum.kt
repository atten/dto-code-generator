package org.codegen.generators

import kotlin.reflect.KClass

@Suppress("unused")
enum class AllGeneratorsEnum(val generatorClass: KClass<out AbstractCodeGenerator>) {
    KT_DATACLASS(KtDataclassCodeGenerator::class),
    KT_SERIALIZABLE_DATACLASS(KtSerializableDataclassCodeGenerator::class),
    KT_INTERFACE(KtInterfaceCodeGenerator::class),
    KT_API_INTERFACE(KtApiInterfaceCodeGenerator::class),
    PY_DJANGO_MODEL(PyDjangoModelCodeGenerator::class),
    PY_MARSHMALLOW_DATACLASS(PyMarshmallowDataclassCodeGenerator::class);

    /**
     * define generator aliases for dtype implementation
     */
    fun dtypeAlias(): AllGeneratorsEnum? = when(this) {
        KT_INTERFACE -> KT_DATACLASS
        KT_API_INTERFACE -> KT_DATACLASS
        KT_SERIALIZABLE_DATACLASS -> KT_DATACLASS
        else -> null
    }
}
