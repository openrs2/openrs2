package org.openrs2.protocol.js5

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

@ChannelHandler.Sharable
public class Js5RequestEncoder : MessageToByteEncoder<Js5Request>(Js5Request::class.java) {
    override fun encode(ctx: ChannelHandlerContext, msg: Js5Request, out: ByteBuf) {
        when (msg) {
            is Js5Request.Group -> {
                out.writeByte(if (msg.prefetch) 0 else 1)
                out.writeByte(msg.archive)
                out.writeShort(msg.group)
            }
            is Js5Request.Rekey -> {
                out.writeByte(4)
                out.writeByte(msg.key)
                out.writeZero(2)
            }
            is Js5Request.LoggedIn -> encodeSimple(out, 2)
            is Js5Request.LoggedOut -> encodeSimple(out, 3)
            is Js5Request.Connected -> {
                out.writeByte(6)
                out.writeMedium(3)
            }
            is Js5Request.Disconnect -> encodeSimple(out, 7)
        }
    }

    private fun encodeSimple(out: ByteBuf, opcode: Int) {
        out.writeByte(opcode)
        out.writeZero(3)
    }

    override fun allocateBuffer(ctx: ChannelHandlerContext, msg: Js5Request, preferDirect: Boolean): ByteBuf {
        return if (preferDirect) {
            ctx.alloc().ioBuffer(4, 4)
        } else {
            ctx.alloc().heapBuffer(4, 4)
        }
    }
}
