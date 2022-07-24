package org.openrs2.game.cluster

import org.openrs2.cache.config.enum.EnumTypeList
import org.openrs2.conf.CountryCode
import org.openrs2.protocol.world.downstream.WorldListResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class CountryList @Inject constructor(
    enums: EnumTypeList
) {
    private val ids = enums[COUNTRY_IDS]!!
    private val names = enums[COUNTRY_NAMES]!!

    public operator fun get(code: CountryCode): WorldListResponse.Country {
        return WorldListResponse.Country(ids.getInt(code.ordinal), names.getString(code.ordinal))
    }

    private companion object {
        private const val COUNTRY_IDS = 1669
        private const val COUNTRY_NAMES = 1626
    }
}
