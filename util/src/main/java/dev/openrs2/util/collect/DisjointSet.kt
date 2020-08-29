package dev.openrs2.util.collect

/**
 * A data structure containing a set of elements partitioned into a number of
 * non-overlapping subsets. New elements belong to singleton subsets. The
 * [union] function combines two subsets together into a single larger subset.
 */
public interface DisjointSet<T> : Iterable<DisjointSet.Partition<T>> {
    public interface Partition<T> : Iterable<T>

    public val elements: Int
    public val partitions: Int

    public fun add(x: T): Partition<T>
    public operator fun get(x: T): Partition<T>?
    public fun union(x: Partition<T>, y: Partition<T>)
}
