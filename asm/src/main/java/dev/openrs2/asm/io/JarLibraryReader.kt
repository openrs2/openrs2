package dev.openrs2.asm.io

import dev.openrs2.asm.classpath.JsrInliner
import dev.openrs2.asm.classpath.Library
import dev.openrs2.util.io.entries
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import java.io.InputStream
import java.util.jar.JarInputStream

class JarLibraryReader : LibraryReader {
    override fun read(input: InputStream): Library {
        val library = Library()

        JarInputStream(input).use { jar ->
            for (entry in jar.entries) {
                if (!entry.name.endsWith(CLASS_SUFFIX)) {
                    continue
                }

                val clazz = ClassNode()
                val reader = ClassReader(jar)
                reader.accept(JsrInliner(clazz), ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)

                library.add(clazz)
            }
        }

        return library
    }

    private companion object {
        private const val CLASS_SUFFIX = ".class"
    }
}
