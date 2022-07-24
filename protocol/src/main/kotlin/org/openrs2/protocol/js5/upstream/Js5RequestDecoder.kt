package org.openrs2.protocol.js5.upstream

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.DecoderException

public class Js5RequestDecoder : ByteToMessageDecoder() {
    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
        if (input.readableBytes() < 4) {
            return
        }

        val opcode = input.readUnsignedByte().toInt()

        if (opcode == 0 || opcode == 1) {
            val archive = input.readUnsignedByte().toInt()
            val group = input.readUnsignedShort()
            out += Js5Request.Group(opcode == 0, archive, group)
        } else if (opcode == 4) {
            val key = input.readUnsignedByte().toInt()
            input.skipBytes(2)
            out += Js5Request.Rekey(key)
        } else {
            input.skipBytes(3)
            out += when (opcode) {
                2 -> Js5Request.LoggedIn
                3 -> Js5Request.LoggedOut
                6 -> Js5Request.Connected
                7 -> Js5Request.Disconnect
                else -> throw DecoderException("Unknown JS5 opcode: $opcode")
            }
        }
    }
}
