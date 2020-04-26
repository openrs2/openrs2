package dev.openrs2.yaml

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import javax.inject.Inject
import javax.inject.Provider

class ObjectMapperProvider @Inject constructor(
    private val modules: Set<@JvmSuppressWildcards Module>
) : Provider<ObjectMapper> {
    override fun get(): ObjectMapper {
        return ObjectMapper(YAMLFactory())
            .registerKotlinModule()
            .registerModules(modules)
    }
}
