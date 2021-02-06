package org.openrs2.cache

import io.netty.buffer.ByteBuf
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap
import org.openrs2.buffer.use

public object Group {
    public fun unpack(input: ByteBuf, group: Js5Index.Group): Int2ObjectSortedMap<ByteBuf> {
        require(group.size >= 1)

        val singleEntry = group.singleOrNull()
        if (singleEntry != null) {
            val files = Int2ObjectAVLTreeMap<ByteBuf>()
            files[singleEntry.id] = input.retain()
            return files
        }

        require(input.isReadable)

        val stripes = input.getUnsignedByte(input.writerIndex() - 1)

        var dataIndex = input.readerIndex()
        val trailerIndex = input.writerIndex() - (stripes * group.size * 4) - 1
        require(trailerIndex >= dataIndex)

        input.readerIndex(trailerIndex)

        val lens = IntArray(group.size)
        for (i in 0 until stripes) {
            var prevLen = 0
            for (j in lens.indices) {
                prevLen += input.readInt()
                lens[j] += prevLen
            }
        }

        input.readerIndex(trailerIndex)

        val files = Int2ObjectAVLTreeMap<ByteBuf>()
        try {
            for ((i, entry) in group.withIndex()) {
                files[entry.id] = input.alloc().buffer(lens[i], lens[i])
            }

            for (i in 0 until stripes) {
                var prevLen = 0
                for (entry in group) {
                    prevLen += input.readInt()
                    input.getBytes(dataIndex, files[entry.id], prevLen)
                    dataIndex += prevLen
                }
            }

            check(dataIndex == trailerIndex)

            // consume stripes byte too
            input.skipBytes(1)
            check(!input.isReadable)

            files.values.forEach(ByteBuf::retain)
            return files
        } finally {
            files.values.forEach(ByteBuf::release)
        }
    }

    /*
     * TODO(gpe): support multiple stripes (tricky, as the best sizes are
     * probably specific to the format we're packing...)
     */
    public fun pack(files: Int2ObjectSortedMap<ByteBuf>): ByteBuf {
        require(files.isNotEmpty())

        val first = files.values.first()
        if (files.size == 1) {
            return first.retain()
        }

        first.alloc().buffer().use { output ->
            if (files.values.all { !it.isReadable }) {
                output.writeByte(0)
                return output.retain()
            }

            for (file in files.values) {
                output.writeBytes(file, file.readerIndex(), file.readableBytes())
            }

            var prevLen = 0
            for (file in files.values) {
                val len = file.readableBytes()
                output.writeInt(len - prevLen)
                prevLen = len
            }

            output.writeByte(1)

            return output.retain()
        }
    }
}
