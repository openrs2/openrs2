package dev.openrs2.util.charset

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CharsetEncoder
import java.nio.charset.CoderResult
import java.nio.charset.StandardCharsets

object Cp1252Charset : Charset("Cp1252", null) {
    private val ASCII_CHARSET = StandardCharsets.US_ASCII.javaClass
    private val CODE_PAGE = charArrayOf(
        '\u20AC', '\u0000', '\u201A', '\u0192', '\u201E', '\u2026', '\u2020', '\u2021',
        '\u02C6', '\u2030', '\u0160', '\u2039', '\u0152', '\u0000', '\u017D', '\u0000',
        '\u0000', '\u2018', '\u2019', '\u201C', '\u201D', '\u2022', '\u2013', '\u2014',
        '\u02DC', '\u2122', '\u0161', '\u203A', '\u0153', '\u0000', '\u017E', '\u0178'
    )
    private val ENCODE_TABLE = ByteArray(65536)
    private val DECODE_TABLE = CharArray(256)
    private const val REPLACEMENT_CHAR = '\uFFFD'
    private const val REPLACEMENT_BYTE = '?'.toByte()

    init {
        for (b in 0 until 256) {
            val c = if (b in 0x80 until 0xA0) {
                CODE_PAGE[b and 0x7F]
            } else {
                b.toChar()
            }

            if (c != '\u0000') {
                ENCODE_TABLE[c.toInt()] = b.toByte()
                DECODE_TABLE[b] = c
            }
        }
    }

    fun decode(byte: Byte): Char {
        val char = DECODE_TABLE[byte.toInt() and 0xFF]
        return if (char == '\u0000') {
            REPLACEMENT_CHAR
        } else {
            char
        }
    }

    fun encode(char: Char): Byte {
        val byte = ENCODE_TABLE[char.toInt()]
        return if (byte.toInt() == 0) {
            REPLACEMENT_BYTE
        } else {
            byte
        }
    }

    override fun contains(cs: Charset): Boolean {
        return ASCII_CHARSET.isInstance(cs) || cs is Cp1252Charset
    }

    override fun newEncoder(): CharsetEncoder {
        return object : CharsetEncoder(this, 1F, 1F) {
            init {
                replaceWith(byteArrayOf(REPLACEMENT_BYTE))
            }

            override fun encodeLoop(input: CharBuffer, output: ByteBuffer): CoderResult {
                while (input.hasRemaining()) {
                    if (!output.hasRemaining()) {
                        return CoderResult.OVERFLOW
                    }

                    val char = input.get()
                    val byte = ENCODE_TABLE[char.toInt()]

                    if (byte.toInt() == 0) {
                        input.position(input.position() - 1)
                        return CoderResult.unmappableForLength(1)
                    }

                    output.put(byte)
                }

                return CoderResult.UNDERFLOW
            }
        }
    }

    override fun newDecoder(): CharsetDecoder {
        return object : CharsetDecoder(this, 1F, 1F) {
            init {
                replaceWith(REPLACEMENT_CHAR.toString())
            }

            override fun decodeLoop(input: ByteBuffer, output: CharBuffer): CoderResult {
                while (input.hasRemaining()) {
                    if (!output.hasRemaining()) {
                        return CoderResult.OVERFLOW
                    }

                    val byte = input.get()
                    val char = DECODE_TABLE[byte.toInt() and 0xFF]

                    if (char == '\u0000') {
                        input.position(input.position() - 1)
                        return CoderResult.unmappableForLength(1)
                    }

                    output.put(char)
                }

                return CoderResult.UNDERFLOW
            }
        }
    }
}
