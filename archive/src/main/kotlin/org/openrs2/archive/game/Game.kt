package org.openrs2.archive.game

public data class Game(
    public val id: Int,
    public val hostname: String?,
    public val port: Int?,
    public val build: Int?,
    public val lastMasterIndexId: Int?
)
