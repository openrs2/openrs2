package org.openrs2.protocol

import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.DecoderException
import org.openrs2.crypto.NopStreamCipher
import org.openrs2.crypto.StreamCipher

public class Rs2Decoder(public var protocol: Protocol) : ByteToMessageDecoder() {
    private enum class State {
        READ_OPCODE,
        READ_LENGTH,
        READ_PAYLOAD
    }

    public var cipher: StreamCipher = NopStreamCipher
    private var state = State.READ_OPCODE
    private lateinit var decoder: PacketCodec<*>
    private var length = 0

    init {
        isSingleDecode = true
    }

    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
        if (state == State.READ_OPCODE) {
            if (!input.isReadable) {
                return
            }

            val opcode = (input.readUnsignedByte().toInt() - cipher.nextInt()) and 0xFF
            decoder = protocol.getDecoder(opcode) ?: throw DecoderException("Unsupported opcode: $opcode")
            length = decoder.length

            state = State.READ_LENGTH
        }

        if (state == State.READ_LENGTH) {
            when (length) {
                PacketLength.VARIABLE_BYTE -> {
                    if (!input.isReadable) {
                        return
                    }

                    length = input.readUnsignedByte().toInt()
                }
                PacketLength.VARIABLE_SHORT -> {
                    if (input.readableBytes() < 2) {
                        return
                    }

                    length = input.readUnsignedShort()
                }
            }

            state = State.READ_PAYLOAD
        }

        if (state == State.READ_PAYLOAD) {
            if (input.readableBytes() < length) {
                return
            }

            val payload = input.readSlice(length)
            out += try {
                decoder.decode(payload, cipher)
            } catch (ex: NotImplementedError) {
                // TODO(gpe): remove this catch block when every packet is implemented
                logger.warn { "Skipping unimplemented packet: ${decoder.javaClass}" }
            }

            state = State.READ_OPCODE
        }
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
