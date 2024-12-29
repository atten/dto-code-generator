package org.codegen.format

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StringKtTest {
    @Test
    fun testNormalize() {
        assertEquals("simple text", " Simple  Text ".normalize())
        assertEquals("simple text", "Simple Text".normalize())
        assertEquals("simple text", "SimpleText".normalize())
        assertEquals("simple text", "simpleText".normalize())
        assertEquals("simple text", "simple_text".normalize())
        assertEquals("simple text", "simple_text!".normalize())

        assertEquals("capslock", "CAPSLOCK".normalize())

        assertEquals("simple dto", "simple DTO".normalize())
        assertEquals("simple s3 file", "simple s3 file".normalize())
        assertEquals("simple s3 dto", "simple S3 DTO".normalize())
        assertEquals("simple 3d dto", "simple 3d DTO".normalize())
    }

    @Test
    fun testSnakeCase() {
        assertEquals("simple_text", "SimpleText".snakeCase())
        assertEquals("simple_text", "simpleText".snakeCase())
        assertEquals("simple_text", "simple_text".snakeCase())
        assertEquals("simple_text", "simple_text!".snakeCase())

        assertEquals("capslock", "CAPSLOCK".snakeCase())

        assertEquals("simple_dto", "simple DTO".snakeCase())
        assertEquals("simple_s3_file", "simple s3 file".snakeCase())
        assertEquals("simple_s3_dto", "simple S3 DTO".snakeCase())
        assertEquals("simple_3d_dto", "simple 3d DTO".snakeCase())
    }

    @Test
    fun testCamelCase() {
        assertEquals("SimpleText", "SimpleText".camelCase())
        assertEquals("SimpleText", "simpleText".camelCase())
        assertEquals("SimpleText", "simple_text".camelCase())
        assertEquals("SimpleText", "simple_text!".camelCase())

        assertEquals("Capslock", "CAPSLOCK".camelCase())

        assertEquals("SimpleDto", "simple DTO".camelCase())
        assertEquals("SimpleS3File", "simple s3 file".camelCase())
        assertEquals("SimpleS3Dto", "simple S3 DTO".camelCase())
        assertEquals("Simple3dDto", "simple 3d DTO".camelCase())
    }

    @Test
    fun testPluralize() {
        assertEquals("SimpleTexts", "SimpleText".pluralize())
        assertEquals("statuses", "status".pluralize())
        assertEquals("STATUSES", "STATUS".pluralize())
        assertEquals("REDUXES", "REDUX".pluralize())
        assertEquals("currencies", "currency".pluralize())
        assertEquals("CURRENCIES", "CURRENCY".pluralize())
        assertEquals("status DTOS", "status DTO".pluralize())
    }
}
