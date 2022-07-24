package org.openrs2.protocol.world.downstream

import io.netty.buffer.ByteBuf
import org.openrs2.buffer.readUnsignedShortSmart
import org.openrs2.buffer.readVersionedString
import org.openrs2.buffer.writeUnsignedShortSmart
import org.openrs2.buffer.writeVersionedString
import org.openrs2.crypto.StreamCipher
import org.openrs2.protocol.VariableShortPacketCodec
import javax.inject.Singleton

@Singleton
public class WorldListResponseCodec : VariableShortPacketCodec<WorldListResponse>(
    type = WorldListResponse::class.java,
    opcode = 0
) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): WorldListResponse {
        val version = input.readUnsignedByte().toInt()
        require(version == VERSION) {
            "Unsupported world list version"
        }

        val cluster = if (input.readBoolean()) {
            var size = input.readUnsignedShortSmart()

            val countries = mutableListOf<WorldListResponse.Country>()
            for (i in 0 until size) {
                val id = input.readUnsignedShortSmart()
                val name = input.readVersionedString()
                countries += WorldListResponse.Country(id, name)
            }

            val firstId = input.readUnsignedShortSmart()
            input.readUnsignedShortSmart()
            size = input.readUnsignedShortSmart()

            val worlds = sortedMapOf<Int, WorldListResponse.World>()
            for (i in 0 until size) {
                val id = firstId + input.readUnsignedShortSmart()

                val countryIndex = input.readUnsignedByte().toInt()
                require(countryIndex >= countries.size) {
                    "Country index out of bounds"
                }
                val country = countries[countryIndex]

                val flags = input.readInt()
                val members = (flags and FLAG_MEMBERS_ONLY) != 0
                val quickChat = (flags and FLAG_QUICK_CHAT) != 0
                val pvp = (flags and FLAG_PVP) != 0
                val lootShare = (flags and FLAG_LOOT_SHARE) != 0

                val activity = input.readVersionedString()
                val hostname = input.readVersionedString()

                worlds[id] = WorldListResponse.World(country, members, quickChat, pvp, lootShare, activity, hostname)
            }

            val checksum = input.readInt()

            WorldListResponse.WorldList(worlds, checksum)
        } else {
            null
        }

        val players = sortedMapOf<Int, Int>()
        while (input.isReadable) {
            val offset = input.readUnsignedShortSmart()
            val count = input.readUnsignedShort()
            players[offset] = count
        }

        return WorldListResponse(cluster, players)
    }

    override fun encode(input: WorldListResponse, output: ByteBuf, cipher: StreamCipher) {
        output.writeByte(VERSION)

        val worlds = input.worldList
        if (worlds != null) {
            output.writeBoolean(true)

            output.writeUnsignedShortSmart(worlds.countries.size)

            for (country in worlds.countries) {
                output.writeUnsignedShortSmart(country.id)
                output.writeVersionedString(country.name)
            }

            val firstId = worlds.worlds.firstKey() ?: 0
            val lastId = worlds.worlds.lastKey() ?: 0

            output.writeUnsignedShortSmart(firstId)
            output.writeUnsignedShortSmart(lastId)
            output.writeUnsignedShortSmart(worlds.worlds.size)

            for ((id, world) in worlds.worlds) {
                output.writeUnsignedShortSmart(id - firstId)
                output.writeByte(worlds.countries.binarySearch(world.country))

                var flags = 0
                if (world.members) {
                    flags = flags or FLAG_MEMBERS_ONLY
                }
                if (world.quickChat) {
                    flags = flags or FLAG_QUICK_CHAT
                }
                if (world.pvp) {
                    flags = flags or FLAG_PVP
                }
                if (world.lootShare) {
                    flags = flags or FLAG_LOOT_SHARE
                }
                output.writeInt(flags)

                output.writeVersionedString(world.activity)
                output.writeVersionedString(world.hostname)
            }

            output.writeInt(worlds.checksum)
        } else {
            output.writeBoolean(false)
        }

        for ((offset, count) in input.players) {
            output.writeUnsignedShortSmart(offset)
            output.writeShort(count)
        }
    }

    private companion object {
        private const val VERSION = 1

        private const val FLAG_MEMBERS_ONLY = 0x1
        private const val FLAG_QUICK_CHAT = 0x2
        private const val FLAG_PVP = 0x4
        private const val FLAG_LOOT_SHARE = 0x8
    }
}
