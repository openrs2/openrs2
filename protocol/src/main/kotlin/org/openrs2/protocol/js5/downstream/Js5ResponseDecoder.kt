package org.openrs2.protocol.js5.downstream

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.DecoderException
import kotlin.math.min

public class Js5ResponseDecoder : ByteToMessageDecoder() {
    private enum class State {
        READ_HEADER,
        READ_DATA
    }

    private var state = State.READ_HEADER
    private var prefetch: Boolean = false
    private var archive: Int = 0
    private var group: Int = 0
    private var type: Int = 0
    private var data: ByteBuf = Unpooled.EMPTY_BUFFER

    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
        if (state == State.READ_HEADER) {
            if (input.readableBytes() < 8) {
                return
            }

            archive = input.readUnsignedByte().toInt()
            group = input.readUnsignedShort()
            type = input.readUnsignedByte().toInt()

            if (type and 0x80 != 0) {
                prefetch = true
                type = type and 0x80.inv()
            } else {
                prefetch = false
            }

            val len = input.readInt()
            if (len < 0) {
                throw DecoderException("Length is negative: $len")
            }

            val totalLen = if (type == 0) {
                len + 5
            } else {
                len + 9
            }

            if (totalLen < 0) {
                throw DecoderException("Total length exceeds maximum ByteBuf size")
            }

            // TODO(gpe): release data here?
            data = ctx.alloc().buffer(totalLen, totalLen)
            data.writeByte(type)
            data.writeInt(len)

            state = State.READ_DATA
        }

        if (state == State.READ_DATA) {
            while (data.isWritable) {
                val blockLen = min(511 - ((data.readableBytes() + 2) % 511), data.writableBytes())
                val last = data.writableBytes() <= blockLen

                val blockLenIncludingTrailer = if (last) {
                    blockLen
                } else {
                    blockLen + 1
                }

                if (input.readableBytes() < blockLenIncludingTrailer) {
                    return
                }

                data.writeBytes(input, blockLen)

                if (!last && input.readUnsignedByte().toInt() != 0xFF) {
                    reset()
                    throw DecoderException("Invalid block trailer")
                }
            }

            out += Js5Response(prefetch, archive, group, data)

            data = Unpooled.EMPTY_BUFFER

            state = State.READ_HEADER
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        super.channelInactive(ctx)
        reset()
    }

    override fun handlerRemoved0(ctx: ChannelHandlerContext?) {
        reset()
    }

    private fun reset() {
        data.release()
        data = Unpooled.EMPTY_BUFFER

        state = State.READ_HEADER
    }
}
