package org.codegen.generators

import org.codegen.Args
import org.codegen.Builder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

class KtSerializableDataclassGeneratorTest {
    val args = Args().also {
        it.target = AllGeneratorsEnum.KT_SERIALIZABLE_DATACLASS
        it.inputFiles = listOf(
            this.javaClass.getResource("/input/entities.json")!!.path,
        )
    }

    @Test
    fun entities() {
        val output = Builder(args).build()
        val expectedOutput = File(this.javaClass.getResource("KtSerializableDataclassGenerator/entitiesOutput.kt")!!.path).readText()
        assertEquals(expectedOutput, output)
    }

    @Test
    fun entitiesJacksonEnabled() {
        System.setProperty("USE_JACKSON", "true")
        val output = Builder(args).build()
        val expectedOutput = File(this.javaClass.getResource("KtSerializableDataclassGenerator/entitiesOutputJacksonEnabled.kt")!!.path).readText()
        assertEquals(expectedOutput, output)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            System.setProperty("PACKAGE_NAME", "org.codegen.generators")
        }

        @JvmStatic
        @AfterAll
        fun teardown() {
            System.clearProperty("PACKAGE_NAME")
        }
    }
}
