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

    abstract fun getAccess(method: MemberDesc): Int?

    fun isOverride(method: MemberDesc): Boolean {
        val superClass = this.superClass
        if (superClass != null) {
            if (method in superClass.methods) {
                return true
            }

            if (superClass.isOverride(method)) {
                return true
            }
        }

        for (superInterface in superInterfaces) {
            if (method in superInterface.methods) {
                return true
            }

            if (superInterface.isOverride(method)) {
                return true
            }
        }

        return false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClassMetadata) return false

        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}
