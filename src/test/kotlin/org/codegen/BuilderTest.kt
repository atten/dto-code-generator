package org.codegen

import kotlinx.serialization.SerializationException
import org.codegen.generators.AllGeneratorsEnum
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BuilderTest {

    @Test
    fun parseSchemaWithUnknownFields() {
        val args = Args().also {
            it.target = AllGeneratorsEnum.PY_DJANGO_MODEL
            it.inputFiles = listOf(
                this.javaClass.getResource("/input/entities.json")!!.path,
                this.javaClass.getResource("/input/unknownSchemaFields.json")!!.path,
            )
        }
        val builder = Builder(args)
        assertThrows(SerializationException::class.java) { builder.build() }
    }
}
