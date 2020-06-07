package dev.openrs2.asm.io

import org.objectweb.asm.tree.ClassNode
import java.io.InputStream

object PackClassLibraryReader : LibraryReader {
    override fun read(input: InputStream): Iterable<ClassNode> {
        // TODO(gpe): implement
        return emptyList()
    }
}
