package dev.openrs2.asm.classpath

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.NopClassVisitor
import dev.openrs2.asm.remap
import dev.openrs2.common.crypto.Pkcs12KeyStore
import dev.openrs2.common.io.DeterministicJarOutputStream
import dev.openrs2.common.io.SkipOutputStream
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.util.CheckClassAdapter
import java.io.ByteArrayInputStream
import java.io.OutputStream
import java.io.SequenceInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.TreeMap
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.jar.Pack200
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class Library constructor() : Iterable<ClassNode> {
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

    fun writeJar(path: Path, manifest: Manifest? = null) {
        logger.info { "Writing jar $path" }

        Files.newOutputStream(path).use {
            writeJar(it, manifest)
        }
    }

    fun writeJar(out: OutputStream, manifest: Manifest? = null) {
        DeterministicJarOutputStream.create(out, manifest).use { jar ->
            for (clazz in classes.values) {
                val writer = ClassWriter(0)

                clazz.accept(writer)

                jar.putNextEntry(JarEntry(clazz.name + CLASS_SUFFIX))
                jar.write(writer.toByteArray())

                /*
                 * XXX(gpe): CheckClassAdapter breaks the Label offset
                 * calculation in the OriginalPcTable's write method, so we do
                 * a second pass without any attributes to check the class,
                 * feeding the callbacks into a no-op visitor.
                 */
                for (method in clazz.methods) {
                    method.attrs?.clear()
                }
                clazz.accept(CheckClassAdapter(NopClassVisitor, true))
            }
        }
    }

    fun writeSignedJar(path: Path, keyStore: Pkcs12KeyStore, manifest: Manifest? = null) {
        logger.info { "Writing signed jar $path" }

        val unsignedPath = Files.createTempFile("tmp", ".jar")
        try {
            writeJar(unsignedPath, manifest)
            keyStore.signJar(unsignedPath)
            DeterministicJarOutputStream.repack(unsignedPath, path)
        } finally {
            Files.deleteIfExists(unsignedPath)
        }
    }

    fun writePack(out: OutputStream) {
        val temp = Files.createTempFile(TEMP_PREFIX, JAR_SUFFIX)
        try {
            writeJar(temp)

            JarInputStream(Files.newInputStream(temp)).use { `in` ->
                val headerSize = GZIP_HEADER.size.toLong()

                GZIPOutputStream(SkipOutputStream(out, headerSize)).use { gzip ->
                    Pack200.newPacker().pack(`in`, gzip)
                }
            }
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    fun writeJs5(out: OutputStream) {
        // TODO(gpe): implement
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
                    val entry = `in`.nextJarEntry ?: break
                    if (!entry.name.endsWith(CLASS_SUFFIX)) {
                        continue
                    }

                    val clazz = ClassNode()
                    val reader = ClassReader(`in`)
                    reader.accept(JsrInliner(clazz), ClassReader.SKIP_DEBUG)

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
