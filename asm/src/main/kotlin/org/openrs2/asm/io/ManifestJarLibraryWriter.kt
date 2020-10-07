package org.openrs2.asm.io

import org.openrs2.util.io.DeterministicJarOutputStream
import java.io.OutputStream
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

public class ManifestJarLibraryWriter(private val manifest: Manifest) : AbstractJarLibraryWriter() {
    override fun createJarOutputStream(output: OutputStream): JarOutputStream {
        return DeterministicJarOutputStream(output, manifest)
    }
}
