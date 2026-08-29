package org.openrs2.yaml

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import jakarta.inject.Inject
import jakarta.inject.Provider

public class ObjectMapperProvider @Inject constructor(
    private val modules: Set<Module>
) : Provider<ObjectMapper> {
    override fun get(): ObjectMapper {
        return YAMLMapper.builder()
            .addModules(kotlinModule())
            .addModules(modules)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .build()
    }
}
