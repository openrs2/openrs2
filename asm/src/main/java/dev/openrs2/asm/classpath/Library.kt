package dev.openrs2.asm.classpath

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.io.LibraryReader
import dev.openrs2.asm.io.LibraryWriter
import dev.openrs2.util.io.useAtomicOutputStream
import org.objectweb.asm.tree.ClassNode
import java.nio.file.Files
import java.nio.file.Path
import java.util.SortedMap
import java.util.TreeMap

class Library(val name: String) : Iterable<ClassNode> {
    private var classes: SortedMap<String, ClassNode> = TreeMap()

    constructor(name: String, library: Library) : this(name) {
        for (clazz in library.classes.values) {
            val copy = ClassNode()
            clazz.accept(copy)
            add(copy)
        }
    }

    operator fun contains(name: String): Boolean {
        return classes.containsKey(name)
    }

    operator fun get(name: String): ClassNode? {
        return classes[name]
    }

    fun add(clazz: ClassNode): ClassNode? {
        return classes.put(clazz.name, clazz)
    }

    fun remove(name: String): ClassNode? {
        return classes.remove(name)
    }

    override fun iterator(): Iterator<ClassNode> {
        return classes.values.iterator()
    }

    fun remap(remapper: ExtendedRemapper) {
        classes = LibraryRemapper(remapper, classes).remap()
    }

    fun write(path: Path, writer: LibraryWriter, classPath: ClassPath) {
        logger.info { "Writing library $path" }

        path.useAtomicOutputStream { output ->
            writer.write(output, classPath, classes.values)
        }
    }

    companion object {
        private val logger = InlineLogger()

        fun read(name: String, path: Path, reader: LibraryReader): Library {
            logger.info { "Reading library $path" }

            val classes = Files.newInputStream(path).use { input ->
                reader.read(input)
            }

            val library = Library(name)
            for (clazz in classes) {
                library.add(clazz)
            }
            return library
        }
    }
}
