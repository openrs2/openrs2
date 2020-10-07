package org.openrs2.deob.analysis

public enum class IntBranchResult {
    ALWAYS_TAKEN, NEVER_TAKEN, UNKNOWN;

    public companion object {
        public fun fromTakenNotTaken(taken: Int, notTaken: Int): IntBranchResult {
            require(taken != 0 || notTaken != 0)

            return when {
                taken == 0 -> NEVER_TAKEN
                notTaken == 0 -> ALWAYS_TAKEN
                else -> UNKNOWN
            }
        }
    }
}
