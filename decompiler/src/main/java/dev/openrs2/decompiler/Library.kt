package dev.openrs2.decompiler

import java.nio.file.Path

class Library(
    val source: Path,
    val destination: Path,
    val dependencies: List<Path> = emptyList()
)
