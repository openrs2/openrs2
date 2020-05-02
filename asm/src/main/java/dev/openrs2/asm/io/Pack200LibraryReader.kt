package dev.openrs2.asm.io

import dev.openrs2.asm.classpath.Library
import dev.openrs2.compress.gzip.Gzip
import java.io.InputStream
import java.nio.file.Files
import java.util.jar.JarOutputStream
import java.util.jar.Pack200

class Pack200LibraryReader : LibraryReader {
    override fun read(input: InputStream): Library {
        val temp = Files.createTempFile(TEMP_PREFIX, JAR_SUFFIX)
        try {
            Gzip.createHeaderlessInputStream(input).use { gzipInput ->
                JarOutputStream(Files.newOutputStream(temp)).use { output ->
                    Pack200.newUnpacker().unpack(gzipInput, output)
                }
            }

            return Files.newInputStream(temp).use { tempInput ->
                JarLibraryReader().read(tempInput)
            }
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    private companion object {
        private const val TEMP_PREFIX = "tmp"
        private const val JAR_SUFFIX = ".jar"
    }
}
