package org.codegen.generators

import org.codegen.Args
import org.codegen.Builder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class PyAmqpBlockingClientGeneratorTest {
    @Test
    fun endpoints() {
        val args = Args()
        args.target = AllGeneratorsEnum.PY_AMQP_BLOCKING_CLIENT
        args.includeFiles = listOf(
            this.javaClass.getResource("/input/entities.json")!!.path,
        )
        args.inputFiles = listOf(
            this.javaClass.getResource("/input/endpoints.json")!!.path,
        )

        val output = Builder(args).build()
        val expectedOutput = File(this.javaClass.getResource("PyAmqpBlockingClientGenerator/endpointsOutput.py")!!.path).readText()

        Assertions.assertEquals(expectedOutput, output)
    }
}
