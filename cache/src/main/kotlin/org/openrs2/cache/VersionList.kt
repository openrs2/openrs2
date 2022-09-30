package org.openrs2.cache

import org.openrs2.buffer.use

public class VersionList(
    public val files: List<List<File>>,
    public val maps: Map<Int, MapSquare>
) {
    public data class File(
        val version: Int,
        val checksum: Int
    )

    public data class MapSquare(
        val mapFile: Int,
        val locFile: Int,
        val freeToPlay: Boolean
    )

    public companion object {
        private val NAMES = listOf("model", "anim", "midi", "map")

        @JvmStatic
        public fun read(archive: JagArchive): VersionList {
            val files = mutableListOf<List<File>>()

            for (name in NAMES) {
                val versions = archive.read("${name}_version".uppercase()).use { buf ->
                    IntArray(buf.readableBytes() / 2) {
                        buf.readUnsignedShort()
                    }
                }

                val checksums = archive.read("${name}_crc".uppercase()).use { buf ->
                    IntArray(buf.readableBytes() / 4) {
                        buf.readInt()
                    }
                }

                require(versions.size == checksums.size)

                files += versions.zip(checksums, ::File)
            }

            val maps = mutableMapOf<Int, MapSquare>()

            archive.read("map_index".uppercase()).use { buf ->
                while (buf.readableBytes() >= 7) {
                    val mapSquare = buf.readUnsignedShort()
                    val mapFile = buf.readUnsignedShort()
                    val locFile = buf.readUnsignedShort()
                    val freeToPlay = buf.readBoolean()

                    if (maps.containsKey(mapSquare)) {
                        /*
                         * If there's a map square collision, pick the first
                         * entry for compatibility with the client.
                         */
                        continue
                    }

                    maps[mapSquare] = MapSquare(mapFile, locFile, freeToPlay)
                }
            }

            return VersionList(files, maps)
        }
    }
}
