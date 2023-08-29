package org.openrs2.crypto

import java.security.SecureRandom

public data class SymmetricKey(
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
        public val ZERO: SymmetricKey = SymmetricKey(0, 0, 0, 0)

        @JvmStatic
        @JvmOverloads
        public fun generate(r: SecureRandom = secureRandom): SymmetricKey {
            return SymmetricKey(r.nextInt(), r.nextInt(), r.nextInt(), r.nextInt())
        }

        @JvmStatic
        public fun fromIntArray(a: IntArray): SymmetricKey {
            require(a.size == 4)

            return SymmetricKey(a[0], a[1], a[2], a[3])
        }

        @JvmStatic
        public fun fromHex(s: String): SymmetricKey {
            return fromHexOrNull(s) ?: throw IllegalArgumentException()
        }

        @JvmStatic
        public fun fromHexOrNull(s: String): SymmetricKey? {
            if (s.length != 32) {
                return null
            }

            return try {
                val k0 = Integer.parseUnsignedInt(s, 0, 8, 16)
                val k1 = Integer.parseUnsignedInt(s, 8, 16, 16)
                val k2 = Integer.parseUnsignedInt(s, 16, 24, 16)
                val k3 = Integer.parseUnsignedInt(s, 24, 32, 16)

                SymmetricKey(k0, k1, k2, k3)
            } catch (ex: NumberFormatException) {
                null
            }
        }
    }
}
