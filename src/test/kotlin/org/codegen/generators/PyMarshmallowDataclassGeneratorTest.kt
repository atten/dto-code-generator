package org.codegen.generators

import org.codegen.Args
import org.codegen.Builder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class PyMarshmallowDataclassGeneratorTest {
    @Test
    fun entities() {
        val args = Args()
        args.target = AllGeneratorsEnum.PY_MARSHMALLOW_DATACLASS
        args.inputFiles = listOf(
            this.javaClass.getResource("/input/entities.json")!!.path,
        )

        val output = Builder(args).build()
        val expectedOutput = File(this.javaClass.getResource("PyMarshmallowGenerator/entitiesOutput.py")!!.path).readText()

        assertEquals(expectedOutput, output)
    }
}
