package org.openrs2.deob.util.profile

import com.google.inject.AbstractModule
import com.google.inject.Scopes

public class ProfileModule(private val profileName: String) : AbstractModule() {
    override fun configure() {
        bind(String::class.java)
            .annotatedWith(ProfileName::class.java)
            .toInstance(profileName)

        bind(Profile::class.java)
            .toProvider(ProfileProvider::class.java)
            .`in`(Scopes.SINGLETON)
    }
}
