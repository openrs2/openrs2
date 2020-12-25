package org.openrs2.deob.bytecode

import com.fasterxml.jackson.databind.ObjectMapper
import org.openrs2.yaml.Yaml
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Provider

public class ProfileProvider @Inject constructor(@Yaml private val mapper: ObjectMapper) : Provider<Profile> {
    override fun get(): Profile {
        return Files.newBufferedReader(PATH).use { reader ->
            mapper.readValue(reader, Profile::class.java)
        }
    }

    private companion object {
        private val PATH = Paths.get("share/deob/profile.yaml")
    }
}
