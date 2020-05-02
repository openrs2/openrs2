package dev.openrs2.asm.io

import dev.openrs2.asm.classpath.Library
import java.io.InputStream

interface LibraryReader {
    fun read(input: InputStream): Library
}
