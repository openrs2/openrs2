package dev.openrs2.asm.io

import dev.openrs2.asm.classpath.JsrInliner
import dev.openrs2.util.io.entries
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import java.io.InputStream
import java.util.jar.JarInputStream

object JarLibraryReader : LibraryReader {
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
