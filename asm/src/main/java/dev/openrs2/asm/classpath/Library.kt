package dev.openrs2.asm.classpath

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.hasCode
import dev.openrs2.asm.remap.ClassForNameRemapper
import dev.openrs2.common.io.DeterministicJarOutputStream
import dev.openrs2.common.io.SkipOutputStream
import org.apache.harmony.pack200.Pack200
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.util.CheckClassAdapter
import java.io.ByteArrayInputStream
import java.io.SequenceInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class Library constructor() : Iterable<ClassNode> {
    private val classes = TreeMap<String, ClassNode>()

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
        val classNames = HashSet<String>()

        for (clazz in classes.values) {
            for (method in clazz.methods) {
                if (method.hasCode()) {
                    ClassForNameRemapper.remap(remapper, method)
                }
            }

            classNames.add(clazz.name)
        }

        for (name in classNames) {
            val `in` = classes.remove(name)

            val out = ClassNode()
            `in`!!.accept(ClassRemapper(out, remapper))

            classes[out.name] = out
        }
    }

    fun writeJar(path: Path) {
        logger.info { "Writing jar $path" }

        DeterministicJarOutputStream(Files.newOutputStream(path)).use { out ->
            for (clazz in classes.values) {
                val writer = ClassWriter(0)

                clazz.accept(CheckClassAdapter(writer, true))

                out.putNextEntry(JarEntry(clazz.name + CLASS_SUFFIX))
                out.write(writer.toByteArray())
            }
        }
    }

    fun writePack(path: Path) {
        logger.info { "Writing pack $path" }

        val temp = Files.createTempFile(TEMP_PREFIX, JAR_SUFFIX)
        try {
            writeJar(temp)

            JarInputStream(Files.newInputStream(temp)).use { `in` ->
                val data = Files.newOutputStream(path)
                val headerSize = GZIP_HEADER.size.toLong()

                GZIPOutputStream(SkipOutputStream(data, headerSize)).use { out ->
                    Pack200.newPacker().pack(`in`, out)
                }
            }
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    companion object {
        private val logger = InlineLogger()
        private const val CLASS_SUFFIX = ".class"
        private const val TEMP_PREFIX = "tmp"
        private const val JAR_SUFFIX = ".jar"
        private val GZIP_HEADER = byteArrayOf(0x1F, 0x8B.toByte())

        fun readJar(path: Path): Library {
            logger.info { "Reading jar $path" }

            val library = Library()

            JarInputStream(Files.newInputStream(path)).use { `in` ->
                while (true) {
                    val entry = `in`.nextJarEntry
                    if (entry == null) {
                        break
                    } else if (!entry.name.endsWith(CLASS_SUFFIX)) {
                        continue
                    }

                    val clazz = ClassNode()
                    val reader = ClassReader(`in`)
                    reader.accept(clazz, ClassReader.SKIP_DEBUG)

                    library.add(clazz)
                }
            }

            return library
        }

        fun readPack(path: Path): Library {
            logger.info { "Reading pack $path" }

            val temp = Files.createTempFile(TEMP_PREFIX, JAR_SUFFIX)
            try {
                val header = ByteArrayInputStream(GZIP_HEADER)
                val data = Files.newInputStream(path)

                GZIPInputStream(SequenceInputStream(header, data)).use { `in` ->
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
