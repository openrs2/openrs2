package org.openrs2.http

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.net.http.HttpClient
import javax.inject.Provider

public class HttpClientProvider : Provider<HttpClient> {
    override fun get(): HttpClient {
        return HttpClient.newBuilder()
            .authenticator(NetrcAuthenticator.read())
            .executor(Dispatchers.IO.asExecutor())
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }
}
