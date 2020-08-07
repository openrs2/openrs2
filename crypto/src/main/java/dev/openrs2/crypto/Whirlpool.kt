package dev.openrs2.crypto

class Whirlpool {
    private val bitLength = ByteArray(32)
    private val buffer = ByteArray(64)
    private var bufferBits = 0
    private var bufferPos = 0
    private val hash = LongArray(8)
    private val K = LongArray(8)
    private val L = LongArray(8)
    private val block = LongArray(8)
    private val state = LongArray(8)

    private fun processBuffer() {
        var j = 0
        for (i in 0 until 8) {
            block[i] = (buffer[j].toLong() shl 56) xor
                ((buffer[j + 1].toLong() and 0xFFL) shl 48) xor
                ((buffer[j + 2].toLong() and 0xFFL) shl 40) xor
                ((buffer[j + 3].toLong() and 0xFFL) shl 32) xor
                ((buffer[j + 4].toLong() and 0xFFL) shl 24) xor
                ((buffer[j + 5].toLong() and 0xFFL) shl 16) xor
                ((buffer[j + 6].toLong() and 0xFFL) shl 8) xor
                (buffer[j + 7].toLong() and 0xFFL)
            j += 8
        }

        for (i in 0 until 8) {
            K[i] = hash[i]
            state[i] = block[i] xor K[i]
        }

        for (r in 1..R) {
            for (i in 0 until 8) {
                L[i] = 0

                var s = 56
                for (t in 0 until 8) {
                    L[i] = L[i] xor C[t][(K[(i - t) and 7] ushr s).toInt() and 0xFF]
                    s -= 8
                }
            }

            for (i in 0 until 8) {
                K[i] = L[i]
            }

            K[0] = K[0] xor rc[r]

            for (i in 0 until 8) {
                L[i] = K[i]

                var s = 56
                for (t in 0 until 8) {
                    L[i] = L[i] xor C[t][(state[(i - t) and 7] ushr s).toInt() and 0xFF]
                    s -= 8
                }
            }

            for (i in 0 until 8) {
                state[i] = L[i]
            }
        }

        for (i in 0 until 8) {
            hash[i] = hash[i] xor state[i] xor block[i]
        }
    }

    fun NESSIEinit() {
        bitLength.fill(0)
        bufferBits = 0
        bufferPos = 0
        buffer[0] = 0
        hash.fill(0)
    }

    fun NESSIEadd(source: ByteArray, bits: Long) {
        var sourceBits = bits
        var sourcePos = 0
        val sourceGap = (8 - (sourceBits.toInt() and 7)) and 7
        val bufferRem = bufferBits and 7
        var b: Int

        var value = sourceBits
        var carry = 0
        for (i in 31 downTo 0) {
            carry += (bitLength[i].toInt() and 0xFF) + (value.toInt() and 0xFF)
            bitLength[i] = carry.toByte()
            carry = carry ushr 8
            value = value ushr 8
        }

        while (sourceBits > 8) {
            b = ((source[sourcePos].toInt() shl sourceGap) and 0xFF) or
                ((source[sourcePos + 1].toInt() and 0xFF) ushr (8 - sourceGap))
            check(b in 0..255)

            buffer[bufferPos] = (buffer[bufferPos].toInt() or (b ushr bufferRem)).toByte()
            bufferPos++
            bufferBits += 8 - bufferRem

            if (bufferBits == 512) {
                processBuffer()
                bufferBits = 0
                bufferPos = 0
            }

            buffer[bufferPos] = ((b shl (8 - bufferRem)) and 0xFF).toByte()
            bufferBits += bufferRem

            sourceBits -= 8
            sourcePos++
        }

        if (sourceBits > 0) {
            b = (source[sourcePos].toInt() shl sourceGap) and 0xFF
            buffer[bufferPos] = (buffer[bufferPos].toInt() or (b ushr bufferRem)).toByte()
        } else {
            b = 0
        }

        if (bufferRem + sourceBits < 8) {
            bufferBits += sourceBits.toInt()
        } else {
            bufferPos++
            bufferBits += 8 - bufferRem
            sourceBits -= 8 - bufferRem

            if (bufferBits == 512) {
                processBuffer()
                bufferBits = 0
                bufferPos = 0
            }

            buffer[bufferPos] = ((b shl (8 - bufferRem)) and 0xFF).toByte()
            bufferBits += sourceBits.toInt()
        }
    }

    fun NESSIEfinalize(digest: ByteArray) {
        buffer[bufferPos] = (buffer[bufferPos].toInt() or (0x80 ushr (bufferBits and 7))).toByte()
        bufferPos++

        if (bufferPos > 32) {
            while (bufferPos < 64) {
                buffer[bufferPos++] = 0
            }

            processBuffer()
            bufferPos = 0
        }

        while (bufferPos < 32) {
            buffer[bufferPos++] = 0
        }

        bitLength.copyInto(buffer, destinationOffset = 32, startIndex = 0, endIndex = 32)
        processBuffer()

        var j = 0
        for (i in 0 until 8) {
            val h = hash[i]
            digest[j] = (h ushr 56).toByte()
            digest[j + 1] = (h ushr 48).toByte()
            digest[j + 2] = (h ushr 40).toByte()
            digest[j + 3] = (h ushr 32).toByte()
            digest[j + 4] = (h ushr 24).toByte()
            digest[j + 5] = (h ushr 16).toByte()
            digest[j + 6] = (h ushr 8).toByte()
            digest[j + 7] = h.toByte()
            j += 8
        }
    }

    companion object {
        private const val DIGESTBITS = 512
        private const val DIGESTBYTES = DIGESTBITS ushr 3
        private const val R = 10
        private const val sbox = "\u1823\uc6E8\u87B8\u014F\u36A6\ud2F5\u796F\u9152" +
            "\u60Bc\u9B8E\uA30c\u7B35\u1dE0\ud7c2\u2E4B\uFE57" +
            "\u1577\u37E5\u9FF0\u4AdA\u58c9\u290A\uB1A0\u6B85" +
            "\uBd5d\u10F4\ucB3E\u0567\uE427\u418B\uA77d\u95d8" +
            "\uFBEE\u7c66\udd17\u479E\ucA2d\uBF07\uAd5A\u8333" +
            "\u6302\uAA71\uc819\u49d9\uF2E3\u5B88\u9A26\u32B0" +
            "\uE90F\ud580\uBEcd\u3448\uFF7A\u905F\u2068\u1AAE" +
            "\uB454\u9322\u64F1\u7312\u4008\uc3Ec\udBA1\u8d3d" +
            "\u9700\ucF2B\u7682\ud61B\uB5AF\u6A50\u45F3\u30EF" +
            "\u3F55\uA2EA\u65BA\u2Fc0\udE1c\uFd4d\u9275\u068A" +
            "\uB2E6\u0E1F\u62d4\uA896\uF9c5\u2559\u8472\u394c" +
            "\u5E78\u388c\ud1A5\uE261\uB321\u9c1E\u43c7\uFc04" +
            "\u5199\u6d0d\uFAdF\u7E24\u3BAB\ucE11\u8F4E\uB7EB" +
            "\u3c81\u94F7\uB913\u2cd3\uE76E\uc403\u5644\u7FA9" +
            "\u2ABB\uc153\udc0B\u9d6c\u3174\uF646\uAc89\u14E1" +
            "\u163A\u6909\u70B6\ud0Ed\ucc42\u98A4\u285c\uF886"
        private val C = Array(8) { LongArray(256) }
        private val rc = LongArray(R + 1)

        init {
            for (x in 0 until 256) {
                val c = sbox[x / 2]

                val v1 = if ((x and 1) == 0) {
                    c.toLong() ushr 8
                } else {
                    c.toLong() and 0xFF
                }

                var v2 = v1 shl 1
                if (v2 >= 0x100) {
                    v2 = v2 xor 0x11D
                }

                var v4 = v2 shl 1
                if (v4 >= 0x100) {
                    v4 = v4 xor 0x11D
                }

                val v5 = v4 xor v1

                var v8 = v4 shl 1
                if (v8 >= 0x100) {
                    v8 = v8 xor 0x11D
                }

                val v9 = v8 xor v1

                C[0][x] = (v1 shl 56) or (v1 shl 48) or (v4 shl 40) or (v1 shl 32) or
                    (v8 shl 24) or (v5 shl 16) or (v2 shl 8) or v9

                for (t in 1 until 8) {
                    C[t][x] = (C[t - 1][x] ushr 8) or (C[t - 1][x] shl 56)
                }
            }

            rc[0] = 0

            for (r in 1..R) {
                val i = 8 * (r - 1)
                rc[r] = (C[0][i] and -0x100000000000000L) xor
                    (C[1][i + 1] and 0x00FF000000000000L) xor
                    (C[2][i + 2] and 0x0000FF0000000000L) xor
                    (C[3][i + 3] and 0x000000FF00000000L) xor
                    (C[4][i + 4] and 0x00000000FF000000L) xor
                    (C[5][i + 5] and 0x0000000000FF0000L) xor
                    (C[6][i + 6] and 0x000000000000FF00L) xor
                    (C[7][i + 7] and 0x00000000000000FFL)
            }
        }

        fun whirlpool(data: ByteArray, off: Int = 0, len: Int = data.size): ByteArray {
            val source: ByteArray
            if (off <= 0) {
                source = data
            } else {
                source = ByteArray(len)
                for (i in 0 until len) {
                    source[i] = data[off + i]
                }
            }

            val whirlpool = Whirlpool()
            whirlpool.NESSIEinit()
            whirlpool.NESSIEadd(source, (len * 8).toLong())

            val digest = ByteArray(64)
            whirlpool.NESSIEfinalize(digest)
            return digest
        }
    }
}
