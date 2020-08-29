package dev.openrs2.deob

import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Provider

public class ProfileProvider @Inject constructor(private val mapper: ObjectMapper) : Provider<Profile> {
    override fun get(): Profile {
        return Files.newBufferedReader(PATH).use { reader ->
            mapper.readValue(reader, Profile::class.java)
        }
    }

    private companion object {
        private val PATH = Paths.get("share/deob/profile.yaml")
    }
}
