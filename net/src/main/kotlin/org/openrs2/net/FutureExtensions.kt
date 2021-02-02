package org.openrs2.net

import io.netty.util.concurrent.Future
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

public suspend fun <V> Future<V>.awaitSuspend(): V {
    if (isDone) {
        if (isSuccess) {
            return now
        } else {
            throw cause()
        }
    }

    return suspendCoroutine { continuation ->
        addListener {
            if (isSuccess) {
                continuation.resume(now)
            } else {
                continuation.resumeWithException(cause())
            }
        }
    }
}
