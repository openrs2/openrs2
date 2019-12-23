package dev.openrs2.asm.classpath

import dev.openrs2.asm.MemberDesc

abstract class ClassMetadata {
    abstract val name: String
    abstract val dependency: Boolean
    abstract val `interface`: Boolean
    abstract val superClass: ClassMetadata?
    abstract val superInterfaces: List<ClassMetadata>
    abstract val fields: List<MemberDesc>
    abstract val methods: List<MemberDesc>

    abstract fun isNative(method: MemberDesc): Boolean

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClassMetadata) return false

        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}
