package dev.openrs2.asm.io

import dev.openrs2.asm.classpath.Library

interface LibraryReader {
    fun read(): Library
}
