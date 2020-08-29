package dev.openrs2.asm.io

import dev.openrs2.asm.ClassVersionUtils
import dev.openrs2.asm.NopClassVisitor
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.StackFrameClassWriter
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.util.CheckClassAdapter
import java.io.OutputStream
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

public abstract class AbstractJarLibraryWriter : LibraryWriter {
    override fun write(output: OutputStream, classPath: ClassPath, classes: Iterable<ClassNode>) {
        createJarOutputStream(output).use { jar ->
            for (clazz in classes) {
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

    protected abstract fun createJarOutputStream(output: OutputStream): JarOutputStream

    private companion object {
        private const val CLASS_SUFFIX = ".class"
    }
}
