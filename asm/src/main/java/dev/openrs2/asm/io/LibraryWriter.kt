package dev.openrs2.asm.io

import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library

interface LibraryWriter {
    fun write(classPath: ClassPath, library: Library)
}
