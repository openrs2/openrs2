package org.openrs2.net

import io.netty.util.concurrent.Future
import java.util.concurrent.CompletableFuture
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

public fun <V> Future<V>.asCompletableFuture(): CompletableFuture<V> {
    if (isDone) {
        return if (isSuccess) {
            CompletableFuture.completedFuture(now)
        } else {
            CompletableFuture.failedFuture(cause())
        }
    }

    val future = CompletableFuture<V>()

    addListener {
        if (isSuccess) {
            future.complete(now)
        } else {
            future.completeExceptionally(cause())
        }
    }

    return future
}
