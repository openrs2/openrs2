package dev.openrs2.deob

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
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
        val unpackClass = Library.readJar(input.resolve("unpackclass.pack"))
        val glUnpackClass = Library(unpackClass)
        val loader = Library.readJar(input.resolve("loader.jar"))
        val glLoader = Library.readJar(input.resolve("loader_gl.jar"))
        val gl = Library.readPack(input.resolve("jaggl.pack200"))
        val client = Library.readJar(input.resolve("runescape.jar"))
        val glClient = Library.readPack(input.resolve("runescape_gl.pack200"))

        // TODO(gpe): it'd be nice to have separate signlink.jar and
        // signlink-unsigned.jar files so we don't (effectively) deobfuscate
        // runescape.jar twice with different sets of names, but thinking about
        // how this would work is tricky (as the naming must match)
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

        client.writeJar(classPath, output.resolve("runescape.jar"))
        loader.writeJar(classPath, output.resolve("loader.jar"))
        signLink.writeJar(classPath, output.resolve("signlink.jar"))
        unpack.writeJar(classPath, output.resolve("unpack.jar"))
        unpackClass.writeJar(classPath, output.resolve("unpackclass.jar"))

        gl.writeJar(glClassPath, output.resolve("jaggl.jar"))
        glClient.writeJar(glClassPath, output.resolve("runescape_gl.jar"))
        glLoader.writeJar(glClassPath, output.resolve("loader_gl.jar"))
        glSignLink.writeJar(glClassPath, output.resolve("signlink_gl.jar"))
        glUnpack.writeJar(glClassPath, output.resolve("unpack_gl.jar"))
        glUnpackClass.writeJar(glClassPath, output.resolve("unpackclass_gl.jar"))

        unsignedClient.writeJar(unsignedClassPath, output.resolve("runescape_unsigned.jar"))
    }

    companion object {
        private val logger = InlineLogger()
    }
}
