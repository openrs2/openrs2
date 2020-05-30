package dev.openrs2.deob.remap

class StaticClassGenerator(private val generator: NameGenerator, private val maxMembers: Int) {
    private var lastClass: String? = null
    private var members = 0

    fun generate(): String {
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
