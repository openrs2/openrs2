package org.openrs2.crypto

import io.netty.buffer.ByteBuf

private const val GOLDEN_RATIO = 0x9E3779B9.toInt()
private const val ROUNDS = 32
public const val XTEA_BLOCK_SIZE: Int = 8
private const val BLOCK_SIZE_MASK = XTEA_BLOCK_SIZE - 1

public fun ByteBuf.xteaEncrypt(index: Int, length: Int, key: XteaKey) {
    val k = key.toIntArray()

    val end = index + (length and BLOCK_SIZE_MASK.inv())
    for (i in index until end step XTEA_BLOCK_SIZE) {
        var sum = 0
        var v0 = getInt(i)
        var v1 = getInt(i + 4)

        for (j in 0 until ROUNDS) {
            v0 += (((v1 shl 4) xor (v1 ushr 5)) + v1) xor (sum + k[sum and 3])
            sum += GOLDEN_RATIO
            v1 += (((v0 shl 4) xor (v0 ushr 5)) + v0) xor (sum + k[(sum ushr 11) and 3])
        }

        setInt(i, v0)
        setInt(i + 4, v1)
    }
}

public fun ByteBuf.xteaDecrypt(index: Int, length: Int, key: XteaKey) {
    val k = key.toIntArray()

    val end = index + (length and BLOCK_SIZE_MASK.inv())
    for (i in index until end step XTEA_BLOCK_SIZE) {
        @Suppress("INTEGER_OVERFLOW")
        var sum = GOLDEN_RATIO * ROUNDS
        var v0 = getInt(i)
        var v1 = getInt(i + 4)

        for (j in 0 until ROUNDS) {
            v1 -= (((v0 shl 4) xor (v0 ushr 5)) + v0) xor (sum + k[(sum ushr 11) and 3])
            sum -= GOLDEN_RATIO
            v0 -= (((v1 shl 4) xor (v1 ushr 5)) + v1) xor (sum + k[sum and 3])
        }

        setInt(i, v0)
        setInt(i + 4, v1)
    }
}
