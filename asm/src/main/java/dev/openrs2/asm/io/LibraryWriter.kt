package dev.openrs2.asm.io

import dev.openrs2.asm.classpath.ClassPath
import org.objectweb.asm.tree.ClassNode
import java.io.OutputStream

interface LibraryWriter {
    fun write(output: OutputStream, classPath: ClassPath, classes: Iterable<ClassNode>)
}
