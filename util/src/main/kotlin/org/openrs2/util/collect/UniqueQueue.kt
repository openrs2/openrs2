package org.openrs2.util.collect

public class UniqueQueue<T> {
    private val queue = ArrayDeque<T>()
    private val set = mutableSetOf<T>()

    public fun add(v: T): Boolean {
        if (set.add(v)) {
            queue.addLast(v)
            return true
        }

        return false
    }

    public operator fun plusAssign(v: T) {
        add(v)
    }

    public fun addAll(vs: Iterable<T>) {
        for (v in vs) {
            add(v)
        }
    }

    public operator fun plusAssign(vs: Iterable<T>) {
        addAll(vs)
    }

    public fun poll(): T? {
        val v = queue.removeFirstOrNull()
        if (v != null) {
            set.remove(v)
            return v
        }

        return null
    }

    public fun clear() {
        queue.clear()
        set.clear()
    }
}
