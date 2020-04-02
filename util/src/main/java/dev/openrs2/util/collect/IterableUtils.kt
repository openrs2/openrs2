package dev.openrs2.util.collect

inline fun <T> MutableIterable<T>.removeFirst(predicate: (T) -> Boolean): Boolean {
    val iterator = iterator()
    for (element in iterator) {
        if (predicate(element)) {
            iterator.remove()
            return true
        }
    }

    return false
}
