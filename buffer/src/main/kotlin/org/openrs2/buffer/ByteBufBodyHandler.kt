package org.openrs2.buffer

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class ByteBufBodyHandler @Inject constructor(
    private val alloc: ByteBufAllocator
) : HttpResponse.BodyHandler<ByteBuf> {
    override fun apply(responseInfo: HttpResponse.ResponseInfo): HttpResponse.BodySubscriber<ByteBuf> {
        return object : HttpResponse.BodySubscriber<ByteBuf> {
            private val buf = alloc.compositeBuffer()
            private val future = CompletableFuture<ByteBuf>()

            override fun onSubscribe(subscription: Flow.Subscription) {
                subscription.request(Long.MAX_VALUE)
            }

            override fun onNext(item: List<ByteBuffer>) {
                for (b in item) {
                    buf.addComponent(Unpooled.wrappedBuffer(b))
                }
            }

            override fun onError(throwable: Throwable) {
                future.completeExceptionally(throwable)
                buf.release()
            }

            override fun onComplete() {
                future.complete(buf)
            }

            override fun getBody(): CompletionStage<ByteBuf> {
                return future
            }
        }
    }
}
