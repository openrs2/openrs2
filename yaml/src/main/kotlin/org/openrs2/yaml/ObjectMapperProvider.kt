package org.openrs2.yaml

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import javax.inject.Inject
import javax.inject.Provider

public class ObjectMapperProvider @Inject constructor(
    private val modules: Set<Module>
) : Provider<ObjectMapper> {
    override fun get(): ObjectMapper {
        return ObjectMapper(YAMLFactory())
            .registerKotlinModule()
            .registerModules(modules)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
    }
}
