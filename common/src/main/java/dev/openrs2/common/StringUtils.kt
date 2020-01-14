package dev.openrs2.common

fun CharSequence.indefiniteArticle(): String {
    require(isNotEmpty())

    return when (first().toLowerCase()) {
        'a', 'e', 'i', 'o', 'u' -> "an"
        else -> "a"
    }
}
