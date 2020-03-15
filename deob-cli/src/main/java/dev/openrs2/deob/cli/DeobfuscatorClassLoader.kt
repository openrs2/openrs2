package dev.openrs2.deob.cli

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.InputStream
import java.lang.IllegalArgumentException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private fun load(input: InputStream) = input.use {
    val node = ClassNode()
    val reader = ClassReader(input)

    reader.accept(node, ClassReader.SKIP_DEBUG)

    node
}

interface DeobfuscatorClassLoader {
    fun load(name: String): ClassNode
}

object SystemClassLoader : DeobfuscatorClassLoader {
    override fun load(name: String): ClassNode {
        val classPath = "/${name.replace('.', File.separatorChar)}.class"
        val classFile = this.javaClass.getResourceAsStream(classPath)

        return load(classFile)
    }
}

class ClasspathClassLoader(val classPath: List<Path>) : DeobfuscatorClassLoader {
    override fun load(name: String): ClassNode {
        val relativePath = Paths.get("${name.replace('.', File.separatorChar)}.class")

        for (entry in classPath) {
            val classFilePath = entry.resolve(relativePath)
            if (!Files.exists(classFilePath)) {
                continue
            }

            return load(Files.newInputStream(classFilePath))
        }

        throw IllegalArgumentException("Unable to find class named $name")
    }
}
