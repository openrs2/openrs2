package dev.openrs2.asm.classpath

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.ClassVersionUtils
import dev.openrs2.asm.NopClassVisitor
import dev.openrs2.asm.remap
import dev.openrs2.compress.gzip.Gzip
import dev.openrs2.crypto.Pkcs12KeyStore
import dev.openrs2.util.io.DeterministicJarOutputStream
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.util.CheckClassAdapter
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.TreeMap
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
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

    fun writeJar(classPath: ClassPath, path: Path, manifest: Manifest? = null) {
        logger.info { "Writing jar $path" }

        Files.newOutputStream(path).use {
            writeJar(classPath, it, manifest)
        }
    }

    fun writeJar(classPath: ClassPath, out: OutputStream, manifest: Manifest? = null) {
        DeterministicJarOutputStream.create(out, manifest).use { jar ->
            for (clazz in classes.values) {
                val writer = if (ClassVersionUtils.gte(clazz.version, Opcodes.V1_7)) {
                    StackFrameClassWriter(classPath)
                } else {
                    ClassWriter(ClassWriter.COMPUTE_MAXS)
                }

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

    fun writeSignedJar(classPath: ClassPath, path: Path, keyStore: Pkcs12KeyStore, manifest: Manifest? = null) {
        logger.info { "Writing signed jar $path" }

        val unsignedPath = Files.createTempFile(TEMP_PREFIX, JAR_SUFFIX)
        try {
            writeJar(classPath, unsignedPath, manifest)

            val signedPath = Files.createTempFile(TEMP_PREFIX, JAR_SUFFIX)
            try {
                keyStore.signJar(unsignedPath, signedPath)
                DeterministicJarOutputStream.repack(signedPath, path)
            } finally {
                Files.deleteIfExists(signedPath)
            }
        } finally {
            Files.deleteIfExists(unsignedPath)
        }
    }

    fun writePack(classPath: ClassPath, out: OutputStream) {
        val temp = Files.createTempFile(TEMP_PREFIX, JAR_SUFFIX)
        try {
            writeJar(classPath, temp)

            JarInputStream(Files.newInputStream(temp)).use { `in` ->
                Gzip.createHeaderlessOutputStream(out).use { gzip ->
                    Pack200.newPacker().pack(`in`, gzip)
                }
            }
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    fun writeJs5(classPath: ClassPath, out: OutputStream) {
        // TODO(gpe): implement
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
