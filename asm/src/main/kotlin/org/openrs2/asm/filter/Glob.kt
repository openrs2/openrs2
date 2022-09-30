package org.openrs2.asm.filter

public object Glob {
    public fun compile(pattern: String): Regex {
        return compile(pattern, className = false)
    }

    public fun compileClass(pattern: String): Regex {
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

                /*
                 * The deobfuscator uses ! in class names to separate the
                 * library name from the rest of the package/class name.
                 */
                regex.append("[^/!]*")
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
