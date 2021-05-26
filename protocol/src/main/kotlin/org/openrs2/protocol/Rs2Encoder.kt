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

        val len = encoder.length
        val lenIndex = out.writerIndex()
        when (len) {
            PacketLength.VARIABLE_BYTE -> out.writeZero(1)
            PacketLength.VARIABLE_SHORT -> out.writeZero(2)
        }

        val payloadIndex = out.writerIndex()
        encoder.encode(msg, out, cipher)

        val written = out.writerIndex() - payloadIndex

        when (len) {
            PacketLength.VARIABLE_BYTE -> {
                if (written >= 256) {
                    throw EncoderException("Variable byte payload too long: $written bytes")
                }

                out.setByte(lenIndex, written)
            }
            PacketLength.VARIABLE_SHORT -> {
                if (written >= 65536) {
                    throw EncoderException("Variable short payload too long: $written bytes")
                }

                out.setShort(lenIndex, written)
            }
            else -> {
                if (written != len) {
                    throw EncoderException("Fixed payload length mismatch (expected $len bytes, got $written bytes)")
                }
            }
        }
    }

    override fun allocateBuffer(ctx: ChannelHandlerContext, msg: Packet, preferDirect: Boolean): ByteBuf {
        val encoder = protocol.getEncoder(msg.javaClass)
            ?: throw EncoderException("Unsupported packet type: ${msg.javaClass}")

        return encoder.allocateBuffer(ctx.alloc(), msg, preferDirect)
    }
}
