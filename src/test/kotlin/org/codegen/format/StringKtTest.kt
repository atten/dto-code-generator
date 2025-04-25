package org.codegen.format

import org.codegen.utils.camelCase
import org.codegen.utils.normalize
import org.codegen.utils.pluralize
import org.codegen.utils.snakeCase
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
        assertEquals("http validation error", "HTTPValidationError".normalize())

        assertEquals("simple s3 url", "simple s3 url".normalize())
        assertEquals("simple s3 url", "simpleS3Url".normalize())
        assertEquals("simple s3 url", "simple S3 URL".normalize())

        assertEquals("vector 2d", "vector2D".normalize())
        assertEquals("vector 2d dto", "vector 2d DTO".normalize())

        assertEquals("e2e asset generator", "E2eAssetGenerator".normalize())
    }

    @Test
    fun testSnakeCase() {
        assertEquals("simple_text", "SimpleText".snakeCase())
        assertEquals("simple_text", "simpleText".snakeCase())
        assertEquals("simple_text", "simple_text".snakeCase())
        assertEquals("simple_text", "simple_text!".snakeCase())

        assertEquals("simple_dto", "simple DTO".snakeCase())
        assertEquals("simple_s3_file", "simple s3 file".snakeCase())
        assertEquals("simple_s3_dto", "simple S3 DTO".snakeCase())
        assertEquals("simple_3d_dto", "simple 3d DTO".snakeCase())

        assertEquals("e2e_asset_generator", "E2eAssetGenerator".snakeCase())
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

        assertEquals("E2eAssetGenerator", "E2eAssetGenerator".camelCase())
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
