package org.codegen.generators

import org.codegen.Args
import org.codegen.Builder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class PyDataclassGeneratorTest {
    @Test
    fun entities() {
        val args = Args().also {
            it.target = AllGeneratorsEnum.PY_DATACLASS
            it.inputFiles = listOf(
                this.javaClass.getResource("/input/entities.json")!!.path,
            )
        }
        System.setProperty("DECORATOR_ARGS", "")

        val output = Builder(args).build()
        val expectedOutput = File(this.javaClass.getResource("PyDataclassGenerator/entitiesOutput.py")!!.path).readText()

        assertEquals(expectedOutput, output)
    }
}
