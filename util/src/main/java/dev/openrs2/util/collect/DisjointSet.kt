package dev.openrs2.util.collect

interface DisjointSet<T> : Iterable<DisjointSet.Partition<T>> {
    interface Partition<T> : Iterable<T>

    fun add(x: T): Partition<T>
    operator fun get(x: T): Partition<T>?
    fun union(x: Partition<T>, y: Partition<T>)
    fun elements(): Int
    fun partitions(): Int
}
