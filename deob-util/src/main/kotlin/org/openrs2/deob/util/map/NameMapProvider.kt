package org.openrs2.deob.util.map

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Inject
import jakarta.inject.Provider
import org.openrs2.deob.util.profile.Profile
import org.openrs2.yaml.Yaml
import java.nio.file.Files
import java.nio.file.Path

public class NameMapProvider @Inject constructor(
    @param:Yaml private val mapper: ObjectMapper,
    private val profile: Profile
) : Provider<NameMap> {
    override fun get(): NameMap {
        val combinedMap = NameMap()
        val path = profile.mapping

        if (!Files.exists(path)) {
            return combinedMap
        }

        for (file in Files.list(path).filter(::isYamlFile)) {
            val map = Files.newBufferedReader(file).use { reader ->
                mapper.readValue(reader, NameMap::class.java)
            }
            combinedMap.add(map)
        }

        return combinedMap
    }

    private fun isYamlFile(path: Path): Boolean {
        return Files.isRegularFile(path) && path.fileName.toString().endsWith(YAML_SUFFIX)
    }

    private companion object {
        private const val YAML_SUFFIX = ".yaml"
    }
}
