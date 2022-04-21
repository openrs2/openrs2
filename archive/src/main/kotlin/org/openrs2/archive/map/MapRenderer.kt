package org.openrs2.archive.map

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap
import org.openrs2.buffer.use
import org.openrs2.cache.Group
import org.openrs2.cache.Js5Archive
import org.openrs2.cache.Js5Compression
import org.openrs2.cache.Js5ConfigGroup
import org.openrs2.cache.Js5Index
import org.openrs2.db.Database
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.sql.Connection
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

public class MapRenderer @Inject constructor(
    private val database: Database
) {
    private enum class MapSquareState(
        val outlineColor: Color
    ) {
        UNKNOWN(Color.RED),
        EMPTY(Color.GRAY),
        VALID(Color.GREEN);

        val fillColor = Color(outlineColor.red, outlineColor.green, outlineColor.blue, 128)
    }

    public suspend fun render(scope: String, masterIndexId: Int): BufferedImage {
        return database.execute { connection ->
            val scopeId = connection.prepareStatement(
                """
                SELECT id
                FROM scopes
                WHERE name = ?
            """.trimIndent()
            ).use { stmt ->
                stmt.setString(1, scope)

                stmt.executeQuery().use { rows ->
                    if (!rows.next()) {
                        throw IllegalArgumentException("Invalid scope")
                    }

                    rows.getInt(1)
                }
            }

            // read config index
            val configIndex = readIndex(connection, scopeId, masterIndexId, Js5Archive.CONFIG)
                ?: throw IllegalArgumentException("Config index missing")

            // read FluType group
            val underlayColors = mutableMapOf<Int, Int>()

            val underlayGroup = configIndex[Js5ConfigGroup.FLUTYPE]
                ?: throw IllegalArgumentException("FluType group missing in index")

            val underlayFiles = readGroup(connection, scopeId, masterIndexId, Js5Archive.CONFIG, underlayGroup)
                ?: throw IllegalArgumentException("FluType group missing")
            try {
                for ((id, file) in underlayFiles) {
                    underlayColors[id] = FluType.read(file).color
                }
            } finally {
                underlayFiles.values.forEach(ByteBuf::release)
            }

            // read FloType group
            val overlays = mutableMapOf<Int, FloType>()

            val overlayGroup = configIndex[Js5ConfigGroup.FLOTYPE]
                ?: throw IllegalArgumentException("FloType group missing in index")

            val overlayFiles = readGroup(connection, scopeId, masterIndexId, Js5Archive.CONFIG, overlayGroup)
                ?: throw IllegalArgumentException("FloType group missing")
            try {
                for ((id, file) in overlayFiles) {
                    overlays[id] = FloType.read(file)
                }
            } finally {
                overlayFiles.values.forEach(ByteBuf::release)
            }

            // read textures
            val textures = mutableMapOf<Int, Int>()
            val materialsIndex = readIndex(connection, scopeId, masterIndexId, Js5Archive.MATERIALS)

            if (materialsIndex != null) {
                val materialsGroup = materialsIndex[0]
                    ?: throw IllegalArgumentException("Materials group missing in index")

                val materialsFiles = readGroup(connection, scopeId, masterIndexId, Js5Archive.MATERIALS, materialsGroup)
                    ?: throw IllegalArgumentException("Materials group missing")
                try {
                    val metadata = materialsFiles[0]
                    val len = metadata.readUnsignedShort()

                    val ids = mutableSetOf<Int>()
                    for (id in 0 until len) {
                        if (metadata.readBoolean()) {
                            ids += id
                        }
                    }

                    val use = mutableSetOf<Int>()
                    for (id in ids) {
                        if (metadata.readBoolean()) {
                            use += id
                        }
                    }

                    // the number of booleans to skip varies in different builds
                    outer@ while (true) {
                        val start = metadata.readerIndex()

                        for (i in 0 until ids.size) {
                            if (metadata.getUnsignedByte(start + i) > 1) {
                                break@outer
                            }
                        }

                        metadata.skipBytes(ids.size)
                    }

                    metadata.skipBytes(ids.size * 4)

                    for (id in ids) {
                        textures[id] = metadata.readUnsignedShort()

                        if (id !in use) {
                            textures.remove(id)
                        }
                    }
                } finally {
                    materialsFiles.values.forEach(ByteBuf::release)
                }
            } else {
                val textureIndex = readIndex(connection, scopeId, masterIndexId, Js5Archive.TEXTURES)
                    ?: throw IllegalArgumentException("Textures index missing")

                val textureGroup = textureIndex[0]
                    ?: throw IllegalArgumentException("Textures group missing from index")

                val textureFiles = readGroup(connection, scopeId, masterIndexId, Js5Archive.TEXTURES, textureGroup)
                    ?: throw IllegalArgumentException("Textures group missing")
                try {
                    for ((id, file) in textureFiles) {
                        textures[id] = file.readUnsignedShort()
                    }
                } finally {
                    textureFiles.values.forEach(ByteBuf::release)
                }
            }

            // create overlay colors
            val overlayColors = createOverlayColors(overlays, textures)

            // read loc encrypted/empty loc flags and keys and determine bounds of the map
            var x0 = Int.MAX_VALUE
            var x1 = Int.MIN_VALUE
            var z0 = Int.MAX_VALUE
            var z1 = Int.MIN_VALUE
            val states = mutableMapOf<Int, MapSquareState>()

            connection.prepareStatement(
                """
                SELECT n.name, g.encrypted, g.empty_loc, g.key_id
                FROM resolved_groups g
                JOIN names n ON n.hash = g.name_hash
                WHERE g.scope_id = ? AND g.master_index_id = ? AND g.archive_id = ${Js5Archive.MAPS} AND
                    n.name ~ '^[lm](?:[0-9]|[1-9][0-9])_(?:[0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$'
            """.trimIndent()
            ).use { stmt ->
                stmt.setInt(1, scopeId)
                stmt.setInt(2, masterIndexId)

                stmt.executeQuery().use { rows ->
                    while (rows.next()) {
                        val name = rows.getString(1)
                        val encrypted = rows.getBoolean(2)
                        val empty = rows.getBoolean(3)
                        var keyId: Long? = rows.getLong(4)
                        if (rows.wasNull()) {
                            keyId = null
                        }

                        val (x, z) = getMapCoordinates(name)
                        x0 = min(x0, x)
                        x1 = max(x1, x)
                        z0 = min(z0, z)
                        z1 = max(z1, z)

                        if (name.startsWith('l')) {
                            val mapSquare = getMapSquare(x, z)
                            states[mapSquare] = if (!encrypted || keyId != null) {
                                MapSquareState.VALID
                            } else if (empty) {
                                MapSquareState.EMPTY
                            } else {
                                MapSquareState.UNKNOWN
                            }
                        }
                    }
                }
            }

            if (x0 == Int.MAX_VALUE) {
                throw IllegalArgumentException("Map empty")
            }

            // read and render maps
            val image = BufferedImage(
                ((x1 - x0) + 1) * MAP_SIZE,
                ((z1 - z0) + 1) * MAP_SIZE,
                BufferedImage.TYPE_INT_RGB
            )

            connection.prepareStatement(
                """
                SELECT n.name, g.data
                FROM resolved_groups g
                JOIN names n ON n.hash = g.name_hash
                WHERE g.scope_id = ? AND g.master_index_id = ? AND g.archive_id = ${Js5Archive.MAPS} AND
                    n.name ~ '^m(?:[0-9]|[1-9][0-9])_(?:[0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$'
            """.trimIndent()
            ).use { stmt ->
                stmt.setInt(1, scopeId)
                stmt.setInt(2, masterIndexId)

                stmt.executeQuery().use { rows ->
                    while (rows.next()) {
                        val name = rows.getString(1)
                        val bytes = rows.getBytes(2)

                        val (x, z) = getMapCoordinates(name)

                        Unpooled.wrappedBuffer(bytes).use { compressed ->
                            Js5Compression.uncompress(compressed).use { uncompressed ->
                                renderMap(image, x - x0, z - z0, uncompressed, underlayColors, overlayColors)
                            }
                        }
                    }
                }
            }

            // render state overlay
            val graphics = image.createGraphics()

            for (x in x0..x1) {
                for (z in z0..z1) {
                    val mapSquare = getMapSquare(x, z)
                    val state = states[mapSquare] ?: continue
                    val label = "${x}_$z"

                    renderStateOverlay(image, graphics, x - x0, z - z0, state, label)
                }
            }

            return@execute image
        }
    }

    private fun readIndex(connection: Connection, scopeId: Int, masterIndexId: Int, archiveId: Int): Js5Index? {
        connection.prepareStatement(
            """
            SELECT data
            FROM resolved_indexes
            WHERE scope_id = ? AND master_index_id = ? AND archive_id = ?
        """.trimIndent()
        ).use { stmt ->
            stmt.setInt(1, scopeId)
            stmt.setInt(2, masterIndexId)
            stmt.setInt(3, archiveId)

            stmt.executeQuery().use { rows ->
                if (!rows.next()) {
                    return null
                }

                val bytes = rows.getBytes(1)

                Unpooled.wrappedBuffer(bytes).use { compressed ->
                    Js5Compression.uncompress(compressed).use { uncompressed ->
                        return Js5Index.read(uncompressed)
                    }
                }
            }
        }
    }

    private fun readGroup(
        connection: Connection,
        scopeId: Int,
        masterIndexId: Int,
        archiveId: Int,
        group: Js5Index.Group<*>
    ): Int2ObjectSortedMap<ByteBuf>? {
        connection.prepareStatement(
            """
            SELECT data
            FROM resolved_groups
            WHERE scope_id = ? AND master_index_id = ? AND archive_id = ? AND group_id = ?
        """.trimIndent()
        ).use { stmt ->
            stmt.setInt(1, scopeId)
            stmt.setInt(2, masterIndexId)
            stmt.setInt(3, archiveId)
            stmt.setInt(4, group.id)

            stmt.executeQuery().use { rows ->
                if (!rows.next()) {
                    return null
                }

                val bytes = rows.getBytes(1)

                Unpooled.wrappedBuffer(bytes).use { compressed ->
                    Js5Compression.uncompress(compressed).use { uncompressed ->
                        return Group.unpack(uncompressed, group)
                    }
                }
            }
        }
    }

    private fun createOverlayColors(overlays: Map<Int, FloType>, textures: Map<Int, Int>): Map<Int, Int> {
        return overlays.mapValues { (_, type) ->
            if (type.blendColor != -1) {
                type.blendColor
            } else if (type.texture != -1 && type.texture in textures) {
                val averageColor = textures[type.texture]!!
                Colors.hslToRgb(Colors.multiplyLightness(averageColor, 96))
            } else if (type.color == 0xFF00FF) {
                0
            } else {
                type.color
            }
        }
    }

    private fun renderMap(
        image: BufferedImage,
        x: Int,
        z: Int,
        buf: ByteBuf,
        underlayColors: Map<Int, Int>,
        overlayColors: Map<Int, Int>
    ) {
        for (plane in 0 until LEVELS) {
            for (dx in 0 until MAP_SIZE) {
                for (dz in 0 until MAP_SIZE) {
                    var overlay = 0
                    var shape = 0
                    var underlay = 0

                    while (true) {
                        val code = buf.readUnsignedByte().toInt()
                        if (code == 0) {
                            break
                        } else if (code == 1) {
                            buf.skipBytes(1)
                            break
                        } else if (code <= 49) {
                            overlay = buf.readUnsignedByte().toInt()
                            shape = (code - 2) shr 2
                        } else if (code <= 81) {
                            // empty
                        } else {
                            underlay = code - 81
                        }
                    }

                    var color = 0

                    if (underlay != 0) {
                        color = underlayColors[underlay - 1]!!
                    }

                    if (overlay != 0 && shape == 0) {
                        color = overlayColors[overlay - 1]!!
                    }

                    if (color > 0) {
                        image.setRGB(x * MAP_SIZE + dx, image.height - (z * MAP_SIZE + dz) - 1, color)
                    }
                }
            }
        }
    }

    private fun renderStateOverlay(
        image: BufferedImage,
        graphics: Graphics2D,
        mapX: Int,
        mapZ: Int,
        state: MapSquareState,
        label: String
    ) {
        val x = mapX * MAP_SIZE
        val y = image.height - (mapZ + 1) * MAP_SIZE

        if (state != MapSquareState.VALID) {
            graphics.color = state.fillColor
            graphics.fillRect(
                x,
                y,
                MAP_SIZE,
                MAP_SIZE
            )
        }

        graphics.color = state.outlineColor
        graphics.drawRect(
            x,
            y,
            MAP_SIZE - 1,
            MAP_SIZE - 1
        )

        val labelWidth = graphics.fontMetrics.stringWidth(label)
        val labelHeight = graphics.fontMetrics.height
        val labelAscent = graphics.fontMetrics.ascent

        val labelX = x + (MAP_SIZE - labelWidth) / 2
        val labelY = y + (MAP_SIZE - labelHeight) / 2 + labelAscent

        graphics.color = Color.BLACK
        graphics.drawString(label, labelX + 1, labelY)

        graphics.color = Color.BLACK
        graphics.drawString(label, labelX, labelY + 1)

        graphics.color = Color.WHITE
        graphics.drawString(label, labelX, labelY)
    }

    private companion object {
        private val LOC_OR_MAP_NAME_REGEX = Regex("[lm](\\d+)_(\\d+)")
        private const val MAP_SIZE = 64
        private const val LEVELS = 4

        private fun getMapCoordinates(name: String): Pair<Int, Int> {
            val match = LOC_OR_MAP_NAME_REGEX.matchEntire(name)
            require(match != null)

            val x = match.groupValues[1].toInt()
            val z = match.groupValues[2].toInt()

            return Pair(x, z)
        }

        private fun getMapSquare(x: Int, z: Int): Int {
            return (x shl 8) or z
        }
    }
}
