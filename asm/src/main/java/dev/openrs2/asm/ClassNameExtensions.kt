package dev.openrs2.asm

fun String.toBinaryClassName(): String {
    return replace('/', '.')
}

fun String.toInternalClassName(): String {
    return replace('.', '/')
}
