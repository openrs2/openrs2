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

        for (ch in pattern) {
            if (star) {
                star = false

                if (ch == '*') {
                    regex.append(".*")
                    continue
                }

                regex.append("[^/]*")
            }

            when (ch) {
                '*' -> if (className) {
                    star = true
                } else {
                    regex.append(".*")
                }
                else -> regex.append(Regex.escape(ch.toString()))
            }
        }

        if (star) {
            regex.append(".*")
        }

        return Regex(regex.toString())
    }
}
