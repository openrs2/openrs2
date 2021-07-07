package org.openrs2.archive.cache.nxt

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

@ChannelHandler.Sharable
public object Js5RequestEncoder : MessageToByteEncoder<Js5Request>(Js5Request::class.java) {
    override fun encode(ctx: ChannelHandlerContext, msg: Js5Request, out: ByteBuf) {
        when (msg) {
            is Js5Request.Group -> {
                out.writeByte(if (msg.prefetch) 32 else 33)
                out.writeByte(msg.archive)
                out.writeInt(msg.group)
                out.writeShort(msg.build)
                out.writeShort(0)
            }
            is Js5Request.Connected -> {
                out.writeByte(6)
                out.writeMedium(5)
                out.writeShort(0)
                out.writeShort(msg.build)
                out.writeShort(0)
            }
        }
    }

    override fun allocateBuffer(ctx: ChannelHandlerContext, msg: Js5Request, preferDirect: Boolean): ByteBuf {
        return if (preferDirect) {
            ctx.alloc().ioBuffer(10, 10)
        } else {
            ctx.alloc().heapBuffer(10, 10)
        }
    }
}
