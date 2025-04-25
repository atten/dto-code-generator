package org.codegen.parser

import org.codegen.schema.Document

abstract class AbstractParser {
    abstract fun parse(content: String): Document
}
