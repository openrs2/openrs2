package dev.openrs2.asm.classpath

import dev.openrs2.asm.remap
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.ClassNode
import java.util.TreeMap

class Library() : Iterable<ClassNode> {
    private var classes = TreeMap<String, ClassNode>()

    constructor(library: Library) : this() {
        for (clazz in library.classes.values) {
            val copy = ClassNode()
            clazz.accept(copy)
            add(copy)
        }
    }

    operator fun contains(name: String): Boolean {
        return classes.containsKey(name)
    }

    operator fun get(name: String): ClassNode? {
        return classes[name]
    }

    fun add(clazz: ClassNode): ClassNode? {
        return classes.put(clazz.name, clazz)
    }

    fun remove(name: String): ClassNode? {
        return classes.remove(name)
    }

    override fun iterator(): Iterator<ClassNode> {
        return classes.values.iterator()
    }

    fun remap(remapper: Remapper) {
        for (clazz in classes.values) {
            clazz.remap(remapper)
        }

        classes = classes.mapKeysTo(TreeMap()) { (_, clazz) -> clazz.name }
    }
}
