package dev.openrs2.deob.analysis

enum class IntBranchResult {
    ALWAYS_TAKEN, NEVER_TAKEN, UNKNOWN;

    companion object {
        fun fromTakenNotTaken(taken: Int, notTaken: Int): IntBranchResult {
            require(taken != 0 || notTaken != 0)

            return when {
                taken == 0 -> NEVER_TAKEN
                notTaken == 0 -> ALWAYS_TAKEN
                else -> UNKNOWN
            }
        }
    }
}
