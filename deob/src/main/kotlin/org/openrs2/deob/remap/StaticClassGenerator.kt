package org.openrs2.deob.remap

public class StaticClassGenerator(private val generator: NameGenerator, private val maxMembers: Int) {
    private var lastClass: String? = null
    private var members = 0

    public fun generate(): String {
        var clazz = lastClass

        if (clazz == null || members >= maxMembers) {
            clazz = generator.generate("Static")
            lastClass = clazz
            members = 1
        } else {
            members++
        }

        return clazz
    }
}
