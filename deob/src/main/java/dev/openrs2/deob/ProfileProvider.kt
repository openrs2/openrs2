package dev.openrs2.deob

import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Provider

class ProfileProvider @Inject constructor(private val mapper: ObjectMapper) : Provider<Profile> {
    override fun get(): Profile {
        return Files.newBufferedReader(Paths.get("share/deob/profile.yaml")).use { reader ->
            mapper.readValue(reader, Profile::class.java)
        }
    }
}
