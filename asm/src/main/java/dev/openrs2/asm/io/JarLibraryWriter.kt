package dev.openrs2.asm.io

import dev.openrs2.asm.ClassVersionUtils
import dev.openrs2.asm.NopClassVisitor
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.classpath.StackFrameClassWriter
import dev.openrs2.util.io.DeterministicJarOutputStream
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.util.CheckClassAdapter
import java.io.OutputStream
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

open class JarLibraryWriter : LibraryWriter {
    override fun write(output: OutputStream, classPath: ClassPath, library: Library) {
        createJarOutputStream(output).use { jar ->
            for (clazz in library) {
                val writer = if (ClassVersionUtils.gte(clazz.version, Opcodes.V1_7)) {
                    StackFrameClassWriter(classPath)
                } else {
                    ClassWriter(ClassWriter.COMPUTE_MAXS)
                }

                clazz.accept(writer)

                jar.putNextEntry(JarEntry(clazz.name + CLASS_SUFFIX))
                jar.write(writer.toByteArray())

                /*
                 * XXX(gpe): CheckClassAdapter breaks the Label offset
                 * calculation in the OriginalPcTable's write method, so we do
                 * a second pass without any attributes to check the class,
                 * feeding the callbacks into a no-op visitor.
                 */
                for (method in clazz.methods) {
                    method.attrs?.clear()
                }
                clazz.accept(CheckClassAdapter(NopClassVisitor, true))
            }
        }
    }

    protected open fun createJarOutputStream(output: OutputStream): JarOutputStream {
        return DeterministicJarOutputStream(output)
    }

    private companion object {
        private const val CLASS_SUFFIX = ".class"
    }
}
