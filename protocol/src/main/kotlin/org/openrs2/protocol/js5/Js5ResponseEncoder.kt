package org.openrs2.protocol.js5

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.EncoderException
import io.netty.handler.codec.MessageToByteEncoder
import kotlin.math.min

@ChannelHandler.Sharable
public object Js5ResponseEncoder : MessageToByteEncoder<Js5Response>(Js5Response::class.java) {
    override fun encode(ctx: ChannelHandlerContext, msg: Js5Response, out: ByteBuf) {
        out.writeByte(msg.archive)
        out.writeShort(msg.group)

        if (!msg.data.isReadable) {
            // TOOD(gpe): check if the entire container is well-formed?
            throw EncoderException("Missing compression byte")
        }

        var compression = msg.data.readUnsignedByte().toInt()
        if (msg.prefetch) {
            compression = compression or 0x80
        }
        out.writeByte(compression)

        out.writeBytes(msg.data, min(msg.data.readableBytes(), 508))

        while (msg.data.isReadable) {
            out.writeByte(0xFF)
            out.writeBytes(msg.data, min(msg.data.readableBytes(), 511))
        }
    }

    override fun allocateBuffer(ctx: ChannelHandlerContext, msg: Js5Response, preferDirect: Boolean): ByteBuf {
        val dataLen = msg.data.readableBytes()
        val len = 3 + dataLen + (2 + dataLen) / 511

        return if (preferDirect) {
            ctx.alloc().ioBuffer(len, len)
        } else {
            ctx.alloc().heapBuffer(len, len)
        }
    }
}
