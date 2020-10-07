package org.openrs2.asm.io

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.openrs2.asm.classpath.JsrInliner
import org.openrs2.util.io.entries
import java.io.InputStream
import java.util.jar.JarInputStream

public object JarLibraryReader : LibraryReader {
    private const val CLASS_SUFFIX = ".class"

    override fun read(input: InputStream): Iterable<ClassNode> {
        val classes = mutableListOf<ClassNode>()

        JarInputStream(input).use { jar ->
            for (entry in jar.entries) {
                if (!entry.name.endsWith(CLASS_SUFFIX)) {
                    continue
                }

                val clazz = ClassNode()
                val reader = ClassReader(jar)
                reader.accept(JsrInliner(clazz), ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)

                classes += clazz
            }
        }

        return classes
    }
}
