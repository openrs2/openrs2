package org.openrs2.game.store

import com.github.michaelbull.result.Result
import org.openrs2.conf.CountryCode
import org.openrs2.protocol.create.downstream.CreateResponse
import java.time.LocalDate

public interface PlayerStore {
    public suspend fun checkName(username: String): Result<Unit, CreateResponse>
    public suspend fun create(
        gameNewsletters: Boolean,
        otherNewsletters: Boolean,
        shareDetailsWithBusinessPartners: Boolean,
        username: String,
        password: String,
        affiliate: Int,
        dateOfBirth: LocalDate,
        country: CountryCode,
        email: String
    ): Result<Unit, CreateResponse>
}
