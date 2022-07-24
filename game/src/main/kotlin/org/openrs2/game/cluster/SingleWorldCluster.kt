package org.openrs2.game.cluster

import org.openrs2.conf.Config
import org.openrs2.protocol.world.downstream.WorldListResponse
import java.util.SortedMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class SingleWorldCluster @Inject constructor(
    config: Config,
    countries: CountryList
) : Cluster {
    private val worlds = sortedMapOf(
        config.world to WorldListResponse.World(
            countries[config.country],
            config.members,
            config.quickChat,
            config.pvp,
            config.lootShare,
            config.activity,
            config.hostname
        )
    )
    private val players = sortedMapOf(0 to 0)

    override fun getWorldList(): Pair<SortedMap<Int, WorldListResponse.World>, SortedMap<Int, Int>> {
        return Pair(worlds, players)
    }
}
