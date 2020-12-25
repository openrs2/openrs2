package org.openrs2.deob.bytecode.remap

public class StaticClassMapping {
    private val nameGenerator = NameGenerator()
    private val mapping = mutableMapOf<String, String>()

    public operator fun get(name: String): String {
        return mapping.computeIfAbsent(name) { nameGenerator.generate("Static") }
    }
}
