package dev.openrs2.asm.io

import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import java.io.OutputStream

class Js5LibraryWriter(private val output: OutputStream) : LibraryWriter {
    override fun write(classPath: ClassPath, library: Library) {
        // TODO(gpe): implement
    }
}
