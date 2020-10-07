package org.openrs2.asm.classpath

import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.AbstractInsnNode

public abstract class ExtendedRemapper : Remapper() {
    public open fun getFieldInitializer(owner: String, name: String, descriptor: String): List<AbstractInsnNode>? {
        return null
    }

    public open fun mapFieldOwner(owner: String, name: String, descriptor: String): String {
        return mapType(owner)
    }

    public open fun mapMethodOwner(owner: String, name: String, descriptor: String): String {
        return mapType(owner)
    }

    public open fun mapArgumentName(
        owner: String,
        name: String,
        descriptor: String,
        index: Int,
        argumentName: String?
    ): String? {
        return argumentName
    }
}
