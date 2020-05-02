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
        val unpackClass = Library.read(input.resolve("unpackclass.pack"), JarLibraryReader)
        val glUnpackClass = Library(unpackClass)
        val loader = Library.read(input.resolve("loader.jar"), JarLibraryReader)
        val glLoader = Library.read(input.resolve("loader_gl.jar"), JarLibraryReader)
        val gl = Library.read(input.resolve("jaggl.pack200"), Pack200LibraryReader)
        val client = Library.read(input.resolve("runescape.jar"), JarLibraryReader)
        val glClient = Library.read(input.resolve("runescape_gl.pack200"), Pack200LibraryReader)

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

        client.write(output.resolve("runescape.jar"), JarLibraryWriter, classPath)
        loader.write(output.resolve("loader.jar"), JarLibraryWriter, classPath)
        signLink.write(output.resolve("signlink.jar"), JarLibraryWriter, classPath)
        unpack.write(output.resolve("unpack.jar"), JarLibraryWriter, classPath)
        unpackClass.write(output.resolve("unpackclass.jar"), JarLibraryWriter, classPath)

        gl.write(output.resolve("jaggl.jar"), JarLibraryWriter, glClassPath)
        glClient.write(output.resolve("runescape_gl.jar"), JarLibraryWriter, glClassPath)
        glLoader.write(output.resolve("loader_gl.jar"), JarLibraryWriter, glClassPath)
        glSignLink.write(output.resolve("signlink_gl.jar"), JarLibraryWriter, glClassPath)
        glUnpack.write(output.resolve("unpack_gl.jar"), JarLibraryWriter, glClassPath)
        glUnpackClass.write(output.resolve("unpackclass_gl.jar"), JarLibraryWriter, glClassPath)

        unsignedClient.write(output.resolve("runescape_unsigned.jar"), JarLibraryWriter, unsignedClassPath)
    }

    companion object {
        private val logger = InlineLogger()
    }
}
