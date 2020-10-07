package org.openrs2.asm.io

import org.objectweb.asm.tree.ClassNode
import java.io.InputStream

public interface LibraryReader {
    public fun read(input: InputStream): Iterable<ClassNode>
}
