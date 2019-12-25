package dev.openrs2.common.io

import java.io.OutputStream
import java.nio.file.attribute.FileTime
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipEntry

class DeterministicJarOutputStream : JarOutputStream {
    constructor(out: OutputStream) : super(out)
    constructor(out: OutputStream, man: Manifest) : super(out, man)

    override fun putNextEntry(ze: ZipEntry) {
        ze.creationTime = UNIX_EPOCH
        ze.lastAccessTime = UNIX_EPOCH
        ze.lastModifiedTime = UNIX_EPOCH
        super.putNextEntry(ze)
    }

    companion object {
        private val UNIX_EPOCH = FileTime.fromMillis(0)
    }
}
