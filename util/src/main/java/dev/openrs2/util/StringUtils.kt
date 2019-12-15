package dev.openrs2.util

object StringUtils {
    // TODO(gpe): convert to an extension function
    @JvmStatic
    fun indefiniteArticle(str: String): String {
        require(str.isNotEmpty())

        val first = str.first().toLowerCase()
        return when (first) {
            'a', 'e', 'i', 'o', 'u' -> "an"
            else -> "a"
        }
    }

    @JvmStatic
    fun capitalize(str: String): String {
        return str.capitalize()
    }
}
