package org.openrs2.http

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import java.net.http.HttpClient

public object HttpModule : AbstractModule() {
    override fun configure() {
        bind(HttpClient::class.java)
            .toProvider(HttpClientProvider::class.java)
            .`in`(Scopes.SINGLETON)
    }
}
