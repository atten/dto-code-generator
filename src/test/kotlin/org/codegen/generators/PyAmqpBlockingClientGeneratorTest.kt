package org.codegen.generators

import org.codegen.Args
import org.codegen.Builder
import org.junit.jupiter.api.*
import java.io.File

class PyAmqpBlockingClientGeneratorTest {
    @Test
    fun endpoints() {
        val args =
            Args().also {
                it.target = AllGeneratorsEnum.PY_AMQP_BLOCKING_CLIENT
                it.inputFiles =
                    listOf(
                        this.javaClass.getResource("/input/entities.json")!!.path,
                        this.javaClass.getResource("/input/endpoints.json")!!.path,
                    )
            }

        val output = Builder(args).build()
        val expectedOutput = File(this.javaClass.getResource("PyAmqpBlockingClientGenerator/endpointsOutput.py")!!.path).readText()
        Assertions.assertEquals(expectedOutput, output)
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
