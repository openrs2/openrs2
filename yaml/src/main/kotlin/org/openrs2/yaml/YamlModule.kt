package org.openrs2.yaml

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.AbstractModule
import com.google.inject.Scopes
import com.google.inject.multibindings.Multibinder

public object YamlModule : AbstractModule() {
    override fun configure() {
        Multibinder.newSetBinder(binder(), Module::class.java)

        bind(ObjectMapper::class.java)
            .annotatedWith(Yaml::class.java)
            .toProvider(ObjectMapperProvider::class.java)
            .`in`(Scopes.SINGLETON)
    }
}
