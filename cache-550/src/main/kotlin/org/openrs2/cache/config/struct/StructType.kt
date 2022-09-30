package org.openrs2.cache.config.struct

import io.netty.buffer.ByteBuf
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.openrs2.buffer.readString
import org.openrs2.buffer.writeString
import org.openrs2.cache.config.ConfigType

public class StructType(id: Int) : ConfigType(id) {
    // TODO(gpe): methods for manipulating the map
    private val params: Int2ObjectMap<Any> = Int2ObjectOpenHashMap()

    override fun read(buf: ByteBuf, code: Int) {
        when (code) {
            249 -> {
                val size = buf.readUnsignedByte().toInt()
                for (i in 0 until size) {
                    val string = buf.readBoolean()
                    val id = buf.readUnsignedMedium()

                    if (string) {
                        params[id] = buf.readString()
                    } else {
                        params[id] = buf.readInt()
                    }
                }
            }

            else -> throw IllegalArgumentException("Unsupported config code: $code")
        }
    }

    override fun write(buf: ByteBuf) {
        if (params.isNotEmpty()) {
            buf.writeByte(249)
            buf.writeByte(params.size)

            for ((id, value) in params.int2ObjectEntrySet()) {
                when (value) {
                    is String -> {
                        buf.writeBoolean(true)
                        buf.writeMedium(id)
                        buf.writeString(value)
                    }

                    is Int -> {
                        buf.writeBoolean(false)
                        buf.writeMedium(id)
                        buf.writeInt(value)
                    }

                    else -> throw IllegalStateException()
                }
            }
        }

        buf.writeByte(0)
    }
}
