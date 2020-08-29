package dev.openrs2.decompiler

import java.nio.file.Path

public class Library(
    public val source: Path,
    public val destination: Path,
    public val dependencies: List<Path> = emptyList()
)
