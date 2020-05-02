package dev.openrs2.deob

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.io.JarLibraryReader
import dev.openrs2.asm.io.JarLibraryWriter
import dev.openrs2.asm.io.Pack200LibraryReader
import dev.openrs2.asm.transform.Transformer
import dev.openrs2.deob.remap.PrefixRemapper
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Deobfuscator @Inject constructor(
    @DeobfuscatorQualifier private val transformers: Set<@JvmSuppressWildcards Transformer>
) {
    fun run(input: Path, output: Path) {
        // read input jars/packs
        logger.info { "Reading input jars" }
        val unpackClass = readJar(input.resolve("unpackclass.pack"))
        val glUnpackClass = Library(unpackClass)
        val loader = readJar(input.resolve("loader.jar"))
        val glLoader = readJar(input.resolve("loader_gl.jar"))
        val gl = readPack(input.resolve("jaggl.pack200"))
        val client = readJar(input.resolve("runescape.jar"))
        val glClient = readPack(input.resolve("runescape_gl.pack200"))

        /*
         * TODO(gpe): it'd be nice to have separate signlink.jar and
         * signlink-unsigned.jar files so we don't (effectively) deobfuscate
         * runescape.jar twice with different sets of names, but thinking about
         * how this would work is tricky (as the naming must match)
         */
        val unsignedClient = Library(client)

        // overwrite client's classes with signed classes from the loader
        logger.info { "Moving signed classes from loader" }
        val signLink = Library()
        SignedClassUtils.move(loader, client, signLink)

        logger.info { "Moving signed classes from loader_gl" }
        val glSignLink = Library()
        SignedClassUtils.move(glLoader, glClient, glSignLink)

        // move unpack class out of the loader (so the unpacker and loader can both depend on it)
        logger.info { "Moving unpack from loader to unpack" }
        val unpack = Library()
        unpack.add(loader.remove("unpack")!!)

        logger.info { "Moving unpack from loader_gl to unpack_gl" }
        val glUnpack = Library()
        glUnpack.add(glLoader.remove("unpack")!!)

        // prefix remaining loader/unpacker classes (to avoid conflicts when we rename in the same classpath as the client)
        logger.info { "Prefixing loader and unpackclass class names" }
        loader.remap(PrefixRemapper.create(loader, "loader_"))
        glLoader.remap(PrefixRemapper.create(glLoader, "loader_"))
        unpackClass.remap(PrefixRemapper.create(unpackClass, "unpackclass_"))
        glUnpackClass.remap(PrefixRemapper.create(glUnpackClass, "unpackclass_"))

        // bundle libraries together into a common classpath
        val runtime = ClassLoader.getPlatformClassLoader()
        val classPath = ClassPath(
            runtime,
            dependencies = emptyList(),
            libraries = listOf(client, loader, signLink, unpack, unpackClass)
        )
        val glClassPath = ClassPath(
            runtime,
            dependencies = listOf(gl),
            libraries = listOf(glClient, glLoader, glSignLink, glUnpack, glUnpackClass)
        )
        val unsignedClassPath = ClassPath(
            runtime,
            dependencies = emptyList(),
            libraries = listOf(unsignedClient)
        )

        // deobfuscate
        logger.info { "Transforming client" }
        for (transformer in transformers) {
            logger.info { "Running transformer ${transformer.javaClass.simpleName}" }
            transformer.transform(classPath)
        }

        logger.info { "Transforming client_gl" }
        for (transformer in transformers) {
            logger.info { "Running transformer ${transformer.javaClass.simpleName}" }
            transformer.transform(glClassPath)
        }

        logger.info { "Transforming client_unsigned" }
        for (transformer in transformers) {
            logger.info { "Running transformer ${transformer.javaClass.simpleName}" }
            transformer.transform(unsignedClassPath)
        }

        // write output jars
        logger.info { "Writing output jars" }

        Files.createDirectories(output)

        writeJar(classPath, client, output.resolve("runescape.jar"))
        writeJar(classPath, loader, output.resolve("loader.jar"))
        writeJar(classPath, signLink, output.resolve("signlink.jar"))
        writeJar(classPath, unpack, output.resolve("unpack.jar"))
        writeJar(classPath, unpackClass, output.resolve("unpackclass.jar"))

        writeJar(glClassPath, gl, output.resolve("jaggl.jar"))
        writeJar(glClassPath, glClient, output.resolve("runescape_gl.jar"))
        writeJar(glClassPath, glLoader, output.resolve("loader_gl.jar"))
        writeJar(glClassPath, glSignLink, output.resolve("signlink_gl.jar"))
        writeJar(glClassPath, glUnpack, output.resolve("unpack_gl.jar"))
        writeJar(glClassPath, glUnpackClass, output.resolve("unpackclass_gl.jar"))

        writeJar(unsignedClassPath, unsignedClient, output.resolve("runescape_unsigned.jar"))
    }

    private fun readJar(path: Path): Library {
        logger.info { "Reading jar $path" }

        return Files.newInputStream(path).use { input ->
            JarLibraryReader.read(input)
        }
    }

    private fun readPack(path: Path): Library {
        logger.info { "Reading pack $path" }

        return Files.newInputStream(path).use { input ->
            Pack200LibraryReader.read(input)
        }
    }

    private fun writeJar(classPath: ClassPath, library: Library, path: Path) {
        logger.info { "Writing jar $path" }

        Files.newOutputStream(path).use { output ->
            JarLibraryWriter.write(output, classPath, library)
        }
    }

    companion object {
        private val logger = InlineLogger()
    }
}
