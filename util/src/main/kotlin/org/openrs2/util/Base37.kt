package org.openrs2.util

public object Base37 {
    private const val MAX_LENGTH: Int = 12

    private val FIRST_VALID_NAME = encode("")
    private val LAST_VALID_NAME = encode("999999999999")

    private val DECODE_TABLE = CharArray(37) { i ->
        when (i) {
            0 -> '_'
            in 1..26 -> 'a' + (i - 1)
            else -> '0' + (i - 27)
        }
    }

    public fun encode(s: String): Long {
        // casting to CharSequence avoids a copy
        val trimmed = (s as CharSequence).trim { it == ' ' || it == '_' }
        require(trimmed.length <= MAX_LENGTH)

        var n = 0L

        for (c in trimmed) {
            n *= 37

            when (c) {
                in 'A'..'Z' -> n += 1 + (c - 'A')
                in 'a'..'z' -> n += 1 + (c - 'a')
                in '0'..'9' -> n += 27 + (c - '0')
                ' ', '_' -> Unit
                else -> throw IllegalArgumentException()
            }
        }

        return n
    }

    public fun decodeLowerCase(n: Long): String {
        require(n in FIRST_VALID_NAME..LAST_VALID_NAME)
        require(n == 0L || n % 37 != 0L)

        val chars = CharArray(MAX_LENGTH)
        var len = 0
        var temp = n

        while (temp != 0L) {
            chars[len++] = DECODE_TABLE[(temp % 37).toInt()]
            temp /= 37
        }

        chars.reverse(0, len)
        return String(chars, 0, len)
    }

    public fun decodeTitleCase(n: Long): String {
        require(n in FIRST_VALID_NAME..LAST_VALID_NAME)
        require(n == 0L || n % 37 != 0L)

        val chars = CharArray(MAX_LENGTH)
        var len = 0
        var temp = n

        while (temp != 0L) {
            var c = DECODE_TABLE[(temp % 37).toInt()]
            temp /= 37

            if (c == '_') {
                c = ' '
                chars[len - 1] = chars[len - 1].uppercaseChar()
            }

            chars[len++] = c
        }

        chars.reverse(0, len)
        chars[0] = chars[0].uppercaseChar()

        return String(chars, 0, len)
    }

    public fun toLowerCase(s: String): String {
        return decodeLowerCase(encode(s))
    }

    public fun toTitleCase(s: String): String {
        return decodeTitleCase(encode(s))
    }
}
