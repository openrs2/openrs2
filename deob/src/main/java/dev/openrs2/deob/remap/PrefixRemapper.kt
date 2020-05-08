package dev.openrs2.deob.remap

import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.filter.ClassFilter
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.commons.SimpleRemapper

object PrefixRemapper {
    fun create(library: Library, prefix: String, excluded: ClassFilter): Remapper {
        val mapping = mutableMapOf<String, String>()

        for (clazz in library) {
            if (excluded.matches(clazz.name)) {
                mapping[clazz.name] = clazz.name
            } else {
                mapping[clazz.name] = prefix + clazz.name
            }
        }

        return SimpleRemapper(mapping)
    }
}
