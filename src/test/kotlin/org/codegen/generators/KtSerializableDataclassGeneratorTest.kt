package org.codegen.generators

import org.codegen.Args
import org.codegen.Builder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class KtSerializableDataclassGeneratorTest {
    @Test
    fun entities() {
        val args = Args().also {
            it.target = AllGeneratorsEnum.KT_SERIALIZABLE_DATACLASS
            it.inputFiles = listOf(
                this.javaClass.getResource("/input/entities.json")!!.path,
            )
        }

        System.setProperty("PACKAGE_NAME", "org.codegen.generators")

        val output = Builder(args).build()
        val expectedOutput = File(this.javaClass.getResource("KtSerializableDataclassGenerator/entitiesOutput.kt")!!.path).readText()

        assertEquals(expectedOutput, output)
    }
}
