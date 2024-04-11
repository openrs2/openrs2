package org.openrs2.deob.bytecode

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import jakarta.inject.Provider
import org.openrs2.yaml.Yaml
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

public class ProfileProvider @Inject constructor(
    @param:Yaml private val mapper: ObjectMapper,
    @ProfileName private val profile: String,
) : Provider<Profile> {
    override fun get(): Profile {
        val path = resolveProfile()
        logger.info { "Using deobfuscation profile $path" }

        // This is TOCTOU, but it's fine for newBufferedReader to throw if the file no longer exists
        return Files.newBufferedReader(path).use { reader ->
            mapper.readValue(reader, Profile::class.java)
        }
    }

    private fun resolveProfile(): Path {
        val default = DEFAULT_DIR.resolve(profile)
        if (default.exists()) {
            return default
        }

        val unqualified = Paths.get(profile)
        if (unqualified.exists()) {
            return unqualified
        }

        throw FileNotFoundException("Failed to find deobfuscation profile (searched at: '$default', '$unqualified')")
    }

    private companion object {
        private val DEFAULT_DIR = Path.of("share/deob/")
        private val logger = InlineLogger()
    }
}
