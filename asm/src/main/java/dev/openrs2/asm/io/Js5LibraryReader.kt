package dev.openrs2.asm.io

import dev.openrs2.asm.classpath.Library
import java.io.InputStream

object Js5LibraryReader : LibraryReader {
    override fun read(input: InputStream): Library {
        // TODO(gpe): implement
        return Library()
    }
}
