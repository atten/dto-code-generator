package generators

import org.codegen.common.dto.*

interface CodeGeneratorInterface {
    fun addEntity(entity: Entity)
    fun addDtypeExtension(dtype: String, attrs: DtypeAttributesMapping)
    fun build(): String
}