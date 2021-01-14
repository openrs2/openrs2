package org.openrs2.cli

import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.parameters.options.NullableOption
import com.github.ajalt.clikt.parameters.options.RawOption
import com.github.ajalt.clikt.parameters.options.convert
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

public fun RawOption.instant(): NullableOption<Instant, Instant> = convert({ "TIMESTAMP" }) {
    try {
        OffsetDateTime.parse(it).toInstant()
    } catch (ex: DateTimeParseException) {
        throw BadParameterValue("$it is not a valid timestamp")
    }
}
