package dev.openrs2.deob.remap

import dev.openrs2.asm.classpath.ExtendedRemapper
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.filter.ClassFilter

class PrefixRemapper(private val prefix: String, private val classes: Set<String>) : ExtendedRemapper() {
    override fun map(internalName: String): String {
        return if (classes.contains(internalName)) {
            prefix + internalName
        } else {
            internalName
        }
    }

    companion object {
        fun create(library: Library, prefix: String, excluded: ClassFilter): ExtendedRemapper {
            val classes = mutableSetOf<String>()

            for (clazz in library) {
                if (!excluded.matches(clazz.name)) {
                    classes += clazz.name
                }
            }

            return PrefixRemapper(prefix, classes)
        }
    }
}
