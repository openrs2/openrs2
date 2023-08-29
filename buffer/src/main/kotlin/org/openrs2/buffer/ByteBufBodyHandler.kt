package org.openrs2.buffer

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Flow

@Singleton
public class ByteBufBodyHandler @Inject constructor(
    private val alloc: ByteBufAllocator
) : HttpResponse.BodyHandler<ByteBuf> {
    override fun apply(responseInfo: HttpResponse.ResponseInfo): HttpResponse.BodySubscriber<ByteBuf> {
        return object : HttpResponse.BodySubscriber<ByteBuf> {
            private val buf = alloc.compositeBuffer()
            private val future = CompletableFuture<ByteBuf>()
            private var len = 0

            override fun onSubscribe(subscription: Flow.Subscription) {
                subscription.request(Long.MAX_VALUE)
            }

            override fun onNext(item: List<ByteBuffer>) {
                for (b in item) {
                    val component = Unpooled.wrappedBuffer(b)
                    buf.addComponent(component)

                    len += component.readableBytes()
                    check(len >= 0)
                }
            }

            override fun onError(throwable: Throwable) {
                future.completeExceptionally(throwable)
                buf.release()
            }

            override fun onComplete() {
                buf.writerIndex(len)
                future.complete(buf)
            }

            override fun getBody(): CompletionStage<ByteBuf> {
                return future
            }
        }
    }
}
