package org.codegen.generators

import org.codegen.Args
import org.codegen.Builder
import org.junit.jupiter.api.*
import java.io.File

class PyApiClientGeneratorTest {
    @Test
    fun endpoints() {
        val args =
            Args().also {
                it.target = AllGeneratorsEnum.PY_API_CLIENT
                it.inputFiles =
                    listOf(
                        this.javaClass.getResource("/input/entities.json")!!.path,
                        this.javaClass.getResource("/input/endpoints.json")!!.path,
                    )
            }

        val output = Builder(args).build()
        val expectedOutput = File(this.javaClass.getResource("PyApiClientGenerator/endpointsOutput.py")!!.path).readText()
        Assertions.assertEquals(expectedOutput, output)
    }

    @Test
    fun openApi() {
        val args =
            Args().also {
                it.target = AllGeneratorsEnum.PY_API_CLIENT
                it.inputFiles =
                    listOf(
                        this.javaClass.getResource("/input/openApi.json")!!.path,
                    )
            }

        val output = Builder(args).build()
        val expectedOutput = File(this.javaClass.getResource("PyApiClientGenerator/openApiOutput.py")!!.path).readText()
        Assertions.assertEquals(expectedOutput, output)
    }

    @Test
    fun openApiWithCustomName() {
        val args =
            Args().also {
                it.target = AllGeneratorsEnum.PY_API_CLIENT
                it.name = "SomeRestApiCustom"
                it.inputFiles =
                    listOf(
                        this.javaClass.getResource("/input/openApi.json")!!.path,
                    )
            }

        val output = Builder(args).build()
        val expectedOutput =
            File(this.javaClass.getResource("PyApiClientGenerator/openApiOutput.py")!!.path).readText()
                .replace("SomeRestApi", "SomeRestApiCustom")
        Assertions.assertEquals(expectedOutput, output)
    }

    @Test
    fun openApiPartial() {
        val args =
            Args().also {
                it.target = AllGeneratorsEnum.PY_API_CLIENT
                it.inputFiles =
                    listOf(
                        this.javaClass.getResource("/input/openApi.json")!!.path,
                    )
                it.excludePaths = listOf("basic/{entityId}/", "action")
            }

        val output = Builder(args).build()
        val expectedOutput = File(this.javaClass.getResource("PyApiClientGenerator/openApiPartialOutput.py")!!.path).readText()
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
