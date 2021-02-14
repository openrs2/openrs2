package org.openrs2.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter

public class JsonPrettyPrinter : DefaultPrettyPrinter() {
    init {
        indentArraysWith(UNIX_INDENT)
        indentObjectsWith(UNIX_INDENT)
    }

    override fun createInstance(): DefaultPrettyPrinter {
        return JsonPrettyPrinter()
    }

    override fun writeObjectFieldValueSeparator(g: JsonGenerator) {
        g.writeRaw(": ")
    }

    private companion object {
        private val UNIX_INDENT = DefaultIndenter("  ", "\n")
    }
}
