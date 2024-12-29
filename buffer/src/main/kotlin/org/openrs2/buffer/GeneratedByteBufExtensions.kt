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

public fun ByteBuf.setByteA(index: Int, `value`: Int): ByteBuf {
    setByte(index + 0, (value shr 0) + 128)
    return this
}

public fun ByteBuf.writeByteA(`value`: Int): ByteBuf {
    val index = writerIndex()
    ensureWritable(1)
    setByteA(index, value)
    writerIndex(index + 1)
    return this
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

public fun ByteBuf.setByteC(index: Int, `value`: Int): ByteBuf {
    setByte(index + 0, -(value shr 0))
    return this
}

public fun ByteBuf.writeByteC(`value`: Int): ByteBuf {
    val index = writerIndex()
    ensureWritable(1)
    setByteC(index, value)
    writerIndex(index + 1)
    return this
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

public fun ByteBuf.setByteS(index: Int, `value`: Int): ByteBuf {
    setByte(index + 0, 128 - (value shr 0))
    return this
}

public fun ByteBuf.writeByteS(`value`: Int): ByteBuf {
    val index = writerIndex()
    ensureWritable(1)
    setByteS(index, value)
    writerIndex(index + 1)
    return this
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

public fun ByteBuf.setShortA(index: Int, `value`: Int): ByteBuf {
    setByte(index + 0, (value shr 8))
    setByte(index + 1, (value shr 0) + 128)
    return this
}

public fun ByteBuf.writeShortA(`value`: Int): ByteBuf {
    val index = writerIndex()
    ensureWritable(2)
    setShortA(index, value)
    writerIndex(index + 2)
    return this
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

public fun ByteBuf.setShortC(index: Int, `value`: Int): ByteBuf {
    setByte(index + 0, (value shr 8))
    setByte(index + 1, -(value shr 0))
    return this
}

public fun ByteBuf.writeShortC(`value`: Int): ByteBuf {
    val index = writerIndex()
    ensureWritable(2)
    setShortC(index, value)
    writerIndex(index + 2)
    return this
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

public fun ByteBuf.setShortS(index: Int, `value`: Int): ByteBuf {
    setByte(index + 0, (value shr 8))
    setByte(index + 1, 128 - (value shr 0))
    return this
}

public fun ByteBuf.writeShortS(`value`: Int): ByteBuf {
    val index = writerIndex()
    ensureWritable(2)
    setShortS(index, value)
    writerIndex(index + 2)
    return this
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

public fun ByteBuf.setShortLEA(index: Int, `value`: Int): ByteBuf {
    setByte(index + 0, (value shr 0) + 128)
    setByte(index + 1, (value shr 8))
    return this
}

public fun ByteBuf.writeShortLEA(`value`: Int): ByteBuf {
    val index = writerIndex()
    ensureWritable(2)
    setShortLEA(index, value)
    writerIndex(index + 2)
    return this
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

public fun ByteBuf.setShortLEC(index: Int, `value`: Int): ByteBuf {
    setByte(index + 0, -(value shr 0))
    setByte(index + 1, (value shr 8))
    return this
}

public fun ByteBuf.writeShortLEC(`value`: Int): ByteBuf {
    val index = writerIndex()
    ensureWritable(2)
    setShortLEC(index, value)
    writerIndex(index + 2)
    return this
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

public fun ByteBuf.setShortLES(index: Int, `value`: Int): ByteBuf {
    setByte(index + 0, 128 - (value shr 0))
    setByte(index + 1, (value shr 8))
    return this
}

public fun ByteBuf.writeShortLES(`value`: Int): ByteBuf {
    val index = writerIndex()
    ensureWritable(2)
    setShortLES(index, value)
    writerIndex(index + 2)
    return this
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

public fun ByteBuf.setIntAlt3(index: Int, `value`: Int): ByteBuf {
    setByte(index + 0, (value shr 16))
    setByte(index + 1, (value shr 24))
    setByte(index + 2, (value shr 0))
    setByte(index + 3, (value shr 8))
    return this
}

public fun ByteBuf.writeIntAlt3(`value`: Int): ByteBuf {
    val index = writerIndex()
    ensureWritable(4)
    setIntAlt3(index, value)
    writerIndex(index + 4)
    return this
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

public fun ByteBuf.setIntAlt3Reverse(index: Int, `value`: Int): ByteBuf {
    setByte(index + 0, (value shr 8))
    setByte(index + 1, (value shr 0))
    setByte(index + 2, (value shr 24))
    setByte(index + 3, (value shr 16))
    return this
}

public fun ByteBuf.writeIntAlt3Reverse(`value`: Int): ByteBuf {
    val index = writerIndex()
    ensureWritable(4)
    setIntAlt3Reverse(index, value)
    writerIndex(index + 4)
    return this
}

public fun ByteBuf.getBytesA(index: Int, dst: ByteArray): ByteBuf {
    getBytesA(index, dst, 0, dst.size)
    return this
}

public fun ByteBuf.getBytesA(
    index: Int,
    dst: ByteArray,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    getBytes(index, dst, dstIndex, len)
    for (i in 0 until len) {
        dst[dstIndex + i] = (dst[dstIndex + i].toInt() - 128).toByte()
    }
    return this
}

public fun ByteBuf.getBytesA(index: Int, dst: ByteBuf): ByteBuf {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    getBytesA(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.getBytesA(
    index: Int,
    dst: ByteBuf,
    len: Int,
): ByteBuf {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    getBytesA(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.getBytesA(
    index: Int,
    dst: ByteBuf,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    for (i in 0 until len) {
        dst.setByte(dstIndex + i, getByteA(index + i).toInt())
    }
    return this
}

public fun ByteBuf.readBytesA(dst: ByteArray): ByteBuf {
    readBytesA(dst, 0, dst.size)
    return this
}

public fun ByteBuf.readBytesA(
    dst: ByteArray,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    val index = readerIndex()
    getBytesA(index, dst, dstIndex, len)
    readerIndex(index + len)
    return this
}

public fun ByteBuf.readBytesA(dst: ByteBuf): ByteBuf {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    readBytesA(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.readBytesA(dst: ByteBuf, len: Int): ByteBuf {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    readBytesA(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.readBytesA(
    dst: ByteBuf,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    val index = readerIndex()
    getBytesA(index, dst, dstIndex, len)
    readerIndex(index + len)
    return this
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

public fun ByteBuf.setBytesA(index: Int, src: ByteArray): ByteBuf {
    setBytesA(index, src, 0, src.size)
    return this
}

public fun ByteBuf.setBytesA(
    index: Int,
    src: ByteArray,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    Unpooled.wrappedBuffer(src).use { buf ->
        setBytesA(index, buf, srcIndex, len)
    }
    return this
}

public fun ByteBuf.setBytesA(index: Int, src: ByteBuf): ByteBuf {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    setBytesA(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.setBytesA(
    index: Int,
    src: ByteBuf,
    len: Int,
): ByteBuf {
    val srcIndex = src.readerIndex()
    setBytesA(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.setBytesA(
    index: Int,
    src: ByteBuf,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    for (i in 0 until len) {
        setByteA(index + i, src.getByte(srcIndex + i).toInt())
    }
    return this
}

public fun ByteBuf.writeBytesA(src: ByteArray): ByteBuf {
    writeBytesA(src, 0, src.size)
    return this
}

public fun ByteBuf.writeBytesA(
    src: ByteArray,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    val index = writerIndex()
    ensureWritable(len)
    setBytesA(index, src, srcIndex, len)
    writerIndex(index + len)
    return this
}

public fun ByteBuf.writeBytesA(src: ByteBuf): ByteBuf {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    writeBytesA(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.writeBytesA(src: ByteBuf, len: Int): ByteBuf {
    val srcIndex = src.readerIndex()
    writeBytesA(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.writeBytesA(
    src: ByteBuf,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    val index = writerIndex()
    ensureWritable(len)
    setBytesA(index, src, srcIndex, len)
    writerIndex(index + len)
    return this
}

public fun ByteBuf.getBytesC(index: Int, dst: ByteArray): ByteBuf {
    getBytesC(index, dst, 0, dst.size)
    return this
}

public fun ByteBuf.getBytesC(
    index: Int,
    dst: ByteArray,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    getBytes(index, dst, dstIndex, len)
    for (i in 0 until len) {
        dst[dstIndex + i] = (-dst[dstIndex + i].toInt()).toByte()
    }
    return this
}

public fun ByteBuf.getBytesC(index: Int, dst: ByteBuf): ByteBuf {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    getBytesC(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.getBytesC(
    index: Int,
    dst: ByteBuf,
    len: Int,
): ByteBuf {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    getBytesC(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.getBytesC(
    index: Int,
    dst: ByteBuf,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    for (i in 0 until len) {
        dst.setByte(dstIndex + i, getByteC(index + i).toInt())
    }
    return this
}

public fun ByteBuf.readBytesC(dst: ByteArray): ByteBuf {
    readBytesC(dst, 0, dst.size)
    return this
}

public fun ByteBuf.readBytesC(
    dst: ByteArray,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    val index = readerIndex()
    getBytesC(index, dst, dstIndex, len)
    readerIndex(index + len)
    return this
}

public fun ByteBuf.readBytesC(dst: ByteBuf): ByteBuf {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    readBytesC(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.readBytesC(dst: ByteBuf, len: Int): ByteBuf {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    readBytesC(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.readBytesC(
    dst: ByteBuf,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    val index = readerIndex()
    getBytesC(index, dst, dstIndex, len)
    readerIndex(index + len)
    return this
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

public fun ByteBuf.setBytesC(index: Int, src: ByteArray): ByteBuf {
    setBytesC(index, src, 0, src.size)
    return this
}

public fun ByteBuf.setBytesC(
    index: Int,
    src: ByteArray,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    Unpooled.wrappedBuffer(src).use { buf ->
        setBytesC(index, buf, srcIndex, len)
    }
    return this
}

public fun ByteBuf.setBytesC(index: Int, src: ByteBuf): ByteBuf {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    setBytesC(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.setBytesC(
    index: Int,
    src: ByteBuf,
    len: Int,
): ByteBuf {
    val srcIndex = src.readerIndex()
    setBytesC(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.setBytesC(
    index: Int,
    src: ByteBuf,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    for (i in 0 until len) {
        setByteC(index + i, src.getByte(srcIndex + i).toInt())
    }
    return this
}

public fun ByteBuf.writeBytesC(src: ByteArray): ByteBuf {
    writeBytesC(src, 0, src.size)
    return this
}

public fun ByteBuf.writeBytesC(
    src: ByteArray,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    val index = writerIndex()
    ensureWritable(len)
    setBytesC(index, src, srcIndex, len)
    writerIndex(index + len)
    return this
}

public fun ByteBuf.writeBytesC(src: ByteBuf): ByteBuf {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    writeBytesC(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.writeBytesC(src: ByteBuf, len: Int): ByteBuf {
    val srcIndex = src.readerIndex()
    writeBytesC(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.writeBytesC(
    src: ByteBuf,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    val index = writerIndex()
    ensureWritable(len)
    setBytesC(index, src, srcIndex, len)
    writerIndex(index + len)
    return this
}

public fun ByteBuf.getBytesS(index: Int, dst: ByteArray): ByteBuf {
    getBytesS(index, dst, 0, dst.size)
    return this
}

public fun ByteBuf.getBytesS(
    index: Int,
    dst: ByteArray,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    getBytes(index, dst, dstIndex, len)
    for (i in 0 until len) {
        dst[dstIndex + i] = (128 - dst[dstIndex + i].toInt()).toByte()
    }
    return this
}

public fun ByteBuf.getBytesS(index: Int, dst: ByteBuf): ByteBuf {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    getBytesS(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.getBytesS(
    index: Int,
    dst: ByteBuf,
    len: Int,
): ByteBuf {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    getBytesS(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.getBytesS(
    index: Int,
    dst: ByteBuf,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    for (i in 0 until len) {
        dst.setByte(dstIndex + i, getByteS(index + i).toInt())
    }
    return this
}

public fun ByteBuf.readBytesS(dst: ByteArray): ByteBuf {
    readBytesS(dst, 0, dst.size)
    return this
}

public fun ByteBuf.readBytesS(
    dst: ByteArray,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    val index = readerIndex()
    getBytesS(index, dst, dstIndex, len)
    readerIndex(index + len)
    return this
}

public fun ByteBuf.readBytesS(dst: ByteBuf): ByteBuf {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    readBytesS(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.readBytesS(dst: ByteBuf, len: Int): ByteBuf {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    readBytesS(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.readBytesS(
    dst: ByteBuf,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    val index = readerIndex()
    getBytesS(index, dst, dstIndex, len)
    readerIndex(index + len)
    return this
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

public fun ByteBuf.setBytesS(index: Int, src: ByteArray): ByteBuf {
    setBytesS(index, src, 0, src.size)
    return this
}

public fun ByteBuf.setBytesS(
    index: Int,
    src: ByteArray,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    Unpooled.wrappedBuffer(src).use { buf ->
        setBytesS(index, buf, srcIndex, len)
    }
    return this
}

public fun ByteBuf.setBytesS(index: Int, src: ByteBuf): ByteBuf {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    setBytesS(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.setBytesS(
    index: Int,
    src: ByteBuf,
    len: Int,
): ByteBuf {
    val srcIndex = src.readerIndex()
    setBytesS(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.setBytesS(
    index: Int,
    src: ByteBuf,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    for (i in 0 until len) {
        setByteS(index + i, src.getByte(srcIndex + i).toInt())
    }
    return this
}

public fun ByteBuf.writeBytesS(src: ByteArray): ByteBuf {
    writeBytesS(src, 0, src.size)
    return this
}

public fun ByteBuf.writeBytesS(
    src: ByteArray,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    val index = writerIndex()
    ensureWritable(len)
    setBytesS(index, src, srcIndex, len)
    writerIndex(index + len)
    return this
}

public fun ByteBuf.writeBytesS(src: ByteBuf): ByteBuf {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    writeBytesS(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.writeBytesS(src: ByteBuf, len: Int): ByteBuf {
    val srcIndex = src.readerIndex()
    writeBytesS(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.writeBytesS(
    src: ByteBuf,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    val index = writerIndex()
    ensureWritable(len)
    setBytesS(index, src, srcIndex, len)
    writerIndex(index + len)
    return this
}

public fun ByteBuf.getBytesReverse(index: Int, dst: ByteArray): ByteBuf {
    getBytesReverse(index, dst, 0, dst.size)
    return this
}

public fun ByteBuf.getBytesReverse(
    index: Int,
    dst: ByteArray,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    getBytes(index, dst, dstIndex, len)
    for (i in 0 until len) {
        dst[dstIndex + i] = (dst[dstIndex + i].toInt()).toByte()
    }
    dst.reverse(dstIndex, dstIndex + len)
    return this
}

public fun ByteBuf.getBytesReverse(index: Int, dst: ByteBuf): ByteBuf {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    getBytesReverse(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.getBytesReverse(
    index: Int,
    dst: ByteBuf,
    len: Int,
): ByteBuf {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    getBytesReverse(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.getBytesReverse(
    index: Int,
    dst: ByteBuf,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    for (i in 0 until len) {
        dst.setByte(dstIndex + i, getByte(index + len - i - 1).toInt())
    }
    return this
}

public fun ByteBuf.readBytesReverse(dst: ByteArray): ByteBuf {
    readBytesReverse(dst, 0, dst.size)
    return this
}

public fun ByteBuf.readBytesReverse(
    dst: ByteArray,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    val index = readerIndex()
    getBytesReverse(index, dst, dstIndex, len)
    readerIndex(index + len)
    return this
}

public fun ByteBuf.readBytesReverse(dst: ByteBuf): ByteBuf {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    readBytesReverse(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.readBytesReverse(dst: ByteBuf, len: Int): ByteBuf {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    readBytesReverse(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.readBytesReverse(
    dst: ByteBuf,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    val index = readerIndex()
    getBytesReverse(index, dst, dstIndex, len)
    readerIndex(index + len)
    return this
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

public fun ByteBuf.setBytesReverse(index: Int, src: ByteArray): ByteBuf {
    setBytesReverse(index, src, 0, src.size)
    return this
}

public fun ByteBuf.setBytesReverse(
    index: Int,
    src: ByteArray,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    Unpooled.wrappedBuffer(src).use { buf ->
        setBytesReverse(index, buf, srcIndex, len)
    }
    return this
}

public fun ByteBuf.setBytesReverse(index: Int, src: ByteBuf): ByteBuf {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    setBytesReverse(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.setBytesReverse(
    index: Int,
    src: ByteBuf,
    len: Int,
): ByteBuf {
    val srcIndex = src.readerIndex()
    setBytesReverse(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.setBytesReverse(
    index: Int,
    src: ByteBuf,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    for (i in 0 until len) {
        setByte(index + i, src.getByte(srcIndex + len - i - 1).toInt())
    }
    return this
}

public fun ByteBuf.writeBytesReverse(src: ByteArray): ByteBuf {
    writeBytesReverse(src, 0, src.size)
    return this
}

public fun ByteBuf.writeBytesReverse(
    src: ByteArray,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    val index = writerIndex()
    ensureWritable(len)
    setBytesReverse(index, src, srcIndex, len)
    writerIndex(index + len)
    return this
}

public fun ByteBuf.writeBytesReverse(src: ByteBuf): ByteBuf {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    writeBytesReverse(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.writeBytesReverse(src: ByteBuf, len: Int): ByteBuf {
    val srcIndex = src.readerIndex()
    writeBytesReverse(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.writeBytesReverse(
    src: ByteBuf,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    val index = writerIndex()
    ensureWritable(len)
    setBytesReverse(index, src, srcIndex, len)
    writerIndex(index + len)
    return this
}

public fun ByteBuf.getBytesReverseA(index: Int, dst: ByteArray): ByteBuf {
    getBytesReverseA(index, dst, 0, dst.size)
    return this
}

public fun ByteBuf.getBytesReverseA(
    index: Int,
    dst: ByteArray,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    getBytes(index, dst, dstIndex, len)
    for (i in 0 until len) {
        dst[dstIndex + i] = (dst[dstIndex + i].toInt() - 128).toByte()
    }
    dst.reverse(dstIndex, dstIndex + len)
    return this
}

public fun ByteBuf.getBytesReverseA(index: Int, dst: ByteBuf): ByteBuf {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    getBytesReverseA(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.getBytesReverseA(
    index: Int,
    dst: ByteBuf,
    len: Int,
): ByteBuf {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    getBytesReverseA(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.getBytesReverseA(
    index: Int,
    dst: ByteBuf,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    for (i in 0 until len) {
        dst.setByte(dstIndex + i, getByteA(index + len - i - 1).toInt())
    }
    return this
}

public fun ByteBuf.readBytesReverseA(dst: ByteArray): ByteBuf {
    readBytesReverseA(dst, 0, dst.size)
    return this
}

public fun ByteBuf.readBytesReverseA(
    dst: ByteArray,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    val index = readerIndex()
    getBytesReverseA(index, dst, dstIndex, len)
    readerIndex(index + len)
    return this
}

public fun ByteBuf.readBytesReverseA(dst: ByteBuf): ByteBuf {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    readBytesReverseA(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.readBytesReverseA(dst: ByteBuf, len: Int): ByteBuf {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    readBytesReverseA(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.readBytesReverseA(
    dst: ByteBuf,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    val index = readerIndex()
    getBytesReverseA(index, dst, dstIndex, len)
    readerIndex(index + len)
    return this
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

public fun ByteBuf.setBytesReverseA(index: Int, src: ByteArray): ByteBuf {
    setBytesReverseA(index, src, 0, src.size)
    return this
}

public fun ByteBuf.setBytesReverseA(
    index: Int,
    src: ByteArray,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    Unpooled.wrappedBuffer(src).use { buf ->
        setBytesReverseA(index, buf, srcIndex, len)
    }
    return this
}

public fun ByteBuf.setBytesReverseA(index: Int, src: ByteBuf): ByteBuf {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    setBytesReverseA(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.setBytesReverseA(
    index: Int,
    src: ByteBuf,
    len: Int,
): ByteBuf {
    val srcIndex = src.readerIndex()
    setBytesReverseA(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.setBytesReverseA(
    index: Int,
    src: ByteBuf,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    for (i in 0 until len) {
        setByteA(index + i, src.getByte(srcIndex + len - i - 1).toInt())
    }
    return this
}

public fun ByteBuf.writeBytesReverseA(src: ByteArray): ByteBuf {
    writeBytesReverseA(src, 0, src.size)
    return this
}

public fun ByteBuf.writeBytesReverseA(
    src: ByteArray,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    val index = writerIndex()
    ensureWritable(len)
    setBytesReverseA(index, src, srcIndex, len)
    writerIndex(index + len)
    return this
}

public fun ByteBuf.writeBytesReverseA(src: ByteBuf): ByteBuf {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    writeBytesReverseA(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.writeBytesReverseA(src: ByteBuf, len: Int): ByteBuf {
    val srcIndex = src.readerIndex()
    writeBytesReverseA(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.writeBytesReverseA(
    src: ByteBuf,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    val index = writerIndex()
    ensureWritable(len)
    setBytesReverseA(index, src, srcIndex, len)
    writerIndex(index + len)
    return this
}

public fun ByteBuf.getBytesReverseC(index: Int, dst: ByteArray): ByteBuf {
    getBytesReverseC(index, dst, 0, dst.size)
    return this
}

public fun ByteBuf.getBytesReverseC(
    index: Int,
    dst: ByteArray,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    getBytes(index, dst, dstIndex, len)
    for (i in 0 until len) {
        dst[dstIndex + i] = (-dst[dstIndex + i].toInt()).toByte()
    }
    dst.reverse(dstIndex, dstIndex + len)
    return this
}

public fun ByteBuf.getBytesReverseC(index: Int, dst: ByteBuf): ByteBuf {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    getBytesReverseC(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.getBytesReverseC(
    index: Int,
    dst: ByteBuf,
    len: Int,
): ByteBuf {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    getBytesReverseC(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.getBytesReverseC(
    index: Int,
    dst: ByteBuf,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    for (i in 0 until len) {
        dst.setByte(dstIndex + i, getByteC(index + len - i - 1).toInt())
    }
    return this
}

public fun ByteBuf.readBytesReverseC(dst: ByteArray): ByteBuf {
    readBytesReverseC(dst, 0, dst.size)
    return this
}

public fun ByteBuf.readBytesReverseC(
    dst: ByteArray,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    val index = readerIndex()
    getBytesReverseC(index, dst, dstIndex, len)
    readerIndex(index + len)
    return this
}

public fun ByteBuf.readBytesReverseC(dst: ByteBuf): ByteBuf {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    readBytesReverseC(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.readBytesReverseC(dst: ByteBuf, len: Int): ByteBuf {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    readBytesReverseC(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.readBytesReverseC(
    dst: ByteBuf,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    val index = readerIndex()
    getBytesReverseC(index, dst, dstIndex, len)
    readerIndex(index + len)
    return this
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

public fun ByteBuf.setBytesReverseC(index: Int, src: ByteArray): ByteBuf {
    setBytesReverseC(index, src, 0, src.size)
    return this
}

public fun ByteBuf.setBytesReverseC(
    index: Int,
    src: ByteArray,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    Unpooled.wrappedBuffer(src).use { buf ->
        setBytesReverseC(index, buf, srcIndex, len)
    }
    return this
}

public fun ByteBuf.setBytesReverseC(index: Int, src: ByteBuf): ByteBuf {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    setBytesReverseC(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.setBytesReverseC(
    index: Int,
    src: ByteBuf,
    len: Int,
): ByteBuf {
    val srcIndex = src.readerIndex()
    setBytesReverseC(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.setBytesReverseC(
    index: Int,
    src: ByteBuf,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    for (i in 0 until len) {
        setByteC(index + i, src.getByte(srcIndex + len - i - 1).toInt())
    }
    return this
}

public fun ByteBuf.writeBytesReverseC(src: ByteArray): ByteBuf {
    writeBytesReverseC(src, 0, src.size)
    return this
}

public fun ByteBuf.writeBytesReverseC(
    src: ByteArray,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    val index = writerIndex()
    ensureWritable(len)
    setBytesReverseC(index, src, srcIndex, len)
    writerIndex(index + len)
    return this
}

public fun ByteBuf.writeBytesReverseC(src: ByteBuf): ByteBuf {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    writeBytesReverseC(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.writeBytesReverseC(src: ByteBuf, len: Int): ByteBuf {
    val srcIndex = src.readerIndex()
    writeBytesReverseC(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.writeBytesReverseC(
    src: ByteBuf,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    val index = writerIndex()
    ensureWritable(len)
    setBytesReverseC(index, src, srcIndex, len)
    writerIndex(index + len)
    return this
}

public fun ByteBuf.getBytesReverseS(index: Int, dst: ByteArray): ByteBuf {
    getBytesReverseS(index, dst, 0, dst.size)
    return this
}

public fun ByteBuf.getBytesReverseS(
    index: Int,
    dst: ByteArray,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    getBytes(index, dst, dstIndex, len)
    for (i in 0 until len) {
        dst[dstIndex + i] = (128 - dst[dstIndex + i].toInt()).toByte()
    }
    dst.reverse(dstIndex, dstIndex + len)
    return this
}

public fun ByteBuf.getBytesReverseS(index: Int, dst: ByteBuf): ByteBuf {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    getBytesReverseS(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.getBytesReverseS(
    index: Int,
    dst: ByteBuf,
    len: Int,
): ByteBuf {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    getBytesReverseS(index, dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.getBytesReverseS(
    index: Int,
    dst: ByteBuf,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    for (i in 0 until len) {
        dst.setByte(dstIndex + i, getByteS(index + len - i - 1).toInt())
    }
    return this
}

public fun ByteBuf.readBytesReverseS(dst: ByteArray): ByteBuf {
    readBytesReverseS(dst, 0, dst.size)
    return this
}

public fun ByteBuf.readBytesReverseS(
    dst: ByteArray,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    val index = readerIndex()
    getBytesReverseS(index, dst, dstIndex, len)
    readerIndex(index + len)
    return this
}

public fun ByteBuf.readBytesReverseS(dst: ByteBuf): ByteBuf {
    val dstIndex = dst.writerIndex()
    val len = dst.writableBytes()
    dst.ensureWritable(len)
    readBytesReverseS(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.readBytesReverseS(dst: ByteBuf, len: Int): ByteBuf {
    val dstIndex = dst.writerIndex()
    dst.ensureWritable(len)
    readBytesReverseS(dst, dstIndex, len)
    dst.writerIndex(dstIndex + len)
    return this
}

public fun ByteBuf.readBytesReverseS(
    dst: ByteBuf,
    dstIndex: Int,
    len: Int,
): ByteBuf {
    val index = readerIndex()
    getBytesReverseS(index, dst, dstIndex, len)
    readerIndex(index + len)
    return this
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

public fun ByteBuf.setBytesReverseS(index: Int, src: ByteArray): ByteBuf {
    setBytesReverseS(index, src, 0, src.size)
    return this
}

public fun ByteBuf.setBytesReverseS(
    index: Int,
    src: ByteArray,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    Unpooled.wrappedBuffer(src).use { buf ->
        setBytesReverseS(index, buf, srcIndex, len)
    }
    return this
}

public fun ByteBuf.setBytesReverseS(index: Int, src: ByteBuf): ByteBuf {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    setBytesReverseS(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.setBytesReverseS(
    index: Int,
    src: ByteBuf,
    len: Int,
): ByteBuf {
    val srcIndex = src.readerIndex()
    setBytesReverseS(index, src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.setBytesReverseS(
    index: Int,
    src: ByteBuf,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    for (i in 0 until len) {
        setByteS(index + i, src.getByte(srcIndex + len - i - 1).toInt())
    }
    return this
}

public fun ByteBuf.writeBytesReverseS(src: ByteArray): ByteBuf {
    writeBytesReverseS(src, 0, src.size)
    return this
}

public fun ByteBuf.writeBytesReverseS(
    src: ByteArray,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    val index = writerIndex()
    ensureWritable(len)
    setBytesReverseS(index, src, srcIndex, len)
    writerIndex(index + len)
    return this
}

public fun ByteBuf.writeBytesReverseS(src: ByteBuf): ByteBuf {
    val srcIndex = src.readerIndex()
    val len = src.readableBytes()
    writeBytesReverseS(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.writeBytesReverseS(src: ByteBuf, len: Int): ByteBuf {
    val srcIndex = src.readerIndex()
    writeBytesReverseS(src, srcIndex, len)
    src.readerIndex(srcIndex + len)
    return this
}

public fun ByteBuf.writeBytesReverseS(
    src: ByteBuf,
    srcIndex: Int,
    len: Int,
): ByteBuf {
    val index = writerIndex()
    ensureWritable(len)
    setBytesReverseS(index, src, srcIndex, len)
    writerIndex(index + len)
    return this
}

public fun ByteBuf.getBooleanA(index: Int): Boolean = getByteA(index).toInt() != 0

public fun ByteBuf.readBooleanA(): Boolean = readByteA().toInt() != 0

public fun ByteBuf.setBooleanA(index: Int, `value`: Boolean): ByteBuf {
    if (value) {
        setByteA(index, 1)
    } else {
        setByteA(index, 0)
    }
    return this
}

public fun ByteBuf.writeBooleanA(`value`: Boolean): ByteBuf {
    if (value) {
        writeByteA(1)
    } else {
        writeByteA(0)
    }
    return this
}

public fun ByteBuf.getBooleanC(index: Int): Boolean = getByteC(index).toInt() != 0

public fun ByteBuf.readBooleanC(): Boolean = readByteC().toInt() != 0

public fun ByteBuf.setBooleanC(index: Int, `value`: Boolean): ByteBuf {
    if (value) {
        setByteC(index, 1)
    } else {
        setByteC(index, 0)
    }
    return this
}

public fun ByteBuf.writeBooleanC(`value`: Boolean): ByteBuf {
    if (value) {
        writeByteC(1)
    } else {
        writeByteC(0)
    }
    return this
}

public fun ByteBuf.getBooleanS(index: Int): Boolean = getByteS(index).toInt() != 0

public fun ByteBuf.readBooleanS(): Boolean = readByteS().toInt() != 0

public fun ByteBuf.setBooleanS(index: Int, `value`: Boolean): ByteBuf {
    if (value) {
        setByteS(index, 1)
    } else {
        setByteS(index, 0)
    }
    return this
}

public fun ByteBuf.writeBooleanS(`value`: Boolean): ByteBuf {
    if (value) {
        writeByteS(1)
    } else {
        writeByteS(0)
    }
    return this
}
