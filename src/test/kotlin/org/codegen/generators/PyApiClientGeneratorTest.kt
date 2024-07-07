package org.codegen.generators

import Args
import generate
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class PyApiClientGeneratorTest {
    @Test
    fun endpoints() {
        val args = Args()
        args.target = AllGeneratorsEnum.PY_API_CLIENT
        args.includeFiles = listOf(
            this.javaClass.getResource("/input/entities.json")!!.path,
        )
        args.inputFiles = listOf(
            this.javaClass.getResource("/input/endpoints.json")!!.path,
        )

        val output = generate(args)
        val expectedOutput = File(this.javaClass.getResource("PyApiClientGenerator/endpointsOutput.py")!!.path).readText()

        Assertions.assertEquals(expectedOutput, output)
    }
}