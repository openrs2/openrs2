package org.openrs2.cache

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import org.openrs2.buffer.readUnsignedIntSmart
import org.openrs2.buffer.writeUnsignedIntSmart
import org.openrs2.crypto.Whirlpool

public class Js5Index(
    public var protocol: Js5Protocol,
    public var version: Int = 0,
    public var hasNames: Boolean = false,
    public var hasDigests: Boolean = false,
    public var hasLengths: Boolean = false,
    public var hasUncompressedChecksums: Boolean = false
) : NamedEntryCollection<Js5Index.Group>(::Group) {
    public class Group internal constructor(
        parent: NamedEntryCollection<Group>,
        override val id: Int
    ) : NamedEntryCollection<File>(::File), NamedEntry {
        private var parent: NamedEntryCollection<Group>? = parent
        public var version: Int = 0
        public var checksum: Int = 0
        public var uncompressedChecksum: Int = 0
        public var length: Int = 0
        public var uncompressedLength: Int = 0

        public override var nameHash: Int = -1
            set(value) {
                parent?.rename(id, field, value)
                field = value
            }

        public var digest: ByteArray? = null
            set(value) {
                require(value == null || value.size == Whirlpool.DIGESTBYTES)
                field = value
            }

        override fun remove() {
            parent?.remove(this)
            parent = null
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false

            other as Group

            if (id != other.id) return false
            if (version != other.version) return false
            if (checksum != other.checksum) return false
            if (uncompressedChecksum != other.uncompressedChecksum) return false
            if (length != other.length) return false
            if (uncompressedLength != other.uncompressedLength) return false
            if (nameHash != other.nameHash) return false
            if (digest != null) {
                if (other.digest == null) return false
                if (!digest.contentEquals(other.digest)) return false
            } else if (other.digest != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + id
            result = 31 * result + version
            result = 31 * result + checksum
            result = 31 * result + uncompressedChecksum
            result = 31 * result + length
            result = 31 * result + uncompressedLength
            result = 31 * result + nameHash
            result = 31 * result + (digest?.contentHashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            val digest = digest
            val hex = if (digest != null) {
                ByteBufUtil.hexDump(digest)
            } else {
                "null"
            }
            return "Group{id=$id, nameHash=$nameHash, version=$version, checksum=$checksum, " +
                "uncompressedChecksum=$uncompressedChecksum, length=$length, uncompressedLength=$uncompressedLength, " +
                "digest=$hex, size=$size, capacity=$capacity}"
        }
    }

    public class File internal constructor(
        parent: NamedEntryCollection<File>,
        override val id: Int,
    ) : NamedEntry {
        private var parent: NamedEntryCollection<File>? = parent

        public override var nameHash: Int = -1
            set(value) {
                parent?.rename(id, field, value)
                field = value
            }

        override fun remove() {
            parent?.remove(this)
            parent = null
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as File

            if (id != other.id) return false
            if (nameHash != other.nameHash) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id
            result = 31 * result + nameHash
            return result
        }

        override fun toString(): String {
            return "File{id=$id, nameHash=$nameHash}"
        }
    }

    public fun write(buf: ByteBuf) {
        val writeFunc = if (protocol >= Js5Protocol.SMART) {
            buf::writeUnsignedIntSmart
        } else {
            { value ->
                check(value in 0..65535) {
                    "$value outside of valid non-SMART range"
                }
                buf.writeShort(value)
            }
        }

        buf.writeByte(protocol.id)

        if (protocol >= Js5Protocol.VERSIONED) {
            buf.writeInt(version)
        }

        var flags = 0
        if (hasNames) {
            flags = flags or FLAG_NAMES
        }
        if (hasDigests) {
            flags = flags or FLAG_DIGESTS
        }
        if (hasLengths) {
            flags = flags or FLAG_LENGTHS
        }
        if (hasUncompressedChecksums) {
            flags = flags or FLAG_UNCOMPRESSED_CHECKSUMS
        }
        buf.writeByte(flags)

        writeFunc(size)

        var prevGroupId = 0
        for (group in this) {
            writeFunc(group.id - prevGroupId)
            prevGroupId = group.id
        }

        if (hasNames) {
            for (group in this) {
                buf.writeInt(group.nameHash)
            }
        }

        for (group in this) {
            buf.writeInt(group.checksum)
        }

        if (hasUncompressedChecksums) {
            for (group in this) {
                buf.writeInt(group.uncompressedChecksum)
            }
        }

        if (hasDigests) {
            for (group in this) {
                val digest = group.digest
                if (digest != null) {
                    buf.writeBytes(digest)
                } else {
                    buf.writeZero(Whirlpool.DIGESTBYTES)
                }
            }
        }

        if (hasLengths) {
            for (group in this) {
                buf.writeInt(group.length)
                buf.writeInt(group.uncompressedLength)
            }
        }

        for (group in this) {
            buf.writeInt(group.version)
        }

        for (group in this) {
            writeFunc(group.size)
        }

        for (group in this) {
            var prevFileId = 0
            for (file in group) {
                writeFunc(file.id - prevFileId)
                prevFileId = file.id
            }
        }

        if (hasNames) {
            for (group in this) {
                for (file in group) {
                    buf.writeInt(file.nameHash)
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as Js5Index

        if (protocol != other.protocol) return false
        if (version != other.version) return false
        if (hasNames != other.hasNames) return false
        if (hasDigests != other.hasDigests) return false
        if (hasLengths != other.hasLengths) return false
        if (hasUncompressedChecksums != other.hasUncompressedChecksums) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + protocol.hashCode()
        result = 31 * result + version
        result = 31 * result + hasNames.hashCode()
        result = 31 * result + hasDigests.hashCode()
        result = 31 * result + hasLengths.hashCode()
        result = 31 * result + hasUncompressedChecksums.hashCode()
        return result
    }

    override fun toString(): String {
        return "Js5Index{protocol=$protocol, version=$version, hasNames=$hasNames, hasDigests=$hasDigests, " +
            "hasLengths=$hasLengths, hasUncompressedChecksums=$hasUncompressedChecksums, size=$size, " +
            "capacity=$capacity}"
    }

    public companion object {
        private const val FLAG_NAMES = 0x01
        private const val FLAG_DIGESTS = 0x02
        private const val FLAG_LENGTHS = 0x04
        private const val FLAG_UNCOMPRESSED_CHECKSUMS = 0x08

        public fun read(buf: ByteBuf): Js5Index {
            val number = buf.readUnsignedByte().toInt()
            val protocol = Js5Protocol.fromId(number)
            require(protocol != null) {
                "Unsupported JS5 protocol number: $number"
            }

            val readFunc = if (protocol >= Js5Protocol.SMART) {
                buf::readUnsignedIntSmart
            } else {
                buf::readUnsignedShort
            }

            val version = if (protocol >= Js5Protocol.VERSIONED) {
                buf.readInt()
            } else {
                0
            }
            val flags = buf.readUnsignedByte().toInt()
            val size = readFunc()

            val index = Js5Index(
                protocol,
                version,
                hasNames = flags and FLAG_NAMES != 0,
                hasDigests = flags and FLAG_DIGESTS != 0,
                hasLengths = flags and FLAG_LENGTHS != 0,
                hasUncompressedChecksums = flags and FLAG_UNCOMPRESSED_CHECKSUMS != 0
            )

            var prevGroupId = 0
            for (i in 0 until size) {
                prevGroupId += readFunc()
                index.createOrGet(prevGroupId)
            }

            if (index.hasNames) {
                for (group in index) {
                    group.nameHash = buf.readInt()
                }
            }

            for (group in index) {
                group.checksum = buf.readInt()
            }

            if (index.hasUncompressedChecksums) {
                for (group in index) {
                    group.uncompressedChecksum = buf.readInt()
                }
            }

            if (index.hasDigests) {
                for (group in index) {
                    val digest = ByteArray(Whirlpool.DIGESTBYTES)
                    buf.readBytes(digest)
                    group.digest = digest
                }
            }

            if (index.hasLengths) {
                for (group in index) {
                    group.length = buf.readInt()
                    group.uncompressedLength = buf.readInt()
                }
            }

            for (group in index) {
                group.version = buf.readInt()
            }

            val groupSizes = IntArray(size) {
                readFunc()
            }

            for ((i, group) in index.withIndex()) {
                val groupSize = groupSizes[i]

                var prevFileId = 0
                for (j in 0 until groupSize) {
                    prevFileId += readFunc()
                    group.createOrGet(prevFileId)
                }
            }

            if (index.hasNames) {
                for (group in index) {
                    for (file in group) {
                        file.nameHash = buf.readInt()
                    }
                }
            }

            return index
        }
    }
}
