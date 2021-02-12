package generators

import kotlin.reflect.KClass

enum class AllGeneratorsEnum(val generator_class: KClass<out CodeGeneratorInterface>) {
    DJANGO_MODEL(DjangoModelCodeGenerator::class),
    PY_DATACLASS(PyDataclassCodeGenerator::class);
}