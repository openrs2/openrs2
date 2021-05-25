package org.openrs2.asm.packclass

public data class MemberRef(
    public val clazz: String,
    public val nameAndType: NameAndType
) : Comparable<MemberRef> {
    public constructor(clazz: String, name: String, descriptor: String) : this(clazz, NameAndType(name, descriptor))

    public val name: String = nameAndType.name
    public val descriptor: String = nameAndType.descriptor

    override fun compareTo(other: MemberRef): Int {
        return compareValuesBy(this, other, MemberRef::clazz, MemberRef::nameAndType)
    }
}
