package dev.openrs2.deob.remap

import dev.openrs2.asm.classpath.ExtendedRemapper
import dev.openrs2.asm.classpath.Library

private val BOUNDARY_CHARS = charArrayOf('/', '!')

fun String.splitAtLibraryBoundary(): Pair<String, String> {
    val index = indexOf('!')
    return Pair(substring(0, index), substring(index + 1))
}

fun String.getLibraryAndPackageName(): String {
    return substring(0, lastIndexOfAny(BOUNDARY_CHARS) + 1)
}

fun String.getClassName(): String {
    return substring(lastIndexOfAny(BOUNDARY_CHARS) + 1)
}

class ClassNamePrefixRemapper(vararg libraries: Library) : ExtendedRemapper() {
    private val mapping = mutableMapOf<String, String>()

    init {
        for (library in libraries) {
            for (clazz in library) {
                require(!clazz.name.contains('!')) {
                    "Input class name contains !, which conflicts with library separator"
                }
                mapping.putIfAbsent(clazz.name, "${library.name}!${clazz.name}")
            }
        }
    }

    override fun map(internalName: String): String {
        return mapping.getOrDefault(internalName, internalName)
    }
}

object StripClassNamePrefixRemapper : ExtendedRemapper() {
    override fun map(internalName: String): String {
        return internalName.substring(internalName.indexOf('!') + 1)
    }
}
