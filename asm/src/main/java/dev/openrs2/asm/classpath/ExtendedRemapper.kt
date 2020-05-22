package dev.openrs2.asm.classpath

import org.objectweb.asm.commons.Remapper

abstract class ExtendedRemapper : Remapper() {
    open fun mapFieldOwner(owner: String, name: String, desc: String): String {
        return mapType(owner)
    }

    open fun mapMethodOwner(owner: String, name: String, desc: String): String {
        return mapType(owner)
    }
}
