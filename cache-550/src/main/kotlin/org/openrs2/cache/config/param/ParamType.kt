package org.openrs2.cache.config.param

import io.netty.buffer.ByteBuf
import org.openrs2.buffer.readString
import org.openrs2.buffer.writeString
import org.openrs2.cache.config.ConfigType
import org.openrs2.util.charset.Cp1252Charset

public class ParamType(id: Int) : ConfigType(id) {
    public var type: Char = 0.toChar()
    public var defaultInt: Int = 0
    public var defaultString: String? = null

    override fun read(buf: ByteBuf, code: Int) {
        when (code) {
            1 -> type = Cp1252Charset.decode(buf.readByte())
            2 -> defaultInt = buf.readInt()
            5 -> defaultString = buf.readString()
            else -> throw IllegalArgumentException("Unsupported config code: $code")
        }
    }

    override fun write(buf: ByteBuf) {
        if (type.code != 0) {
            buf.writeByte(1)
            buf.writeByte(Cp1252Charset.encode(type).toInt())
        }

        if (defaultInt != 0) {
            buf.writeByte(2)
            buf.writeInt(defaultInt)
        }

        val defaultString = defaultString
        if (defaultString != null) {
            buf.writeByte(5)
            buf.writeString(defaultString)
        }

        buf.writeByte(0)
    }
}
