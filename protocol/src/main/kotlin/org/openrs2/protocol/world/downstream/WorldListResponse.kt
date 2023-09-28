package org.openrs2.protocol.world.downstream

import org.openrs2.protocol.Packet
import java.util.SortedMap

public data class WorldListResponse(
    public val worldList: WorldList?,
    public val players: SortedMap<Int, Int>
) : Packet {
    public data class Country(
        public val id: Int,
        public val name: String
    ) : Comparable<Country> {
        override fun compareTo(other: Country): Int {
            return compareValuesBy(this, other, Country::name, Country::id)
        }
    }

    public data class World(
        public val country: Country,
        public val members: Boolean,
        public val quickChat: Boolean,
        public val pvp: Boolean,
        public val lootShare: Boolean,
        public val dedicatedActivity: Boolean,
        public val activity: String,
        public val hostname: String
    )

    public data class WorldList(
        public val worlds: SortedMap<Int, World>,
        public val checksum: Int
    ) {
        public val countries: List<Country> = worlds.values.map(World::country).distinct().sorted()
    }

    public companion object {
        public const val OFFLINE: Int = -1
    }
}
