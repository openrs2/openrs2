package dev.openrs2.asm.io

import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.compress.gzip.Gzip
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.jar.JarInputStream
import java.util.jar.Pack200

class Pack200LibraryWriter : LibraryWriter {
    override fun write(output: OutputStream, classPath: ClassPath, library: Library) {
        ByteArrayOutputStream().use { tempOutput ->
            JarLibraryWriter().write(tempOutput, classPath, library)

            return JarInputStream(ByteArrayInputStream(tempOutput.toByteArray())).use { jarInput ->
                Gzip.createHeaderlessOutputStream(output).use { gzipOutput ->
                    Pack200.newPacker().pack(jarInput, gzipOutput)
                }
            }
        }
    }
}
