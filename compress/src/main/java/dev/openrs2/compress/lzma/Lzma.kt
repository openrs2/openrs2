package dev.openrs2.compress.lzma

import com.google.common.io.LittleEndianDataInputStream
import com.google.common.io.LittleEndianDataOutputStream
import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.LZMAInputStream
import org.tukaani.xz.LZMAOutputStream
import java.io.InputStream
import java.io.OutputStream

public object Lzma {
    public val BEST_SPEED: LZMA2Options = LZMA2Options(LZMA2Options.PRESET_MIN)
    public val DEFAULT_COMPRESSION: LZMA2Options = LZMA2Options(LZMA2Options.PRESET_DEFAULT)
    public val BEST_COMPRESSION: LZMA2Options = LZMA2Options(LZMA2Options.PRESET_MAX)

    public fun createHeaderlessInputStream(input: InputStream, length: Long): InputStream {
        val headerInput = LittleEndianDataInputStream(input)

        val properties = headerInput.readByte()
        val dictionarySize = headerInput.readInt()

        return LZMAInputStream(input, length, properties, dictionarySize)
    }

    public fun createHeaderlessOutputStream(output: OutputStream, options: LZMA2Options): OutputStream {
        val headerOutput = LittleEndianDataOutputStream(output)
        headerOutput.writeByte((options.pb * 5 + options.lp) * 9 + options.lc)
        headerOutput.writeInt(options.dictSize)

        return LZMAOutputStream(output, options, false)
    }
}
