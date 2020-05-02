package dev.openrs2.asm.io

import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import java.io.OutputStream

interface LibraryWriter {
    fun write(output: OutputStream, classPath: ClassPath, library: Library)
}
