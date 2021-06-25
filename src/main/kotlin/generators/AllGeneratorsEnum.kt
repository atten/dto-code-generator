package generators

import kotlin.reflect.KClass

enum class AllGeneratorsEnum(val generator_class: KClass<out CodeGeneratorInterface>) {
    DJANGO_MODEL(DjangoModelCodeGenerator::class),
    MARSHMALLOW_DATACLASS(MarshmallowDataclassCodeGenerator::class);
}