package org.openrs2.conf

public data class Config(
    val game: String,
    val operator: String,
    val domain: String,
    val world: Int,
    val hostname: String,
    val country: CountryCode,
    val activity: String,
    val members: Boolean,
    val quickChat: Boolean,
    val pvp: Boolean,
    val lootShare: Boolean,
    val dedicatedActivity: Boolean
) {
    val internalGame: String = game.toInternalName()
    val internalOperator: String = operator.toInternalName()

    private companion object {
        private val INTERNAL_NAME_REGEX = Regex("(?i)[^a-z0-9]+")

        private fun String.toInternalName(): String {
            return replace(INTERNAL_NAME_REGEX, "_").trim('_').lowercase()
        }
    }
}
