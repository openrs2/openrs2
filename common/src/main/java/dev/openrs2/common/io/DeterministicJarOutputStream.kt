package dev.openrs2.common.io

import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipEntry

class DeterministicJarOutputStream : JarOutputStream {
    private constructor(out: OutputStream) : super(out)
    private constructor(out: OutputStream, man: Manifest) : super(out, man)

    override fun putNextEntry(ze: ZipEntry) {
        ze.creationTime = UNIX_EPOCH
        ze.lastAccessTime = UNIX_EPOCH
        ze.lastModifiedTime = UNIX_EPOCH
        super.putNextEntry(ze)
    }

    companion object {
        private val UNIX_EPOCH = FileTime.fromMillis(0)

        fun create(out: OutputStream, manifest: Manifest? = null): JarOutputStream {
            return if (manifest != null) {
                DeterministicJarOutputStream(out, manifest)
            } else {
                DeterministicJarOutputStream(out)
            }
        }

        fun repack(src: Path, dest: Path) {
            JarInputStream(Files.newInputStream(src)).use { `in` ->
                create(Files.newOutputStream(dest), `in`.manifest).use { out ->
                    while (true) {
                        val entry = `in`.nextJarEntry ?: break
                        out.putNextEntry(entry)
                        `in`.copyTo(out)
                    }
                }
            }
        }
    }
}
