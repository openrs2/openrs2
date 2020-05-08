package dev.openrs2.asm.filter

object Glob {
    fun compile(pattern: String): Regex {
        return compile(pattern, className = false)
    }

    fun compileClass(pattern: String): Regex {
        return compile(pattern, className = true)
    }

    private fun compile(pattern: String, className: Boolean): Regex {
        val regex = StringBuilder()
        var star = false
        var escape = false

        for (ch in pattern) {
            check(!star || !escape)

            if (star) {
                star = false

                if (ch == '*') {
                    regex.append(".*")
                    continue
                }

                regex.append("[^/]*")
            } else if (escape) {
                regex.append(Regex.escape(ch.toString()))
                continue
            }

            when (ch) {
                '*' -> if (className) {
                    star = true
                } else {
                    regex.append(".*")
                }
                '\\' -> escape = true
                else -> regex.append(Regex.escape(ch.toString()))
            }
        }

        if (star) {
            regex.append(".*")
        }

        require(!escape) {
            "Unterminated escape sequence"
        }

        return Regex(regex.toString())
    }
}
