package org.openrs2.compress.cli.packclass

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.defaultStdin
import com.github.ajalt.clikt.parameters.types.defaultStdout
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.outputStream
import io.netty.buffer.ByteBufAllocator
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.io.JarLibraryWriter
import org.openrs2.asm.io.PackClassLibraryReader

public class UnpackClassCommand : CliktCommand(name = "unpackclass") {
    private val input by option().inputStream().defaultStdin()
    private val output by option().outputStream().defaultStdout()

    override fun run() {
        val classes = PackClassLibraryReader(ByteBufAllocator.DEFAULT).read(input)
        val classPath = ClassPath(ClassLoader.getPlatformClassLoader(), emptyList(), emptyList())
        JarLibraryWriter.write(output, classPath, classes)
    }
}
