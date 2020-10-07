package org.openrs2.deob.remap

import org.openrs2.deob.ArgRef
import org.openrs2.deob.util.map.NameMap

public class ArgumentMappingGenerator(
    private val nameMap: NameMap
) {
    public fun generate(): Map<ArgRef, String> {
        val argumentNames = mutableMapOf<ArgRef, String>()

        for ((methodRef, method) in nameMap.methods) {
            for ((index, name) in method.arguments) {
                val argument = ArgRef(methodRef, index)
                argumentNames[argument] = name
            }
        }

        return argumentNames
    }
}
