package org.openrs2.asm.io

import org.objectweb.asm.tree.ClassNode
import org.openrs2.asm.classpath.ClassPath
import java.io.OutputStream

public interface LibraryWriter {
    public fun write(output: OutputStream, classPath: ClassPath, classes: Iterable<ClassNode>)
}
