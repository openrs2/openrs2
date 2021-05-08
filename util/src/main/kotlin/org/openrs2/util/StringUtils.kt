package org.openrs2.util

import org.openrs2.util.charset.Cp1252Charset

public fun CharSequence.indefiniteArticle(): String {
    require(isNotEmpty())

    return when (first().lowercaseChar()) {
        'a', 'e', 'i', 'o', 'u' -> "an"
        else -> "a"
    }
}

public fun CharSequence.krHashCode(): Int {
    var hash = 0
    for (c in this) {
        hash = ((hash shl 5) - hash) + Cp1252Charset.encode(c)
    }
    return hash
}

public fun String.capitalize(): String {
    return replaceFirstChar { it.titlecase() }
}
