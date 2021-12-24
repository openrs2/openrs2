package org.openrs2.archive.game

public data class Game(
    public val id: Int,
    public val url: String?,
    public val buildMajor: Int?,
    public val buildMinor: Int?,
    public val lastMasterIndexId: Int?,
    public val languageId: Int
)
