package org.codegen.generators

import kotlin.reflect.KClass

@Suppress("unused")
enum class AllGeneratorsEnum(val generator_class: KClass<out CodeGeneratorInterface>) {
    DJANGO_MODEL(DjangoModelCodeGenerator::class),
    MARSHMALLOW_DATACLASS(MarshmallowDataclassCodeGenerator::class);
}
