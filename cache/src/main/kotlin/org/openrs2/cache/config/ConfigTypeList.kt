package org.openrs2.cache.config

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.openrs2.buffer.use
import org.openrs2.cache.Cache

public abstract class ConfigTypeList<T : ConfigType>(
    protected val cache: Cache,
    protected val archive: Int
) {
    private val types = Int2ObjectOpenHashMap<T>()

    public val capacity: Int by lazy {
        calculateCapacity()
    }

    public operator fun get(id: Int): T? {
        var type = types[id]
        if (type != null) {
            return type
        }

        val group = getGroupId(id)
        val file = getFileId(id)
        if (!cache.exists(archive, group, file)) {
            // TODO(gpe): the client returns an empty config - should we do that?
            return null
        }

        cache.read(archive, group, file).use { buf ->
            type = allocate(id)
            type.read(buf)
            type.postRead()
            types[id] = type
            return type
        }
    }

    protected abstract fun allocate(id: Int): T
    protected abstract fun calculateCapacity(): Int
    protected abstract fun getGroupId(id: Int): Int
    protected abstract fun getFileId(id: Int): Int
}
