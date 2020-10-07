package org.openrs2.conf

public data class Config(
    val game: String,
    val operator: String,
    val domain: String
) {
    val internalGame: String
    val internalOperator: String

    init {
        internalGame = game.toInternalName()
        internalOperator = operator.toInternalName()
    }

    private companion object {
        private val INTERNAL_NAME_REGEX = Regex("(?i)[^a-z0-9]+")

        private fun String.toInternalName(): String {
            return replace(INTERNAL_NAME_REGEX, "_").trim('_').toLowerCase()
        }
    }
}
