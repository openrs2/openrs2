package dev.openrs2.asm.classpath

import org.objectweb.asm.commons.Remapper

abstract class ExtendedRemapper : Remapper() {
    open fun getFieldInitializer(owner: String, name: String, descriptor: String): FieldInitializer? {
        return null
    }

    open fun mapFieldOwner(owner: String, name: String, descriptor: String): String {
        return mapType(owner)
    }

    open fun mapMethodOwner(owner: String, name: String, descriptor: String): String {
        return mapType(owner)
    }
}
