package org.openrs2.deob.util.profile

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import com.google.inject.TypeLiteral
import org.openrs2.deob.util.Module
import org.openrs2.deob.util.ModuleProvider
import org.openrs2.yaml.YamlModule

public class ProfileModule(private val profileName: String) : AbstractModule() {
    override fun configure() {
        install(YamlModule)

        bind(String::class.java)
            .annotatedWith(ProfileName::class.java)
            .toInstance(profileName)

        bind(MODULES_TYPE_LITERAL)
            .toProvider(ModuleProvider::class.java)
            .`in`(Scopes.SINGLETON)

        bind(Profile::class.java)
            .toProvider(ProfileProvider::class.java)
            .`in`(Scopes.SINGLETON)
    }

    private companion object {
        private val MODULES_TYPE_LITERAL = object : TypeLiteral<@JvmSuppressWildcards Set<Module>>() {}
    }
}
