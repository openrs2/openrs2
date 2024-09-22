package org.openrs2.asm.io

import org.glavo.pack200.Pack200
import org.objectweb.asm.tree.ClassNode
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.compress.gzip.Gzip
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.jar.JarInputStream

public object Pack200LibraryWriter : LibraryWriter {
    override fun write(output: OutputStream, classPath: ClassPath, classes: Iterable<ClassNode>) {
        ByteArrayOutputStream().use { tempOutput ->
            JarLibraryWriter.write(tempOutput, classPath, classes)

            return JarInputStream(ByteArrayInputStream(tempOutput.toByteArray())).use { jarInput ->
                Gzip.createHeaderlessOutputStream(output).use { gzipOutput ->
                    Pack200.newPacker().pack(jarInput, gzipOutput)
                }
            }
        }
    }
}
