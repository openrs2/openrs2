package org.openrs2.asm

public fun String.toBinaryClassName(): String {
    return replace('/', '.')
}

public fun String.toInternalClassName(): String {
    return replace('.', '/')
}
