package dev.openrs2.util

import dev.openrs2.util.charset.Cp1252Charset

fun CharSequence.indefiniteArticle(): String {
    require(isNotEmpty())

    return when (first().toLowerCase()) {
        'a', 'e', 'i', 'o', 'u' -> "an"
        else -> "a"
    }
}

fun CharSequence.krHashCode(): Int {
    var hash = 0
    for (c in this) {
        hash = ((hash shl 5) - hash) + Cp1252Charset.encode(c)
    }
    return hash
}
