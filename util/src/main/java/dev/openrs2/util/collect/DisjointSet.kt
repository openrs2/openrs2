package dev.openrs2.util.collect

/**
 * A data structure containing a set of elements partitioned into a number of
 * non-overlapping subsets. New elements belong to singleton subsets. The
 * [union] function combines two subsets together into a single larger subset.
 */
interface DisjointSet<T> : Iterable<DisjointSet.Partition<T>> {
    interface Partition<T> : Iterable<T>

    val elements: Int
    val partitions: Int

    fun add(x: T): Partition<T>
    operator fun get(x: T): Partition<T>?
    fun union(x: Partition<T>, y: Partition<T>)
}
