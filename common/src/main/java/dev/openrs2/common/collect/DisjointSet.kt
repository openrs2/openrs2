package dev.openrs2.common.collect

interface DisjointSet<T> : Iterable<DisjointSet.Partition<T>> {
    interface Partition<T> : Iterable<T>

    val elements: Int
    val partitions: Int

    fun add(x: T): Partition<T>
    operator fun get(x: T): Partition<T>?
    fun union(x: Partition<T>, y: Partition<T>)
}
