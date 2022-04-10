package org.openrs2.crypto

import io.netty.buffer.ByteBuf

private const val GOLDEN_RATIO = 0x9E3779B9.toInt()
private const val ROUNDS = 32
private const val BLOCK_SIZE = 8
private const val BLOCK_SIZE_MASK = BLOCK_SIZE - 1

public data class XteaKey(
    public val k0: Int,
    public val k1: Int,
    public val k2: Int,
    public val k3: Int
) {
    public val isZero: Boolean
        get() = k0 == 0 && k1 == 0 && k2 == 0 && k3 == 0

    public fun toIntArray(): IntArray {
        return intArrayOf(k0, k1, k2, k3)
    }

    public fun toHex(): String {
        return Integer.toUnsignedString(k0, 16).padStart(8, '0') +
            Integer.toUnsignedString(k1, 16).padStart(8, '0') +
            Integer.toUnsignedString(k2, 16).padStart(8, '0') +
            Integer.toUnsignedString(k3, 16).padStart(8, '0')
    }

    override fun toString(): String {
        return toHex()
    }

    public companion object {
        @JvmStatic
        public val ZERO: XteaKey = XteaKey(0, 0, 0, 0)

        @JvmStatic
        public fun fromIntArray(a: IntArray): XteaKey {
            require(a.size == 4)

            return XteaKey(a[0], a[1], a[2], a[3])
        }

        @JvmStatic
        public fun fromHex(s: String): XteaKey {
            return fromHexOrNull(s) ?: throw IllegalArgumentException()
        }

        @JvmStatic
        public fun fromHexOrNull(s: String): XteaKey? {
            if (s.length != 32) {
                return null
            }

            return try {
                val k0 = Integer.parseUnsignedInt(s, 0, 8, 16)
                val k1 = Integer.parseUnsignedInt(s, 8, 16, 16)
                val k2 = Integer.parseUnsignedInt(s, 16, 24, 16)
                val k3 = Integer.parseUnsignedInt(s, 24, 32, 16)

                XteaKey(k0, k1, k2, k3)
            } catch (ex: NumberFormatException) {
                null
            }
        }
    }
}

public fun ByteBuf.xteaEncrypt(index: Int, length: Int, key: XteaKey) {
    val k = key.toIntArray()

    val end = index + (length and BLOCK_SIZE_MASK.inv())
    for (i in index until end step BLOCK_SIZE) {
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
    for (i in index until end step BLOCK_SIZE) {
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
