package dev.openrs2.asm.io

import org.objectweb.asm.tree.ClassNode
import java.io.InputStream

interface LibraryReader {
    fun read(input: InputStream): Iterable<ClassNode>
}
