package org.codegen

import org.codegen.generators.AllGeneratorsEnum
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ArgsParserTest {
    @Test
    fun parseOk() {
        val parser = ArgsParser(arrayOf("file1.json", "file2.json", "-t", "KT_DATACLASS", "--prefixed", "--insecure"))
        val args = parser.parse()

        assertEquals(2, args!!.inputPaths.size)
        assertEquals(true, args.usePrefixed)
        assertEquals(true, args.insecureRequests)
        assertEquals(AllGeneratorsEnum.KT_DATACLASS, args.target)
    }

    @Test
    fun parseHelp() {
        val parser = ArgsParser(arrayOf("--help"))
        assertNull(parser.parse())
    }

    @Test
    fun parseEmpty() {
        val parser = ArgsParser(arrayOf())
        assertNull(parser.parse())
    }

    @Test
    fun parseFail() {
        val parser = ArgsParser(arrayOf("abc"))
        assertNull(parser.parse())
    }
}
