package org.openrs2.game.net.js5

import io.netty.channel.ChannelHandlerContext
import org.openrs2.protocol.js5.Js5Request

public class Js5Client(
    public val ctx: ChannelHandlerContext
) {
    private val urgent = ArrayDeque<Js5Request.Group>()
    private val prefetch = ArrayDeque<Js5Request.Group>()

    public fun push(request: Js5Request.Group) {
        if (request.prefetch) {
            prefetch += request
        } else {
            urgent += request
        }
    }

    public fun pop(): Js5Request.Group? {
        val request = urgent.removeFirstOrNull()
        if (request != null) {
            return request
        }
        return prefetch.removeFirstOrNull()
    }

    public fun isNotFull(): Boolean {
        return urgent.size < MAX_QUEUE_SIZE && prefetch.size < MAX_QUEUE_SIZE
    }

    public fun isNotEmpty(): Boolean {
        return urgent.isNotEmpty() || prefetch.isNotEmpty()
    }

    public fun isReady(): Boolean {
        return ctx.channel().isWritable && isNotEmpty()
    }

    private companion object {
        private const val MAX_QUEUE_SIZE = 20
    }
}
