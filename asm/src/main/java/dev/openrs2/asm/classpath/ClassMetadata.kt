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

    val superClassAndInterfaces: List<ClassMetadata>
        get() {
            val clazz = superClass
            return if (clazz != null) {
                listOf(clazz).plus(superInterfaces)
            } else {
                superInterfaces
            }
        }

    abstract fun getFieldAccess(field: MemberDesc): Int?
    abstract fun getMethodAccess(method: MemberDesc): Int?

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

    fun isAssignableFrom(type: ClassMetadata): Boolean {
        return type == this || isSuperClassOf(type) || isSuperInterfaceOf(type)
    }

    private tailrec fun isSuperClassOf(type: ClassMetadata): Boolean {
        val superClass = type.superClass ?: return false
        if (superClass == this) {
            return true
        }
        return isSuperClassOf(superClass)
    }

    private fun isSuperInterfaceOf(type: ClassMetadata): Boolean {
        for (superInterface in type.superInterfaces) {
            if (superInterface == this || isSuperInterfaceOf(superInterface)) {
                return true
            }
        }

        return false
    }

    fun resolveField(member: MemberDesc): ClassMetadata? {
        // see https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-5.html#jvms-5.4.3.2

        if (fields.contains(member)) {
            return this
        }

        for (superInterface in superInterfaces) {
            val field = superInterface.resolveField(member)
            if (field != null) {
                return field
            }
        }

        return superClass?.resolveField(member)
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
