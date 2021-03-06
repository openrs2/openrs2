package org.openrs2.util.io

import java.io.OutputStream
import java.nio.file.attribute.FileTime
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.Deflater
import java.util.zip.ZipEntry

public class DeterministicJarOutputStream : JarOutputStream {
    public constructor(out: OutputStream) : super(out)
    public constructor(out: OutputStream, man: Manifest) : super(out, man)

    init {
        setLevel(Deflater.BEST_COMPRESSION)
    }

    override fun putNextEntry(ze: ZipEntry) {
        ze.creationTime = UNIX_EPOCH
        ze.lastAccessTime = UNIX_EPOCH
        ze.lastModifiedTime = UNIX_EPOCH
        super.putNextEntry(ze)
    }

    private companion object {
        private val UNIX_EPOCH = FileTime.fromMillis(0)
    }
}
