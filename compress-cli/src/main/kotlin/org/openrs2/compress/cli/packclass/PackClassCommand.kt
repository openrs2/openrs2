package org.openrs2.compress.cli.packclass

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.defaultStdin
import com.github.ajalt.clikt.parameters.types.defaultStdout
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.outputStream
import io.netty.buffer.ByteBufAllocator
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.io.JarLibraryReader
import org.openrs2.asm.io.PackClassLibraryWriter

public class PackClassCommand : CliktCommand(name = "packclass") {
    private val input by option().inputStream().defaultStdin()
    private val output by option().outputStream().defaultStdout()

    override fun run() {
        val classes = JarLibraryReader.read(input)
        val classPath = ClassPath(ClassLoader.getPlatformClassLoader(), emptyList(), emptyList())
        PackClassLibraryWriter(ByteBufAllocator.DEFAULT).write(output, classPath, classes)
    }
}
