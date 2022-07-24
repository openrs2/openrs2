package org.openrs2.game.cluster

import org.openrs2.protocol.world.downstream.WorldListResponse
import java.util.SortedMap

public interface Cluster {
    public fun getWorldList(): Pair<SortedMap<Int, WorldListResponse.World>, SortedMap<Int, Int>>
}
