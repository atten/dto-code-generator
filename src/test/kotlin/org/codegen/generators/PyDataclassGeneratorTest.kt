package org.codegen.generators

import Args
import generate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class PyDataclassGeneratorTest {
    @Test
    fun entities() {
        val args = Args()
        args.target = AllGeneratorsEnum.PY_DATACLASS
        args.inputFiles = listOf(
            this.javaClass.getResource("/input/entities.json")!!.path,
        )

        val output = generate(args)
        val expectedOutput = File(this.javaClass.getResource("PyDataclassGenerator/entitiesOutput.py")!!.path).readText()

        assertEquals(expectedOutput, output)
    }
}