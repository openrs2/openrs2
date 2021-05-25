package org.openrs2.asm.packclass

public data class NameAndType(
    public val name: String,
    public val descriptor: String
) : Comparable<NameAndType> {
    override fun compareTo(other: NameAndType): Int {
        return compareValuesBy(this, other, NameAndType::name, NameAndType::descriptor)
    }
}
