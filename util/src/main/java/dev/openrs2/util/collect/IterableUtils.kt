package dev.openrs2.util.collect

fun <T> MutableIterable<T>.removeFirst(): T {
    return removeFirstOrNull() ?: throw NoSuchElementException()
}

fun <T> MutableIterable<T>.removeFirstOrNull(): T? {
    val iterator = iterator()
    if (!iterator.hasNext()) {
        return null
    }

    val element = iterator.next()
    iterator.remove()

    return element
}

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
