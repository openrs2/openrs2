package dev.openrs2.deob.remap

class NameGenerator {
    private val prefixes = mutableMapOf<String, Int>()

    fun generate(prefix: String): String {
        require(prefix.isNotEmpty())

        val separator = if (prefix.last().isDigit()) {
            "_"
        } else {
            ""
        }

        val index = prefixes.merge(prefix, 1, Integer::sum)

        return prefix + separator + index
    }
}
