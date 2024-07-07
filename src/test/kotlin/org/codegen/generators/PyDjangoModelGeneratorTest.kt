package org.codegen.generators

import org.codegen.Args
import org.codegen.Builder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class PyDjangoModelGeneratorTest {
    @Test
    fun entities() {
        val args = Args().also {
            it.target = AllGeneratorsEnum.PY_DJANGO_MODEL
            it.inputFiles = listOf(
                this.javaClass.getResource("/input/entities.json")!!.path,
            )
        }

        val output = Builder(args).build()
        val expectedOutput = File(this.javaClass.getResource("PyDjangoModelGenerator/entitiesOutput.py")!!.path).readText()

        assertEquals(expectedOutput, output)
    }
}
