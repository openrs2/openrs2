package org.openrs2.conf

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import org.openrs2.yaml.YamlModule

public object ConfigModule : AbstractModule() {
    override fun configure() {
        install(YamlModule)

        bind(Config::class.java)
            .toProvider(ConfigProvider::class.java)
            .`in`(Scopes.SINGLETON)
    }
}
