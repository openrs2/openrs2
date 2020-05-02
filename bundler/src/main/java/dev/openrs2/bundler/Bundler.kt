package dev.openrs2.bundler

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.io.JarLibraryReader
import dev.openrs2.asm.io.ManifestJarLibraryWriter
import dev.openrs2.asm.io.Pack200LibraryReader
import dev.openrs2.asm.io.SignedJarLibraryWriter
import dev.openrs2.asm.transform.Transformer
import dev.openrs2.bundler.transform.ResourceTransformer
import dev.openrs2.conf.Config
import dev.openrs2.crypto.Pkcs12KeyStore
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.jar.Attributes.Name.MANIFEST_VERSION
import java.util.jar.Manifest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Bundler @Inject constructor(
    @BundlerQualifier private val transformers: Set<@JvmSuppressWildcards Transformer>,
    private val config: Config
) {
    private val unsignedManifest = Manifest().apply {
        mainAttributes[MANIFEST_VERSION] = "1.0"
        mainAttributes[APPLICATION_NAME] = config.game
        mainAttributes[PERMISSIONS] = "sandbox"
    }

    private val signedManifest = Manifest().apply {
        mainAttributes[MANIFEST_VERSION] = "1.0"
        mainAttributes[APPLICATION_NAME] = config.game
        mainAttributes[PERMISSIONS] = "all-permissions"
    }

    fun run(input: Path, output: Path, keyStorePath: Path) {
        // read input jars/packs
        logger.info { "Reading input jars" }
        val unpacker = readJar(input.resolve("unpackclass.pack"))
        val loader = readJar(input.resolve("loader.jar"))
        val glLoader = readJar(input.resolve("loader_gl.jar"))
        val gl = readPack(input.resolve("jaggl.pack200"))
        val client = readJar(input.resolve("runescape.jar"))
        val glClient = readPack(input.resolve("runescape_gl.pack200"))

        // bundle libraries together into a common classpath
        val runtime = ClassLoader.getPlatformClassLoader()
        val classPath = ClassPath(
            runtime,
            dependencies = listOf(unpacker),
            libraries = listOf(client, loader)
        )
        val glClassPath = ClassPath(
            runtime,
            dependencies = listOf(gl, unpacker),
            libraries = listOf(glClient, glLoader)
        )

        // run simple transformers
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

        // compress resources
        logger.info { "Compressing resources" }

        val unpackerJar = Resource.compressJar("unpackclass.pack", "game_unpacker.dat", classPath, unpacker)
        val clientPack = Resource.compressPack("runescape.pack200", "main_file_cache.dat0", classPath, client)
        val clientJs5 = Resource.compressJs5("runescape.js5", "main_file_cache.dat1", classPath, client)
        val glClientPack = Resource.compressPack("runescape_gl.pack200", "main_file_cache.dat3", glClassPath, glClient)
        val glClientJs5 = Resource.compressJs5("runescape_gl.js5", "main_file_cache.dat4", glClassPath, glClient)
        val glPack = Resource.compressPack("jaggl.pack200", "main_file_cache.dat5", glClassPath, gl)
        val glJs5 = Resource.compressJs5("jaggl.js5", "main_file_cache.dat6", glClassPath, gl)

        val glNatives = Resource.compressGlNatives()
        val miscNatives = Resource.compressMiscNatives()

        // update checksums in the loader
        logger.info { "Updating checksums" }

        val resourceTransformer = ResourceTransformer(
            resources = listOf(unpackerJar, clientPack, clientJs5),
            glResources = glNatives,
            miscResources = miscNatives
        )
        resourceTransformer.transform(classPath)

        val glResourceTransformer = ResourceTransformer(
            resources = listOf(unpackerJar, glClientPack, glClientJs5, glPack, glJs5),
            glResources = glNatives,
            miscResources = miscNatives
        )
        glResourceTransformer.transform(glClassPath)

        // write all resources to disk
        logger.info { "Writing resources" }

        Files.createDirectories(output)

        val resources = listOf(
            unpackerJar,
            clientPack,
            clientJs5,
            glClientPack,
            glClientJs5,
            glPack,
            glJs5
        ) + glNatives.flatten() + miscNatives
        for (resource in resources) {
            resource.write(output)
        }

        // write unsigned client and loaders
        writeJar(classPath, client, output.resolve("runescape.jar"))

        val keyStore = Pkcs12KeyStore.open(keyStorePath, config.game)
        writeSignedJar(classPath, loader, output.resolve("loader.jar"), keyStore)
        writeSignedJar(glClassPath, glLoader, output.resolve("loader_gl.jar"), keyStore)
    }

    private fun readJar(path: Path): Library {
        logger.info { "Reading jar $path" }

        return Files.newInputStream(path).use { input ->
            JarLibraryReader().read(input)
        }
    }

    private fun readPack(path: Path): Library {
        logger.info { "Reading pack $path" }

        return Files.newInputStream(path).use { input ->
            Pack200LibraryReader().read(input)
        }
    }

    private fun writeJar(classPath: ClassPath, library: Library, path: Path) {
        logger.info { "Writing jar $path" }

        Files.newOutputStream(path).use { output ->
            ManifestJarLibraryWriter(unsignedManifest).write(output, classPath, library)
        }
    }

    private fun writeSignedJar(classPath: ClassPath, library: Library, path: Path, keyStore: Pkcs12KeyStore) {
        logger.info { "Writing signed jar $path" }

        Files.newOutputStream(path).use { output ->
            SignedJarLibraryWriter(signedManifest, keyStore).write(output, classPath, library)
        }
    }

    companion object {
        private val logger = InlineLogger()

        private val APPLICATION_NAME = Attributes.Name("Application-Name")
        private val PERMISSIONS = Attributes.Name("Permissions")
    }
}
