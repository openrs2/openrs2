package org.openrs2.json

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import jakarta.inject.Inject
import jakarta.inject.Provider

public class ObjectMapperProvider @Inject constructor(
    private val modules: Set<Module>
) : Provider<ObjectMapper> {
    override fun get(): ObjectMapper {
        return ObjectMapper()
            .registerKotlinModule()
            .registerModules(modules)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setDefaultPrettyPrinter(JsonPrettyPrinter())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
    }
}
