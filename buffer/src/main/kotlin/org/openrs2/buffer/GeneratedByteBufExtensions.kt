// This file is generated automatically. DO NOT EDIT.
package org.openrs2.buffer

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

public fun ByteBuf.getByteA(index: Int): Byte {
    var value = 0
    value = value or (((getByte(index + 0).toInt() - 128) and 0xFF) shl 0)
    return value.toByte()
}

public fun ByteBuf.readByteA(): Byte {
    val index = readerIndex()
    val result = getByteA(index)
    readerIndex(index + 1)
    return result
}

public fun ByteBuf.getUnsignedByteA(index: Int): Short {
    var value = 0
    value = value or (((getByte(index + 0).toInt() - 128) and 0xFF) shl 0)
    return value.toShort()
}

public fun ByteBuf.readUnsignedByteA(): Short {
    val index = readerIndex()
    val result = getUnsignedByteA(index)
    readerIndex(index + 1)
    return result
}

public fun ByteBuf.setByteA(index: Int, `value`: Int) {
    setByte(index + 0, (value shr 0) + 128)
}

public fun ByteBuf.writeByteA(`value`: Int) {
    val index = writerIndex()
    ensureWritable(1)
    setByteA(index, value)
    writerIndex(index + 1)
}

public fun ByteBuf.getByteC(index: Int): Byte {
    var value = 0
    value = value or (((-getByte(index + 0).toInt()) and 0xFF) shl 0)
    return value.toByte()
}

public fun ByteBuf.readByteC(): Byte {
    val index = readerIndex()
    val result = getByteC(index)
    readerIndex(index + 1)
    return result
}

public fun ByteBuf.getUnsignedByteC(index: Int): Short {
    var value = 0
    value = value or (((-getByte(index + 0).toInt()) and 0xFF) shl 0)
    return value.toShort()
}

public fun ByteBuf.readUnsignedByteC(): Short {
    val index = readerIndex()
    val result = getUnsignedByteC(index)
    readerIndex(index + 1)
    return result
}

public fun ByteBuf.setByteC(index: Int, `value`: Int) {
    setByte(index + 0, -(value shr 0))
}

public fun ByteBuf.writeByteC(`value`: Int) {
    val index = writerIndex()
    ensureWritable(1)
    setByteC(index, value)
    writerIndex(index + 1)
}

public fun ByteBuf.getByteS(index: Int): Byte {
    var value = 0
    value = value or (((128 - getByte(index + 0).toInt()) and 0xFF) shl 0)
    return value.toByte()
}

public fun ByteBuf.readByteS(): Byte {
    val index = readerIndex()
    val result = getByteS(index)
    readerIndex(index + 1)
    return result
}

public fun ByteBuf.getUnsignedByteS(index: Int): Short {
    var value = 0
    value = value or (((128 - getByte(index + 0).toInt()) and 0xFF) shl 0)
    return value.toShort()
}

public fun ByteBuf.readUnsignedByteS(): Short {
    val index = readerIndex()
    val result = getUnsignedByteS(index)
    readerIndex(index + 1)
    return result
}

public fun ByteBuf.setByteS(index: Int, `value`: Int) {
    setByte(index + 0, 128 - (value shr 0))
}

public fun ByteBuf.writeByteS(`value`: Int) {
    val index = writerIndex()
    ensureWritable(1)
    setByteS(index, value)
    writerIndex(index + 1)
}

public fun ByteBuf.getShortA(index: Int): Short {
    var value = 0
    value = value or (((getByte(index + 0).toInt()) and 0xFF) shl 8)
    value = value or (((getByte(index + 1).toInt() - 128) and 0xFF) shl 0)
    return value.toShort()
}

public fun ByteBuf.readShortA(): Short {
    val index = readerIndex()
    val result = getShortA(index)
    readerIndex(index + 2)
    return result
}

public fun ByteBuf.getUnsignedShortA(index: Int): Int {
    var value = 0
    value = value or (((getByte(index + 0).toInt()) and 0xFF) shl 8)
    value = value or (((getByte(index + 1).toInt() - 128) and 0xFF) shl 0)
    return value.toInt()
}

public fun ByteBuf.readUnsignedShortA(): Int {
    val index = readerIndex()
    val result = getUnsignedShortA(index)
    readerIndex(index + 2)
    return result
}

public fun ByteBuf.setShortA(index: Int, `value`: Int) {
    setByte(index + 0, (value shr 8))
    setByte(index + 1, (value shr 0) + 128)
}

public fun ByteBuf.writeShortA(`value`: Int) {
    val index = writerIndex()
    ensureWritable(2)
    setShortA(index, value)
    writerIndex(index + 2)
}

public fun ByteBuf.getShortC(index: Int): Short {
    var value = 0
    value = value or (((getByte(index + 0).toInt()) and 0xFF) shl 8)
    value = value or (((-getByte(index + 1).toInt()) and 0xFF) shl 0)
    return value.toShort()
}

public fun ByteBuf.readShortC(): Short {
    val index = readerIndex()
    val result = getShortC(index)
    readerIndex(index + 2)
    return result
}

public fun ByteBuf.getUnsignedShortC(index: Int): Int {
    var value = 0
    value = value or (((getByte(index + 0).toInt()) and 0xFF) shl 8)
    value = value or (((-getByte(index + 1).toInt()) and 0xFF) shl 0)
    return value.toInt()
}

public fun ByteBuf.readUnsignedShortC(): Int {
    val index = readerIndex()
    val result = getUnsignedShortC(index)
    readerIndex(index + 2)
    return result
}

public fun ByteBuf.setShortC(index: Int, `value`: Int) {
    setByte(index + 0, (value shr 8))
    setByte(index + 1, -(value shr 0))
}

public fun ByteBuf.writeShortC(`value`: Int) {
    val index = writerIndex()
    ensureWritable(2)
    setShortC(index, value)
    writerIndex(index + 2)
}

public fun ByteBuf.getShortS(index: Int): Short {
    var value = 0
    value = value or (((getByte(index + 0).toInt()) and 0xFF) shl 8)
    value = value or (((128 - getByte(index + 1).toInt()) and 0xFF) shl 0)
    return value.toShort()
}

public fun ByteBuf.readShortS(): Short {
    val index = readerIndex()
    val result = getShortS(index)
    readerIndex(index + 2)
    return result
}

public fun ByteBuf.getUnsignedShortS(index: Int): Int {
    var value = 0
    value = value or (((getByte(index + 0).toInt()) and 0xFF) shl 8)
    value = value or (((128 - getByte(index + 1).toInt()) and 0xFF) shl 0)
    return value.toInt()
}

public fun ByteBuf.readUnsignedShortS(): Int {
    val index = readerIndex()
    val result = getUnsignedShortS(index)
    readerIndex(index + 2)
    return result
}

public fun ByteBuf.setShortS(index: Int, `value`: Int) {
    setByte(index + 0, (value shr 8))
    setByte(index + 1, 128 - (value shr 0))
}

public fun ByteBuf.writeShortS(`value`: Int) {
    val index = writerIndex()
    ensureWritable(2)
    setShortS(index, value)
    writerIndex(index + 2)
}

public fun ByteBuf.getShortLEA(index: Int): Short {
    var value = 0
    value = value or (((getByte(index + 0).toInt() - 128) and 0xFF) shl 0)
    value = value or (((getByte(index + 1).toInt()) and 0xFF) shl 8)
    return value.toShort()
}

public fun ByteBuf.readShortLEA(): Short {
    val index = readerIndex()
    val result = getShortLEA(index)
    readerIndex(index + 2)
    return result
}

public fun ByteBuf.getUnsignedShortLEA(index: Int): Int {
    var value = 0
    value = value or (((getByte(index + 0).toInt() - 128) and 0xFF) shl 0)
    value = value or (((getByte(index + 1).toInt()) and 0xFF) shl 8)
    return value.toInt()
}

public fun ByteBuf.readUnsignedShortLEA(): Int {
    val index = readerIndex()
    val result = getUnsignedShortLEA(index)
    readerIndex(index + 2)
    return result
}

public fun ByteBuf.setShortLEA(index: Int, `value`: Int) {
    setByte(index + 0, (value shr 0) + 128)
    setByte(index + 1, (value shr 8))
}

public fun ByteBuf.writeShortLEA(`value`: Int) {
    val index = writerIndex()
    ensureWritable(2)
    setShortLEA(index, value)
    writerIndex(index + 2)
}

public fun ByteBuf.getShortLEC(index: Int): Short {
    var value = 0
    value = value or (((-getByte(index + 0).toInt()) and 0xFF) shl 0)
    value = value or (((getByte(index + 1).toInt()) and 0xFF) shl 8)
    return value.toShort()
}

public fun ByteBuf.readShortLEC(): Short {
    val index = readerIndex()
    val result = getShortLEC(index)
    readerIndex(index + 2)
    return result
}

public fun ByteBuf.getUnsignedShortLEC(index: Int): Int {
    var value = 0
    value = value or (((-getByte(index + 0).toInt()) and 0xFF) shl 0)
    value = value or (((getByte(index + 1).toInt()) and 0xFF) shl 8)
    return value.toInt()
}

public fun ByteBuf.readUnsignedShortLEC(): Int {
    val index = readerIndex()
    val result = getUnsignedShortLEC(index)
    readerIndex(index + 2)
    return result
}

public fun ByteBuf.setShortLEC(index: Int, `value`: Int) {
    setByte(index + 0, -(value shr 0))
    setByte(index + 1, (value shr 8))
}

public fun ByteBuf.writeShortLEC(`value`: Int) {
    val index = writerIndex()
    ensureWritable(2)
    setShortLEC(index, value)
    writerIndex(index + 2)
}

public fun ByteBuf.getShortLES(index: Int): Short {
    var value = 0
    value = value or (((128 - getByte(index + 0).toInt()) and 0xFF) shl 0)
    value = value or (((getByte(index + 1).toInt()) and 0xFF) shl 8)
    return value.toShort()
}

public fun ByteBuf.readShortLES(): Short {
    val index = readerIndex()
    val result = getShortLES(index)
    readerIndex(index + 2)
    return result
}

public fun ByteBuf.getUnsignedShortLES(index: Int): Int {
    var value = 0
    value = value or (((128 - getByte(index + 0).toInt()) and 0xFF) shl 0)
    value = value or (((getByte(index + 1).toInt()) and 0xFF) shl 8)
    return value.toInt()
}

public fun ByteBuf.readUnsignedShortLES(): Int {
    val index = readerIndex()
    val result = getUnsignedShortLES(index)
    readerIndex(index + 2)
    return result
}

public fun ByteBuf.setShortLES(index: Int, `value`: Int) {
    setByte(index + 0, 128 - (value shr 0))
    setByte(index + 1, (value shr 8))
}

public fun ByteBuf.writeShortLES(`value`: Int) {
    val index = writerIndex()
    ensureWritable(2)
    setShortLES(index, value)
    writerIndex(index + 2)
}

public fun ByteBuf.getIntAlt3(index: Int): Int {
    var value = 0
    value = value or (((getByte(index + 0).toInt()) and 0xFF) shl 16)
    value = value or (((getByte(index + 1).toInt()) and 0xFF) shl 24)
    value = value or (((getByte(index + 2).toInt()) and 0xFF) shl 0)
    value = value or (((getByte(index + 3).toInt()) and 0xFF) shl 8)
    return value.toInt()
}

public fun ByteBuf.readIntAlt3(): Int {
    val index = readerIndex()
    val result = getIntAlt3(index)
    readerIndex(index + 4)
    return result
}

public fun ByteBuf.setIntAlt3(index: Int, `value`: Int) {
    setByte(index + 0, (value shr 16))
    setByte(index + 1, (value shr 24))
    setByte(index + 2, (value shr 0))
    setByte(index + 3, (value shr 8))
}

public fun ByteBuf.writeIntAlt3(`value`: Int) {
    val index = writerIndex()
    ensureWritable(4)
    setIntAlt3(index, value)
    writerIndex(index + 4)
}

public fun ByteBuf.getIntAlt3Reverse(index: Int): Int {
    var value = 0
    value = value or (((getByte(index + 0).toInt()) and 0xFF) shl 8)
    value = value or (((getByte(index + 1).toInt()) and 0xFF) shl 0)
    value = value or (((getByte(index + 2).toInt()) and 0xFF) shl 24)
    value = value or (((getByte(index + 3).toInt()) and 0xFF) shl 16)
    return value.toInt()
}

public fun ByteBuf.readIntAlt3Reverse(): Int {
    val index = readerIndex()
    val result = getIntAlt3Reverse(index)
    readerIndex(index + 4)
    return result
}

public fun ByteBuf.setIntAlt3Reverse(index: Int, `value`: Int) {
    setByte(index + 0, (value shr 8))
    setByte(index + 1, (value shr 0))
    setByte(index + 2, (value shr 24))
    setByte(index + 3, (value shr 16))
}

public fun ByteBuf.writeIntAlt3Reverse(`value`: Int) {
    val index = writerIndex()
    ensureWritable(4)
    setIntAlt3Reverse(index, value)
    writerIndex(index + 4)
}

public fun ByteBuf.getBytesA(index: Int, dst: ByteArray) {
    getBytesA(index, dst, 0, dst.size)
}

public fun ByteBuf.getBytesA(
    index: Int,
    dst: ByteArray,
    dstIndex: Int,
    len: Int
) {
    getBytes(index, dst, dstIndex, len)
    for (i in 0 until len) {
        dst[dstIndex + i] = (dst[dstIndex + i].toInt() - 128).toByte()
    }
}

public fun ByteBuf.getBytesA(index: Int, dst: ByteBuf) {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    getBytesA(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.getBytesA(
    index: Int,
    dst: ByteBuf,
    len: Int
) {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    getBytesA(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.getBytesA(
    index: Int,
    dst: ByteBuf,
    dstIndex: Int,
    len: Int
) {
    for (i in 0 until len) {
        dst.setByte(dstIndex + i, getByteA(index + i).toInt())
    }
}

public fun ByteBuf.readBytesA(dst: ByteArray) {
    readBytesA(dst, 0, dst.size)
}

public fun ByteBuf.readBytesA(
    dst: ByteArray,
    dstIndex: Int,
    len: Int
) {
    val index = readerIndex()
    getBytesA(index, dst, dstIndex, len)
    readerIndex(index + len)
}

public fun ByteBuf.readBytesA(dst: ByteBuf) {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    readBytesA(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.readBytesA(dst: ByteBuf, len: Int) {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    readBytesA(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.readBytesA(
    dst: ByteBuf,
    dstIndex: Int,
    len: Int
) {
    val index = readerIndex()
    getBytesA(index, dst, dstIndex, len)
    readerIndex(index + len)
}

public fun ByteBuf.readBytesA(len: Int): ByteBuf {
    if (len == 0) {
        return Unpooled.EMPTY_BUFFER
    }
    alloc().buffer(len).use { dst ->
        readBytesA(dst, len)
        return dst.retain()
    }
}

public fun ByteBuf.setBytesA(index: Int, src: ByteArray) {
    setBytesA(index, src, 0, src.size)
}

public fun ByteBuf.setBytesA(
    index: Int,
    src: ByteArray,
    srcIndex: Int,
    len: Int
) {
    Unpooled.wrappedBuffer(src).use { buf ->
        setBytesA(index, buf, srcIndex, len)
    }
}

public fun ByteBuf.setBytesA(index: Int, src: ByteBuf) {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    setBytesA(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.setBytesA(
    index: Int,
    src: ByteBuf,
    len: Int
) {
    val srcIndex = src.readerIndex()
    setBytesA(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.setBytesA(
    index: Int,
    src: ByteBuf,
    srcIndex: Int,
    len: Int
) {
    for (i in 0 until len) {
        setByteA(index + i, src.getByte(srcIndex + i).toInt())
    }
}

public fun ByteBuf.writeBytesA(src: ByteArray) {
    writeBytesA(src, 0, src.size)
}

public fun ByteBuf.writeBytesA(
    src: ByteArray,
    srcIndex: Int,
    len: Int
) {
    val index = writerIndex()
    ensureWritable(len)
    setBytesA(index, src, srcIndex, len)
    writerIndex(index + len)
}

public fun ByteBuf.writeBytesA(src: ByteBuf) {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    writeBytesA(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.writeBytesA(src: ByteBuf, len: Int) {
    val srcIndex = src.readerIndex()
    writeBytesA(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.writeBytesA(
    src: ByteBuf,
    srcIndex: Int,
    len: Int
) {
    val index = writerIndex()
    ensureWritable(len)
    setBytesA(index, src, srcIndex, len)
    writerIndex(index + len)
}

public fun ByteBuf.getBytesC(index: Int, dst: ByteArray) {
    getBytesC(index, dst, 0, dst.size)
}

public fun ByteBuf.getBytesC(
    index: Int,
    dst: ByteArray,
    dstIndex: Int,
    len: Int
) {
    getBytes(index, dst, dstIndex, len)
    for (i in 0 until len) {
        dst[dstIndex + i] = (-dst[dstIndex + i].toInt()).toByte()
    }
}

public fun ByteBuf.getBytesC(index: Int, dst: ByteBuf) {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    getBytesC(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.getBytesC(
    index: Int,
    dst: ByteBuf,
    len: Int
) {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    getBytesC(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.getBytesC(
    index: Int,
    dst: ByteBuf,
    dstIndex: Int,
    len: Int
) {
    for (i in 0 until len) {
        dst.setByte(dstIndex + i, getByteC(index + i).toInt())
    }
}

public fun ByteBuf.readBytesC(dst: ByteArray) {
    readBytesC(dst, 0, dst.size)
}

public fun ByteBuf.readBytesC(
    dst: ByteArray,
    dstIndex: Int,
    len: Int
) {
    val index = readerIndex()
    getBytesC(index, dst, dstIndex, len)
    readerIndex(index + len)
}

public fun ByteBuf.readBytesC(dst: ByteBuf) {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    readBytesC(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.readBytesC(dst: ByteBuf, len: Int) {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    readBytesC(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.readBytesC(
    dst: ByteBuf,
    dstIndex: Int,
    len: Int
) {
    val index = readerIndex()
    getBytesC(index, dst, dstIndex, len)
    readerIndex(index + len)
}

public fun ByteBuf.readBytesC(len: Int): ByteBuf {
    if (len == 0) {
        return Unpooled.EMPTY_BUFFER
    }
    alloc().buffer(len).use { dst ->
        readBytesC(dst, len)
        return dst.retain()
    }
}

public fun ByteBuf.setBytesC(index: Int, src: ByteArray) {
    setBytesC(index, src, 0, src.size)
}

public fun ByteBuf.setBytesC(
    index: Int,
    src: ByteArray,
    srcIndex: Int,
    len: Int
) {
    Unpooled.wrappedBuffer(src).use { buf ->
        setBytesC(index, buf, srcIndex, len)
    }
}

public fun ByteBuf.setBytesC(index: Int, src: ByteBuf) {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    setBytesC(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.setBytesC(
    index: Int,
    src: ByteBuf,
    len: Int
) {
    val srcIndex = src.readerIndex()
    setBytesC(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.setBytesC(
    index: Int,
    src: ByteBuf,
    srcIndex: Int,
    len: Int
) {
    for (i in 0 until len) {
        setByteC(index + i, src.getByte(srcIndex + i).toInt())
    }
}

public fun ByteBuf.writeBytesC(src: ByteArray) {
    writeBytesC(src, 0, src.size)
}

public fun ByteBuf.writeBytesC(
    src: ByteArray,
    srcIndex: Int,
    len: Int
) {
    val index = writerIndex()
    ensureWritable(len)
    setBytesC(index, src, srcIndex, len)
    writerIndex(index + len)
}

public fun ByteBuf.writeBytesC(src: ByteBuf) {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    writeBytesC(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.writeBytesC(src: ByteBuf, len: Int) {
    val srcIndex = src.readerIndex()
    writeBytesC(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.writeBytesC(
    src: ByteBuf,
    srcIndex: Int,
    len: Int
) {
    val index = writerIndex()
    ensureWritable(len)
    setBytesC(index, src, srcIndex, len)
    writerIndex(index + len)
}

public fun ByteBuf.getBytesS(index: Int, dst: ByteArray) {
    getBytesS(index, dst, 0, dst.size)
}

public fun ByteBuf.getBytesS(
    index: Int,
    dst: ByteArray,
    dstIndex: Int,
    len: Int
) {
    getBytes(index, dst, dstIndex, len)
    for (i in 0 until len) {
        dst[dstIndex + i] = (128 - dst[dstIndex + i].toInt()).toByte()
    }
}

public fun ByteBuf.getBytesS(index: Int, dst: ByteBuf) {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    getBytesS(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.getBytesS(
    index: Int,
    dst: ByteBuf,
    len: Int
) {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    getBytesS(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.getBytesS(
    index: Int,
    dst: ByteBuf,
    dstIndex: Int,
    len: Int
) {
    for (i in 0 until len) {
        dst.setByte(dstIndex + i, getByteS(index + i).toInt())
    }
}

public fun ByteBuf.readBytesS(dst: ByteArray) {
    readBytesS(dst, 0, dst.size)
}

public fun ByteBuf.readBytesS(
    dst: ByteArray,
    dstIndex: Int,
    len: Int
) {
    val index = readerIndex()
    getBytesS(index, dst, dstIndex, len)
    readerIndex(index + len)
}

public fun ByteBuf.readBytesS(dst: ByteBuf) {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    readBytesS(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.readBytesS(dst: ByteBuf, len: Int) {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    readBytesS(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.readBytesS(
    dst: ByteBuf,
    dstIndex: Int,
    len: Int
) {
    val index = readerIndex()
    getBytesS(index, dst, dstIndex, len)
    readerIndex(index + len)
}

public fun ByteBuf.readBytesS(len: Int): ByteBuf {
    if (len == 0) {
        return Unpooled.EMPTY_BUFFER
    }
    alloc().buffer(len).use { dst ->
        readBytesS(dst, len)
        return dst.retain()
    }
}

public fun ByteBuf.setBytesS(index: Int, src: ByteArray) {
    setBytesS(index, src, 0, src.size)
}

public fun ByteBuf.setBytesS(
    index: Int,
    src: ByteArray,
    srcIndex: Int,
    len: Int
) {
    Unpooled.wrappedBuffer(src).use { buf ->
        setBytesS(index, buf, srcIndex, len)
    }
}

public fun ByteBuf.setBytesS(index: Int, src: ByteBuf) {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    setBytesS(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.setBytesS(
    index: Int,
    src: ByteBuf,
    len: Int
) {
    val srcIndex = src.readerIndex()
    setBytesS(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.setBytesS(
    index: Int,
    src: ByteBuf,
    srcIndex: Int,
    len: Int
) {
    for (i in 0 until len) {
        setByteS(index + i, src.getByte(srcIndex + i).toInt())
    }
}

public fun ByteBuf.writeBytesS(src: ByteArray) {
    writeBytesS(src, 0, src.size)
}

public fun ByteBuf.writeBytesS(
    src: ByteArray,
    srcIndex: Int,
    len: Int
) {
    val index = writerIndex()
    ensureWritable(len)
    setBytesS(index, src, srcIndex, len)
    writerIndex(index + len)
}

public fun ByteBuf.writeBytesS(src: ByteBuf) {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    writeBytesS(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.writeBytesS(src: ByteBuf, len: Int) {
    val srcIndex = src.readerIndex()
    writeBytesS(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.writeBytesS(
    src: ByteBuf,
    srcIndex: Int,
    len: Int
) {
    val index = writerIndex()
    ensureWritable(len)
    setBytesS(index, src, srcIndex, len)
    writerIndex(index + len)
}

public fun ByteBuf.getBytesReverse(index: Int, dst: ByteArray) {
    getBytesReverse(index, dst, 0, dst.size)
}

public fun ByteBuf.getBytesReverse(
    index: Int,
    dst: ByteArray,
    dstIndex: Int,
    len: Int
) {
    getBytes(index, dst, dstIndex, len)
    for (i in 0 until len) {
        dst[dstIndex + i] = (dst[dstIndex + i].toInt()).toByte()
    }
    dst.reverse(dstIndex, dstIndex + len)
}

public fun ByteBuf.getBytesReverse(index: Int, dst: ByteBuf) {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    getBytesReverse(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.getBytesReverse(
    index: Int,
    dst: ByteBuf,
    len: Int
) {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    getBytesReverse(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.getBytesReverse(
    index: Int,
    dst: ByteBuf,
    dstIndex: Int,
    len: Int
) {
    for (i in 0 until len) {
        dst.setByte(dstIndex + i, getByte(index + len - i - 1).toInt())
    }
}

public fun ByteBuf.readBytesReverse(dst: ByteArray) {
    readBytesReverse(dst, 0, dst.size)
}

public fun ByteBuf.readBytesReverse(
    dst: ByteArray,
    dstIndex: Int,
    len: Int
) {
    val index = readerIndex()
    getBytesReverse(index, dst, dstIndex, len)
    readerIndex(index + len)
}

public fun ByteBuf.readBytesReverse(dst: ByteBuf) {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    readBytesReverse(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.readBytesReverse(dst: ByteBuf, len: Int) {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    readBytesReverse(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.readBytesReverse(
    dst: ByteBuf,
    dstIndex: Int,
    len: Int
) {
    val index = readerIndex()
    getBytesReverse(index, dst, dstIndex, len)
    readerIndex(index + len)
}

public fun ByteBuf.readBytesReverse(len: Int): ByteBuf {
    if (len == 0) {
        return Unpooled.EMPTY_BUFFER
    }
    alloc().buffer(len).use { dst ->
        readBytesReverse(dst, len)
        return dst.retain()
    }
}

public fun ByteBuf.setBytesReverse(index: Int, src: ByteArray) {
    setBytesReverse(index, src, 0, src.size)
}

public fun ByteBuf.setBytesReverse(
    index: Int,
    src: ByteArray,
    srcIndex: Int,
    len: Int
) {
    Unpooled.wrappedBuffer(src).use { buf ->
        setBytesReverse(index, buf, srcIndex, len)
    }
}

public fun ByteBuf.setBytesReverse(index: Int, src: ByteBuf) {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    setBytesReverse(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.setBytesReverse(
    index: Int,
    src: ByteBuf,
    len: Int
) {
    val srcIndex = src.readerIndex()
    setBytesReverse(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.setBytesReverse(
    index: Int,
    src: ByteBuf,
    srcIndex: Int,
    len: Int
) {
    for (i in 0 until len) {
        setByte(index + i, src.getByte(srcIndex + len - i - 1).toInt())
    }
}

public fun ByteBuf.writeBytesReverse(src: ByteArray) {
    writeBytesReverse(src, 0, src.size)
}

public fun ByteBuf.writeBytesReverse(
    src: ByteArray,
    srcIndex: Int,
    len: Int
) {
    val index = writerIndex()
    ensureWritable(len)
    setBytesReverse(index, src, srcIndex, len)
    writerIndex(index + len)
}

public fun ByteBuf.writeBytesReverse(src: ByteBuf) {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    writeBytesReverse(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.writeBytesReverse(src: ByteBuf, len: Int) {
    val srcIndex = src.readerIndex()
    writeBytesReverse(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.writeBytesReverse(
    src: ByteBuf,
    srcIndex: Int,
    len: Int
) {
    val index = writerIndex()
    ensureWritable(len)
    setBytesReverse(index, src, srcIndex, len)
    writerIndex(index + len)
}

public fun ByteBuf.getBytesReverseA(index: Int, dst: ByteArray) {
    getBytesReverseA(index, dst, 0, dst.size)
}

public fun ByteBuf.getBytesReverseA(
    index: Int,
    dst: ByteArray,
    dstIndex: Int,
    len: Int
) {
    getBytes(index, dst, dstIndex, len)
    for (i in 0 until len) {
        dst[dstIndex + i] = (dst[dstIndex + i].toInt() - 128).toByte()
    }
    dst.reverse(dstIndex, dstIndex + len)
}

public fun ByteBuf.getBytesReverseA(index: Int, dst: ByteBuf) {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    getBytesReverseA(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.getBytesReverseA(
    index: Int,
    dst: ByteBuf,
    len: Int
) {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    getBytesReverseA(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.getBytesReverseA(
    index: Int,
    dst: ByteBuf,
    dstIndex: Int,
    len: Int
) {
    for (i in 0 until len) {
        dst.setByte(dstIndex + i, getByteA(index + len - i - 1).toInt())
    }
}

public fun ByteBuf.readBytesReverseA(dst: ByteArray) {
    readBytesReverseA(dst, 0, dst.size)
}

public fun ByteBuf.readBytesReverseA(
    dst: ByteArray,
    dstIndex: Int,
    len: Int
) {
    val index = readerIndex()
    getBytesReverseA(index, dst, dstIndex, len)
    readerIndex(index + len)
}

public fun ByteBuf.readBytesReverseA(dst: ByteBuf) {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    readBytesReverseA(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.readBytesReverseA(dst: ByteBuf, len: Int) {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    readBytesReverseA(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.readBytesReverseA(
    dst: ByteBuf,
    dstIndex: Int,
    len: Int
) {
    val index = readerIndex()
    getBytesReverseA(index, dst, dstIndex, len)
    readerIndex(index + len)
}

public fun ByteBuf.readBytesReverseA(len: Int): ByteBuf {
    if (len == 0) {
        return Unpooled.EMPTY_BUFFER
    }
    alloc().buffer(len).use { dst ->
        readBytesReverseA(dst, len)
        return dst.retain()
    }
}

public fun ByteBuf.setBytesReverseA(index: Int, src: ByteArray) {
    setBytesReverseA(index, src, 0, src.size)
}

public fun ByteBuf.setBytesReverseA(
    index: Int,
    src: ByteArray,
    srcIndex: Int,
    len: Int
) {
    Unpooled.wrappedBuffer(src).use { buf ->
        setBytesReverseA(index, buf, srcIndex, len)
    }
}

public fun ByteBuf.setBytesReverseA(index: Int, src: ByteBuf) {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    setBytesReverseA(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.setBytesReverseA(
    index: Int,
    src: ByteBuf,
    len: Int
) {
    val srcIndex = src.readerIndex()
    setBytesReverseA(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.setBytesReverseA(
    index: Int,
    src: ByteBuf,
    srcIndex: Int,
    len: Int
) {
    for (i in 0 until len) {
        setByteA(index + i, src.getByte(srcIndex + len - i - 1).toInt())
    }
}

public fun ByteBuf.writeBytesReverseA(src: ByteArray) {
    writeBytesReverseA(src, 0, src.size)
}

public fun ByteBuf.writeBytesReverseA(
    src: ByteArray,
    srcIndex: Int,
    len: Int
) {
    val index = writerIndex()
    ensureWritable(len)
    setBytesReverseA(index, src, srcIndex, len)
    writerIndex(index + len)
}

public fun ByteBuf.writeBytesReverseA(src: ByteBuf) {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    writeBytesReverseA(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.writeBytesReverseA(src: ByteBuf, len: Int) {
    val srcIndex = src.readerIndex()
    writeBytesReverseA(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.writeBytesReverseA(
    src: ByteBuf,
    srcIndex: Int,
    len: Int
) {
    val index = writerIndex()
    ensureWritable(len)
    setBytesReverseA(index, src, srcIndex, len)
    writerIndex(index + len)
}

public fun ByteBuf.getBytesReverseC(index: Int, dst: ByteArray) {
    getBytesReverseC(index, dst, 0, dst.size)
}

public fun ByteBuf.getBytesReverseC(
    index: Int,
    dst: ByteArray,
    dstIndex: Int,
    len: Int
) {
    getBytes(index, dst, dstIndex, len)
    for (i in 0 until len) {
        dst[dstIndex + i] = (-dst[dstIndex + i].toInt()).toByte()
    }
    dst.reverse(dstIndex, dstIndex + len)
}

public fun ByteBuf.getBytesReverseC(index: Int, dst: ByteBuf) {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    getBytesReverseC(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.getBytesReverseC(
    index: Int,
    dst: ByteBuf,
    len: Int
) {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    getBytesReverseC(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.getBytesReverseC(
    index: Int,
    dst: ByteBuf,
    dstIndex: Int,
    len: Int
) {
    for (i in 0 until len) {
        dst.setByte(dstIndex + i, getByteC(index + len - i - 1).toInt())
    }
}

public fun ByteBuf.readBytesReverseC(dst: ByteArray) {
    readBytesReverseC(dst, 0, dst.size)
}

public fun ByteBuf.readBytesReverseC(
    dst: ByteArray,
    dstIndex: Int,
    len: Int
) {
    val index = readerIndex()
    getBytesReverseC(index, dst, dstIndex, len)
    readerIndex(index + len)
}

public fun ByteBuf.readBytesReverseC(dst: ByteBuf) {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    readBytesReverseC(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.readBytesReverseC(dst: ByteBuf, len: Int) {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    readBytesReverseC(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.readBytesReverseC(
    dst: ByteBuf,
    dstIndex: Int,
    len: Int
) {
    val index = readerIndex()
    getBytesReverseC(index, dst, dstIndex, len)
    readerIndex(index + len)
}

public fun ByteBuf.readBytesReverseC(len: Int): ByteBuf {
    if (len == 0) {
        return Unpooled.EMPTY_BUFFER
    }
    alloc().buffer(len).use { dst ->
        readBytesReverseC(dst, len)
        return dst.retain()
    }
}

public fun ByteBuf.setBytesReverseC(index: Int, src: ByteArray) {
    setBytesReverseC(index, src, 0, src.size)
}

public fun ByteBuf.setBytesReverseC(
    index: Int,
    src: ByteArray,
    srcIndex: Int,
    len: Int
) {
    Unpooled.wrappedBuffer(src).use { buf ->
        setBytesReverseC(index, buf, srcIndex, len)
    }
}

public fun ByteBuf.setBytesReverseC(index: Int, src: ByteBuf) {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    setBytesReverseC(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.setBytesReverseC(
    index: Int,
    src: ByteBuf,
    len: Int
) {
    val srcIndex = src.readerIndex()
    setBytesReverseC(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.setBytesReverseC(
    index: Int,
    src: ByteBuf,
    srcIndex: Int,
    len: Int
) {
    for (i in 0 until len) {
        setByteC(index + i, src.getByte(srcIndex + len - i - 1).toInt())
    }
}

public fun ByteBuf.writeBytesReverseC(src: ByteArray) {
    writeBytesReverseC(src, 0, src.size)
}

public fun ByteBuf.writeBytesReverseC(
    src: ByteArray,
    srcIndex: Int,
    len: Int
) {
    val index = writerIndex()
    ensureWritable(len)
    setBytesReverseC(index, src, srcIndex, len)
    writerIndex(index + len)
}

public fun ByteBuf.writeBytesReverseC(src: ByteBuf) {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    writeBytesReverseC(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.writeBytesReverseC(src: ByteBuf, len: Int) {
    val srcIndex = src.readerIndex()
    writeBytesReverseC(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.writeBytesReverseC(
    src: ByteBuf,
    srcIndex: Int,
    len: Int
) {
    val index = writerIndex()
    ensureWritable(len)
    setBytesReverseC(index, src, srcIndex, len)
    writerIndex(index + len)
}

public fun ByteBuf.getBytesReverseS(index: Int, dst: ByteArray) {
    getBytesReverseS(index, dst, 0, dst.size)
}

public fun ByteBuf.getBytesReverseS(
    index: Int,
    dst: ByteArray,
    dstIndex: Int,
    len: Int
) {
    getBytes(index, dst, dstIndex, len)
    for (i in 0 until len) {
        dst[dstIndex + i] = (128 - dst[dstIndex + i].toInt()).toByte()
    }
    dst.reverse(dstIndex, dstIndex + len)
}

public fun ByteBuf.getBytesReverseS(index: Int, dst: ByteBuf) {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    getBytesReverseS(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.getBytesReverseS(
    index: Int,
    dst: ByteBuf,
    len: Int
) {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    getBytesReverseS(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.getBytesReverseS(
    index: Int,
    dst: ByteBuf,
    dstIndex: Int,
    len: Int
) {
    for (i in 0 until len) {
        dst.setByte(dstIndex + i, getByteS(index + len - i - 1).toInt())
    }
}

public fun ByteBuf.readBytesReverseS(dst: ByteArray) {
    readBytesReverseS(dst, 0, dst.size)
}

public fun ByteBuf.readBytesReverseS(
    dst: ByteArray,
    dstIndex: Int,
    len: Int
) {
    val index = readerIndex()
    getBytesReverseS(index, dst, dstIndex, len)
    readerIndex(index + len)
}

public fun ByteBuf.readBytesReverseS(dst: ByteBuf) {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    readBytesReverseS(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.readBytesReverseS(dst: ByteBuf, len: Int) {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    readBytesReverseS(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
}

public fun ByteBuf.readBytesReverseS(
    dst: ByteBuf,
    dstIndex: Int,
    len: Int
) {
    val index = readerIndex()
    getBytesReverseS(index, dst, dstIndex, len)
    readerIndex(index + len)
}

public fun ByteBuf.readBytesReverseS(len: Int): ByteBuf {
    if (len == 0) {
        return Unpooled.EMPTY_BUFFER
    }
    alloc().buffer(len).use { dst ->
        readBytesReverseS(dst, len)
        return dst.retain()
    }
}

public fun ByteBuf.setBytesReverseS(index: Int, src: ByteArray) {
    setBytesReverseS(index, src, 0, src.size)
}

public fun ByteBuf.setBytesReverseS(
    index: Int,
    src: ByteArray,
    srcIndex: Int,
    len: Int
) {
    Unpooled.wrappedBuffer(src).use { buf ->
        setBytesReverseS(index, buf, srcIndex, len)
    }
}

public fun ByteBuf.setBytesReverseS(index: Int, src: ByteBuf) {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    setBytesReverseS(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.setBytesReverseS(
    index: Int,
    src: ByteBuf,
    len: Int
) {
    val srcIndex = src.readerIndex()
    setBytesReverseS(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.setBytesReverseS(
    index: Int,
    src: ByteBuf,
    srcIndex: Int,
    len: Int
) {
    for (i in 0 until len) {
        setByteS(index + i, src.getByte(srcIndex + len - i - 1).toInt())
    }
}

public fun ByteBuf.writeBytesReverseS(src: ByteArray) {
    writeBytesReverseS(src, 0, src.size)
}

public fun ByteBuf.writeBytesReverseS(
    src: ByteArray,
    srcIndex: Int,
    len: Int
) {
    val index = writerIndex()
    ensureWritable(len)
    setBytesReverseS(index, src, srcIndex, len)
    writerIndex(index + len)
}

public fun ByteBuf.writeBytesReverseS(src: ByteBuf) {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    writeBytesReverseS(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.writeBytesReverseS(src: ByteBuf, len: Int) {
    val srcIndex = src.readerIndex()
    writeBytesReverseS(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
}

public fun ByteBuf.writeBytesReverseS(
    src: ByteBuf,
    srcIndex: Int,
    len: Int
) {
    val index = writerIndex()
    ensureWritable(len)
    setBytesReverseS(index, src, srcIndex, len)
    writerIndex(index + len)
}

public fun ByteBuf.getBooleanA(index: Int): Boolean = getByteA(index).toInt() != 0

public fun ByteBuf.readBooleanA(): Boolean = readByteA().toInt() != 0

public fun ByteBuf.setBooleanA(index: Int, `value`: Boolean) {
    if (value) {
        setByteA(index, 1)
    } else {
        setByteA(index, 0)
    }
}

public fun ByteBuf.writeBooleanA(`value`: Boolean) {
    if (value) {
        writeByteA(1)
    } else {
        writeByteA(0)
    }
}

public fun ByteBuf.getBooleanC(index: Int): Boolean = getByteC(index).toInt() != 0

public fun ByteBuf.readBooleanC(): Boolean = readByteC().toInt() != 0

public fun ByteBuf.setBooleanC(index: Int, `value`: Boolean) {
    if (value) {
        setByteC(index, 1)
    } else {
        setByteC(index, 0)
    }
}

public fun ByteBuf.writeBooleanC(`value`: Boolean) {
    if (value) {
        writeByteC(1)
    } else {
        writeByteC(0)
    }
}

public fun ByteBuf.getBooleanS(index: Int): Boolean = getByteS(index).toInt() != 0

public fun ByteBuf.readBooleanS(): Boolean = readByteS().toInt() != 0

public fun ByteBuf.setBooleanS(index: Int, `value`: Boolean) {
    if (value) {
        setByteS(index, 1)
    } else {
        setByteS(index, 0)
    }
}

public fun ByteBuf.writeBooleanS(`value`: Boolean) {
    if (value) {
        writeByteS(1)
    } else {
        writeByteS(0)
    }
}
