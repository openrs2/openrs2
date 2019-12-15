package dev.openrs2.util

fun CharSequence.indefiniteArticle(): String {
    require(isNotEmpty())

    val first = first().toLowerCase()
    return when (first) {
        'a', 'e', 'i', 'o', 'u' -> "an"
        else -> "a"
    }
}
