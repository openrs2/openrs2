package org.openrs2.archive.cache.nxt

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.DecoderException
import kotlin.math.min

public class Js5ResponseDecoder : ByteToMessageDecoder() {
    private data class Request(val prefetch: Boolean, val archive: Int, val group: Int)

    private enum class State {
        READ_HEADER,
        READ_LEN,
        READ_DATA
    }

    private var state = State.READ_HEADER
    private val buffers = mutableMapOf<Request, ByteBuf>()
    private var request: Request? = null

    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
        if (state == State.READ_HEADER) {
            if (input.readableBytes() < 5) {
                return
            }

            val prefetch: Boolean
            val archive = input.readUnsignedByte().toInt()
            var group = input.readInt()

            if (group and 0x80000000.toInt() != 0) {
                prefetch = true
                group = group and 0x7FFFFFFF
            } else {
                prefetch = false
            }

            request = Request(prefetch, archive, group)

            if (buffers.containsKey(request)) {
                state = State.READ_DATA
            } else {
                state = State.READ_LEN
            }
        }

        if (state == State.READ_LEN) {
            if (input.readableBytes() < 5) {
                return
            }

            val type = input.readUnsignedByte().toInt()

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

            val data = ctx.alloc().buffer(totalLen, totalLen)
            data.writeByte(type)
            data.writeInt(len)

            buffers[request!!] = data

            state = State.READ_DATA
        }

        if (state == State.READ_DATA) {
            val data = buffers[request!!]!!

            var blockLen = if (data.writerIndex() == 5) {
                102400 - 10
            } else {
                102400 - 5
            }

            blockLen = min(blockLen, data.writableBytes())

            if (input.readableBytes() < blockLen) {
                return
            }

            data.writeBytes(input, blockLen)

            if (!data.isWritable) {
                out += Js5Response(request!!.prefetch, request!!.archive, request!!.group, data)
                buffers.remove(request!!)
                request = null
            }

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
        buffers.values.forEach(ByteBuf::release)
        buffers.clear()

        state = State.READ_HEADER
    }
}
