package dev.openrs2.bundler

import com.github.michaelbull.logging.InlineLogger
import com.google.inject.Guice
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.bundler.transform.BufferSizeTransformer
import dev.openrs2.bundler.transform.CachePathTransformer
import dev.openrs2.bundler.transform.HostCheckTransformer
import dev.openrs2.bundler.transform.LoadLibraryTransformer
import dev.openrs2.bundler.transform.MacResizeTransformer
import dev.openrs2.bundler.transform.PlatformDetectionTransformer
import dev.openrs2.bundler.transform.PublicKeyTransformer
import dev.openrs2.bundler.transform.ResourceTransformer
import dev.openrs2.bundler.transform.RightClickTransformer
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.Attributes
import java.util.jar.Attributes.Name.MANIFEST_VERSION
import java.util.jar.Manifest
import javax.inject.Inject
import javax.inject.Singleton

fun main() {
    val injector = Guice.createInjector(BundlerModule())
    val bundler = injector.getInstance(Bundler::class.java)
    bundler.run(Paths.get("nonfree/code"), Paths.get("nonfree/code/bundle"))
}

@Singleton
class Bundler @Inject constructor(publicKeyTransformer: PublicKeyTransformer) {
    private val transformers = listOf(*TRANSFORMERS.toTypedArray(), publicKeyTransformer)

    fun run(input: Path, output: Path) {
        // read input jars/packs
        logger.info { "Reading input jars" }
        val unpacker = Library.readJar(input.resolve("game_unpacker.dat"))
        val loader = Library.readJar(input.resolve("loader.jar"))
        val glLoader = Library.readJar(input.resolve("loader_gl.jar"))
        val gl = Library.readPack(input.resolve("jaggl.pack200"))
        val client = Library.readJar(input.resolve("runescape.jar"))
        val glClient = Library.readPack(input.resolve("runescape_gl.pack200"))

        // bundle libraries together into a common classpath
        val runtime = ClassLoader.getSystemClassLoader()
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
            logger.info { "Running transformer ${transformer.javaClass.simpleName} " }
            transformer.transform(classPath)
        }

        logger.info { "Transforming client_gl" }
        for (transformer in transformers) {
            logger.info { "Running transformer ${transformer.javaClass.simpleName} " }
            transformer.transform(glClassPath)
        }

        // compress resources
        logger.info { "Compressing resources" }

        val unpackerJar = Resource.compressJar("unpackclass.pack", "game_unpacker.dat", unpacker)
        val clientPack = Resource.compressPack("runescape.pack200", "main_file_cache.dat0", client)
        val clientJs5 = Resource.compressJs5("runescape.js5", "main_file_cache.dat1", client)
        val glClientPack = Resource.compressPack("runescape_gl.pack200", "main_file_cache.dat3", glClient)
        val glClientJs5 = Resource.compressJs5("runescape_gl.js5", "main_file_cache.dat4", glClient)
        val glPack = Resource.compressPack("jaggl.pack200", "main_file_cache.dat5", gl)
        val glJs5 = Resource.compressJs5("jaggl.js5", "main_file_cache.dat6", gl)

        val glNatives = Resource.compressGlResources()
        val miscNatives = Resource.compressMiscResources()

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

        val resources = listOf(
            unpackerJar,
            clientPack,
            clientJs5,
            glClientPack,
            glClientJs5,
            glPack,
            glJs5,
            *glNatives.flatten().toTypedArray(),
            *miscNatives.toTypedArray()
        )
        resources.forEach { it.write(output) }

        // write unsigned client and loaders
        client.writeJar(output.resolve("runescape.jar"), unsignedManifest)
        loader.writeJar(output.resolve("loader.jar"), signedManifest)
        glLoader.writeJar(output.resolve("loader_gl.jar"), signedManifest)

        // sign loaders
        logger.info { "Signing loaders" }
        // TODO(gpe): implement
    }

    companion object {
        private val logger = InlineLogger()
        val TRANSFORMERS = listOf(
            BufferSizeTransformer(),
            CachePathTransformer(),
            HostCheckTransformer(),
            MacResizeTransformer(),
            RightClickTransformer(),
            LoadLibraryTransformer(),
            PlatformDetectionTransformer()
        )

        private val unsignedManifest = Manifest()
        private val signedManifest = Manifest()
        private val APPLICATION_NAME = Attributes.Name("Application-Name")
        private val PERMISSIONS = Attributes.Name("Permissions")

        init {
            unsignedManifest.mainAttributes[MANIFEST_VERSION] = "1.0"
            unsignedManifest.mainAttributes[APPLICATION_NAME] = "OpenRS2"
            unsignedManifest.mainAttributes[PERMISSIONS] = "sandbox"

            signedManifest.mainAttributes[MANIFEST_VERSION] = "1.0"
            signedManifest.mainAttributes[APPLICATION_NAME] = "OpenRS2"
            signedManifest.mainAttributes[PERMISSIONS] = "all-permissions"
        }
    }
}
