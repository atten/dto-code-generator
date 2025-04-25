package org.codegen.generators

import org.codegen.Args
import org.codegen.Builder
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File

class PyApiClientGeneratorTest {
    @Test
    fun endpoints() {
        val args =
            Args().also {
                it.target = AllGeneratorsEnum.PY_API_CLIENT
                it.inputPaths =
                    listOf(
                        this.javaClass.getResource("/input/entities.json")!!.path,
                        this.javaClass.getResource("/input/endpoints.json")!!.path,
                    )
            }

        val output = Builder(args).build()
        val expectedOutput = File(this.javaClass.getResource("PyApiClientGenerator/endpointsOutput.py")!!.path).readText()
        assertEquals(expectedOutput, output)
    }

    @Test
    fun openApiJson() {
        val args =
            Args().also {
                it.target = AllGeneratorsEnum.PY_API_CLIENT
                it.inputPaths =
                    listOf(
                        this.javaClass.getResource("/input/openApi.json")!!.path,
                    )
            }

        val output = Builder(args).build()
        val expectedOutput = File(this.javaClass.getResource("PyApiClientGenerator/openApiOutput.py")!!.path).readText()
        assertEquals(expectedOutput, output)
    }

    @Test
    fun openApiYaml() {
        val args =
            Args().also {
                it.target = AllGeneratorsEnum.PY_API_CLIENT
                it.inputPaths =
                    listOf(
                        this.javaClass.getResource("/input/openApi.yaml")!!.path,
                    )
            }

        val output = Builder(args).build()
        val expectedOutput = File(this.javaClass.getResource("PyApiClientGenerator/openApiOutput.py")!!.path).readText()
        assertEquals(expectedOutput, output)
    }

    @Test
    fun openApiJsonWithCustomName() {
        val args =
            Args().also {
                it.target = AllGeneratorsEnum.PY_API_CLIENT
                it.name = "SomeRestApiCustom"
                it.inputPaths =
                    listOf(
                        this.javaClass.getResource("/input/openApi.json")!!.path,
                    )
            }

        val output = Builder(args).build()
        val expectedOutput =
            File(this.javaClass.getResource("PyApiClientGenerator/openApiOutput.py")!!.path).readText()
                .replace("SomeRestApi", "SomeRestApiCustom")
        assertEquals(expectedOutput, output)
    }

    @Test
    fun openApiJsonPartial() {
        val args =
            Args().also {
                it.target = AllGeneratorsEnum.PY_API_CLIENT
                it.inputPaths =
                    listOf(
                        this.javaClass.getResource("/input/openApi.json")!!.path,
                    )
                it.excludeUrlPaths = listOf("basic/{entityId}/", "action")
            }

        val output = Builder(args).build()
        val fullOutput = File(this.javaClass.getResource("PyApiClientGenerator/openApiOutput.py")!!.path).readText()
        assertTrue(fullOutput.contains("action", ignoreCase = true))
        assertTrue(fullOutput.contains("basic/{entity", ignoreCase = true))
        assertFalse(output.contains("action", ignoreCase = true))
        assertFalse(output.contains("basic/{entity", ignoreCase = true))
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
