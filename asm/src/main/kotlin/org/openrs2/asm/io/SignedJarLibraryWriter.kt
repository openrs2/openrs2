package org.openrs2.asm.io

import org.objectweb.asm.tree.ClassNode
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.crypto.Pkcs12KeyStore
import org.openrs2.util.io.DeterministicJarOutputStream
import org.openrs2.util.io.entries
import org.openrs2.util.io.useTempFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.util.jar.JarInputStream
import java.util.jar.Manifest

public class SignedJarLibraryWriter(
    private val manifest: Manifest,
    private val keyStore: Pkcs12KeyStore
) : LibraryWriter {
    override fun write(output: OutputStream, classPath: ClassPath, classes: Iterable<ClassNode>) {
        useTempFile(TEMP_PREFIX, JAR_SUFFIX) { unsignedJar ->
            Files.newOutputStream(unsignedJar).use { unsignedOutput ->
                ManifestJarLibraryWriter(manifest).write(unsignedOutput, classPath, classes)
            }

            ByteArrayOutputStream().use { signedOutput ->
                keyStore.signJar(unsignedJar, signedOutput)

                return ByteArrayInputStream(signedOutput.toByteArray()).use { signedInput ->
                    repack(signedInput, output)
                }
            }
        }
    }

    private fun repack(input: InputStream, output: OutputStream) {
        JarInputStream(input).use { jarInput ->
            DeterministicJarOutputStream(output, jarInput.manifest).use { jarOutput ->
                for (entry in jarInput.entries) {
                    jarOutput.putNextEntry(entry)
                    jarInput.copyTo(jarOutput)
                }
            }
        }
    }

    private companion object {
        private const val TEMP_PREFIX = "tmp"
        private const val JAR_SUFFIX = ".jar"
    }
}
