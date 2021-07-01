package org.codegen.generators

import org.codegen.dto.*

const val EMPTY_PLACEHOLDER = "EMPTY"

interface CodeGeneratorInterface {
    fun addEntity(entity: Entity)
    fun addDtypeExtension(dtype: String, attrs: DtypeAttributesMapping)
    fun build(): String
}
