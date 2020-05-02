package dev.openrs2.asm.io

import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.compress.gzip.Gzip
import java.io.OutputStream
import java.nio.file.Files
import java.util.jar.JarInputStream
import java.util.jar.Pack200

class Pack200LibraryWriter : LibraryWriter {
    override fun write(output: OutputStream, classPath: ClassPath, library: Library) {
        val tempJar = Files.createTempFile(TEMP_PREFIX, JAR_SUFFIX)
        try {
            Files.newOutputStream(tempJar).use { tempOutput ->
                JarLibraryWriter().write(tempOutput, classPath, library)
            }

            JarInputStream(Files.newInputStream(tempJar)).use { input ->
                Gzip.createHeaderlessOutputStream(output).use { gzip ->
                    Pack200.newPacker().pack(input, gzip)
                }
            }
        } finally {
            Files.deleteIfExists(tempJar)
        }
    }

    private companion object {
        private const val TEMP_PREFIX = "tmp"
        private const val JAR_SUFFIX = ".jar"
    }
}
