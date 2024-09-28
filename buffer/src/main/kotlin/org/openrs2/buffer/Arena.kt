package org.openrs2.buffer

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.CompositeByteBuf
import io.netty.util.ReferenceCounted

public class Arena(
    private val alloc: ByteBufAllocator,
) : ByteBufAllocator, AutoCloseable {
    private val buffers = mutableListOf<ByteBuf>()

    override fun buffer(): ByteBuf {
        val buf = alloc.buffer()
        buffers += buf
        return buf
    }

    override fun buffer(initialCapacity: Int): ByteBuf {
        val buf = alloc.buffer(initialCapacity)
        buffers += buf
        return buf
    }

    override fun buffer(initialCapacity: Int, maxCapacity: Int): ByteBuf {
        val buf = alloc.buffer(initialCapacity, maxCapacity)
        buffers += buf
        return buf
    }

    override fun ioBuffer(): ByteBuf {
        val buf = alloc.ioBuffer()
        buffers += buf
        return buf
    }

    override fun ioBuffer(initialCapacity: Int): ByteBuf {
        val buf = alloc.ioBuffer(initialCapacity)
        buffers += buf
        return buf
    }

    override fun ioBuffer(initialCapacity: Int, maxCapacity: Int): ByteBuf {
        val buf = alloc.ioBuffer(initialCapacity, maxCapacity)
        buffers += buf
        return buf
    }

    override fun heapBuffer(): ByteBuf {
        val buf = alloc.heapBuffer()
        buffers += buf
        return buf
    }

    override fun heapBuffer(initialCapacity: Int): ByteBuf {
        val buf = alloc.heapBuffer(initialCapacity)
        buffers += buf
        return buf
    }

    override fun heapBuffer(initialCapacity: Int, maxCapacity: Int): ByteBuf {
        val buf = alloc.heapBuffer(initialCapacity, maxCapacity)
        buffers += buf
        return buf
    }

    override fun directBuffer(): ByteBuf {
        val buf = alloc.directBuffer()
        buffers += buf
        return buf
    }

    override fun directBuffer(initialCapacity: Int): ByteBuf {
        val buf = alloc.directBuffer(initialCapacity)
        buffers += buf
        return buf
    }

    override fun directBuffer(initialCapacity: Int, maxCapacity: Int): ByteBuf {
        val buf = alloc.directBuffer(initialCapacity, maxCapacity)
        buffers += buf
        return buf
    }

    override fun compositeBuffer(): CompositeByteBuf {
        val buf = alloc.compositeBuffer()
        buffers.add(buf)
        return buf
    }

    override fun compositeBuffer(maxNumComponents: Int): CompositeByteBuf {
        val buf = alloc.compositeBuffer(maxNumComponents)
        buffers.add(buf)
        return buf
    }

    override fun compositeHeapBuffer(): CompositeByteBuf {
        val buf = alloc.compositeHeapBuffer()
        buffers.add(buf)
        return buf
    }

    override fun compositeHeapBuffer(maxNumComponents: Int): CompositeByteBuf {
        val buf = alloc.compositeHeapBuffer(maxNumComponents)
        buffers.add(buf)
        return buf
    }

    override fun compositeDirectBuffer(): CompositeByteBuf {
        val buf = alloc.compositeDirectBuffer()
        buffers.add(buf)
        return buf
    }

    override fun compositeDirectBuffer(maxNumComponents: Int): CompositeByteBuf {
        val buf = alloc.compositeDirectBuffer(maxNumComponents)
        buffers.add(buf)
        return buf
    }

    override fun isDirectBufferPooled(): Boolean {
        return alloc.isDirectBufferPooled
    }

    override fun calculateNewCapacity(minNewCapacity: Int, maxCapacity: Int): Int {
        return alloc.calculateNewCapacity(minNewCapacity, maxCapacity)
    }

    public override fun close() {
        buffers.forEach(ReferenceCounted::release)
        buffers.clear()
    }
}
