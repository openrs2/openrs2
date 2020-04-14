package dev.openrs2.asm.classpath

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.remap
import dev.openrs2.compress.gzip.Gzip
import org.objectweb.asm.ClassReader
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.ClassNode
import java.nio.file.Files
import java.nio.file.Path
import java.util.TreeMap
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import java.util.jar.Pack200

class Library() : Iterable<ClassNode> {
    private var classes = TreeMap<String, ClassNode>()

    constructor(library: Library) : this() {
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

    fun remap(remapper: Remapper) {
        for (clazz in classes.values) {
            clazz.remap(remapper)
        }

        classes = classes.mapKeysTo(TreeMap()) { (_, clazz) -> clazz.name }
    }

    companion object {
        private val logger = InlineLogger()
        private const val CLASS_SUFFIX = ".class"
        private const val TEMP_PREFIX = "tmp"
        private const val JAR_SUFFIX = ".jar"

        fun readJar(path: Path): Library {
            logger.info { "Reading jar $path" }

            val library = Library()

            JarInputStream(Files.newInputStream(path)).use { `in` ->
                while (true) {
                    val entry = `in`.nextJarEntry ?: break
                    if (!entry.name.endsWith(CLASS_SUFFIX)) {
                        continue
                    }

                    val clazz = ClassNode()
                    val reader = ClassReader(`in`)
                    reader.accept(JsrInliner(clazz), ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)

                    library.add(clazz)
                }
            }

            return library
        }

        fun readPack(path: Path): Library {
            logger.info { "Reading pack $path" }

            val temp = Files.createTempFile(TEMP_PREFIX, JAR_SUFFIX)
            try {
                Gzip.createHeaderlessInputStream(Files.newInputStream(path)).use { `in` ->
                    JarOutputStream(Files.newOutputStream(temp)).use { out ->
                        Pack200.newUnpacker().unpack(`in`, out)
                    }
                }

                return readJar(temp)
            } finally {
                Files.deleteIfExists(temp)
            }
        }
    }
}
