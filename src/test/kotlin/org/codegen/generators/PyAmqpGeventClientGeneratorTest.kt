package org.codegen.generators

import org.codegen.Args
import org.codegen.Builder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class PyAmqpGeventClientGeneratorTest {
    @Test
    fun endpoints() {
        val args = Args().also {
            it.target = AllGeneratorsEnum.PY_AMQP_GEVENT_CLIENT
            it.includeFiles = listOf(
                this.javaClass.getResource("/input/entities.json")!!.path,
            )
            it.inputFiles = listOf(
                this.javaClass.getResource("/input/endpoints.json")!!.path,
            )
        }

        val output = Builder(args).build()
        val expectedOutput = File(this.javaClass.getResource("PyAmqpGeventClientGenerator/endpointsOutput.py")!!.path).readText()

        Assertions.assertEquals(expectedOutput, output)
    }
}
