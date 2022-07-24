package org.openrs2.protocol

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.EncoderException
import io.netty.handler.codec.MessageToByteEncoder
import org.openrs2.crypto.NopStreamCipher
import org.openrs2.crypto.StreamCipher

public class Rs2Encoder(public var protocol: Protocol) : MessageToByteEncoder<Packet>(Packet::class.java) {
    public var cipher: StreamCipher = NopStreamCipher

    override fun encode(ctx: ChannelHandlerContext, msg: Packet, out: ByteBuf) {
        val encoder = protocol.getEncoder(msg.javaClass)
            ?: throw EncoderException("Unsupported packet type: ${msg.javaClass}")

        out.writeByte(encoder.opcode + cipher.nextInt())

        val lenIndex = out.writerIndex()
        encoder.writeLengthPlaceholder(out)

        val payloadIndex = out.writerIndex()
        encoder.encode(msg, out, cipher)

        val written = out.writerIndex() - payloadIndex
        encoder.setLength(out, lenIndex, written)
    }

    override fun allocateBuffer(ctx: ChannelHandlerContext, msg: Packet, preferDirect: Boolean): ByteBuf {
        val encoder = protocol.getEncoder(msg.javaClass)
            ?: throw EncoderException("Unsupported packet type: ${msg.javaClass}")

        return encoder.allocateBuffer(ctx.alloc(), msg, preferDirect)
    }
}
