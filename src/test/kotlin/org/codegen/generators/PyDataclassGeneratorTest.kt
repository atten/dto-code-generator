package org.codegen.generators

import org.codegen.Args
import org.codegen.Builder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

class PyDataclassGeneratorTest {
    val args =
        Args().also {
            it.target = AllGeneratorsEnum.PY_DATACLASS
            it.inputFiles =
                listOf(
                    this.javaClass.getResource("/input/entities.json")!!.path,
                )
        }

    @Test
    fun entities() {
        val output = Builder(args).build()
        val expectedOutput = File(this.javaClass.getResource("PyDataclassGenerator/entitiesOutput.py")!!.path).readText()
        assertEquals(expectedOutput, output)
    }

    @Test
    fun entitiesCustomDecoratorArgs() {
        System.setProperty("DECORATOR_ARGS", "frozen=True")
        val output = Builder(args).build()
        val expectedOutput = File(this.javaClass.getResource("PyDataclassGenerator/entitiesCustomDecoratorArgsOutput.py")!!.path).readText()
        assertEquals(expectedOutput, output)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            System.setProperty("DECORATOR_ARGS", "")
        }

        @JvmStatic
        @AfterAll
        fun teardown() {
            System.clearProperty("DECORATOR_ARGS")
        }
    }
}
