package dev.openrs2.asm.io

import dev.openrs2.compress.gzip.Gzip
import org.objectweb.asm.tree.ClassNode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.jar.JarOutputStream
import java.util.jar.Pack200

public object Pack200LibraryReader : LibraryReader {
    override fun read(input: InputStream): Iterable<ClassNode> {
        ByteArrayOutputStream().use { tempOutput ->
            Gzip.createHeaderlessInputStream(input).use { gzipInput ->
                JarOutputStream(tempOutput).use { jarOutput ->
                    Pack200.newUnpacker().unpack(gzipInput, jarOutput)
                }
            }

            return ByteArrayInputStream(tempOutput.toByteArray()).use { tempInput ->
                JarLibraryReader.read(tempInput)
            }
        }
    }
}
