package org.openrs2.util.charset

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CharsetEncoder
import java.nio.charset.CoderResult

public object ModifiedUtf8Charset : Charset("ModifiedUtf8", null) {
    override fun contains(cs: Charset): Boolean {
        return Charsets.UTF_8.contains(cs) || cs is Cp1252Charset || cs is ModifiedUtf8Charset
    }

    override fun newEncoder(): CharsetEncoder {
        return object : CharsetEncoder(this, 1F, 3F) {
            override fun encodeLoop(input: CharBuffer, output: ByteBuffer): CoderResult {
                while (input.hasRemaining()) {
                    val char = input.get()

                    val len = if (char != '\u0000' && char < '\u0080') {
                        1
                    } else if (char < '\u0800') {
                        2
                    } else {
                        3
                    }

                    if (output.remaining() < len) {
                        input.position(input.position() - 1)
                        return CoderResult.OVERFLOW
                    }

                    when (len) {
                        1 -> output.put(char.code.toByte())
                        2 -> {
                            output.put((0xC0 or ((char.code shr 6) and 0x1F)).toByte())
                            output.put((0x80 or (char.code and 0x3F)).toByte())
                        }

                        else -> {
                            output.put((0xE0 or ((char.code shr 12) and 0x1F)).toByte())
                            output.put((0x80 or ((char.code shr 6) and 0x1F)).toByte())
                            output.put((0x80 or (char.code and 0x3F)).toByte())
                        }
                    }
                }

                return CoderResult.UNDERFLOW
            }
        }
    }

    override fun newDecoder(): CharsetDecoder {
        return object : CharsetDecoder(this, 1F, 1F) {
            override fun decodeLoop(input: ByteBuffer, output: CharBuffer): CoderResult {
                while (input.hasRemaining()) {
                    if (!output.hasRemaining()) {
                        return CoderResult.OVERFLOW
                    }

                    val a = input.get().toInt() and 0xFF
                    if (a != 0 && a < 0x80) {
                        output.put(a.toChar())
                    } else if ((a and 0xE0) == 0xC0) {
                        if (!input.hasRemaining()) {
                            input.position(input.position() - 1)
                            return CoderResult.UNDERFLOW
                        }

                        val b = input.get().toInt() and 0xFF
                        if ((b and 0xC0) != 0x80) {
                            input.position(input.position() - 2)
                            return CoderResult.malformedForLength(2)
                        }

                        output.put((((a and 0x1F) shl 6) or (b and 0x3F)).toChar())
                    } else if ((a and 0xF0) == 0xE0) {
                        if (input.remaining() < 2) {
                            input.position(input.position() - 1)
                            return CoderResult.UNDERFLOW
                        }

                        val b = input.get().toInt() and 0xFF
                        val c = input.get().toInt() and 0xFF
                        if ((b and 0xC0) != 0x80 || (c and 0xC0) != 0x80) {
                            input.position(input.position() - 3)
                            return CoderResult.malformedForLength(3)
                        }

                        output.put((((a and 0x0F) shl 12) or ((b and 0x3F) shl 6) or (c and 0x3F)).toChar())
                    } else {
                        input.position(input.position() - 1)
                        return CoderResult.malformedForLength(1)
                    }
                }

                return CoderResult.UNDERFLOW
            }
        }
    }
}
