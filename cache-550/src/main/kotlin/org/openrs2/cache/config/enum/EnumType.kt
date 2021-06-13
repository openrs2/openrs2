package org.openrs2.cache.config.enum

import io.netty.buffer.ByteBuf
import it.unimi.dsi.fastutil.ints.Int2IntMap
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.openrs2.buffer.readString
import org.openrs2.buffer.writeString
import org.openrs2.cache.config.ConfigType
import org.openrs2.util.charset.Cp1252Charset

public class EnumType(id: Int) : ConfigType(id) {
    public var keyType: Char = 0.toChar()
    public var valueType: Char = 0.toChar()
    public var defaultString: String = "null"
    public var defaultInt: Int = 0

    // TODO(gpe): methods for manipulating the maps
    private var strings: Int2ObjectMap<String>? = null
    private var ints: Int2IntMap? = null

    override fun read(buf: ByteBuf, code: Int) {
        when (code) {
            1 -> keyType = Cp1252Charset.decode(buf.readByte())
            2 -> valueType = Cp1252Charset.decode(buf.readByte())
            3 -> defaultString = buf.readString()
            4 -> defaultInt = buf.readInt()
            5 -> {
                val size = buf.readUnsignedShort()
                val strings = Int2ObjectOpenHashMap<String>()

                for (i in 0 until size) {
                    val key = buf.readInt()
                    strings[key] = buf.readString()
                }

                this.strings = strings
            }
            6 -> {
                val size = buf.readUnsignedShort()
                val ints = Int2IntOpenHashMap()

                for (i in 0 until size) {
                    val key = buf.readInt()
                    ints[key] = buf.readInt()
                }

                this.ints = ints
            }
            else -> throw IllegalArgumentException("Unsupported config code: $code")
        }
    }

    override fun write(buf: ByteBuf) {
        if (keyType.code != 0) {
            buf.writeByte(1)
            buf.writeByte(Cp1252Charset.encode(keyType).toInt())
        }

        if (valueType.code != 0) {
            buf.writeByte(2)
            buf.writeByte(Cp1252Charset.encode(valueType).toInt())
        }

        if (defaultString != "null") {
            buf.writeByte(3)
            buf.writeString(defaultString)
        }

        if (defaultInt != 0) {
            buf.writeByte(4)
            buf.writeInt(defaultInt)
        }

        val strings = strings
        if (strings != null && strings.isNotEmpty()) {
            buf.writeByte(5)
            buf.writeShort(strings.size)

            for ((key, value) in strings.int2ObjectEntrySet()) {
                buf.writeInt(key)
                buf.writeString(value)
            }
        }

        val ints = ints
        if (ints != null && ints.isNotEmpty()) {
            buf.writeByte(6)
            buf.writeShort(ints.size)

            for ((key, value) in ints.int2IntEntrySet()) {
                buf.writeInt(key)
                buf.writeInt(value)
            }
        }

        buf.writeByte(0)
    }

    public fun getString(key: Int): String {
        val strings = strings ?: return defaultString
        return strings.getOrDefault(key, defaultString)
    }

    public fun getInt(key: Int): Int {
        val ints = ints ?: return defaultInt
        return ints.getOrDefault(key, defaultInt)
    }
}
