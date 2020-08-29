package dev.openrs2.deob.ast

public class LibraryGroup(libraries: Iterable<Library>) : Iterable<Library> {
    private val libraries = libraries.associateBy { it.name }

    public operator fun get(name: String): Library? {
        return libraries[name]
    }

    override fun iterator(): Iterator<Library> {
        return libraries.values.iterator()
    }
}
