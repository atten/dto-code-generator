package org.codegen

import org.codegen.generators.AllGeneratorsEnum
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArgsParserTest {

    @Test
    fun parseOk() {
        val parser = ArgsParser(arrayOf("file1.json", "file2.json", "-t", "KT_DATACLASS", "--prefixed"))
        val args = parser.parse().get()

        assertEquals(2, args.inputFiles.size)
        assertEquals(true, args.usePrefixed)
        assertEquals(AllGeneratorsEnum.KT_DATACLASS, args.target)
    }

    @Test
    fun parseHelp() {
        val parser = ArgsParser(arrayOf("--help"))
        assertTrue(parser.parse().isEmpty)
    }

    @Test
    fun parseEmpty() {
        val parser = ArgsParser(arrayOf())
        assertTrue(parser.parse().isEmpty)
    }

    @Test
    fun parseFail() {
        val parser = ArgsParser(arrayOf("abc"))
        assertTrue(parser.parse().isEmpty)
    }
}
