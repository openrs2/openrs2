package dev.openrs2.util.collect

import java.util.ArrayDeque

class ForestDisjointSet<T> : DisjointSet<T> {
    private class Node<T>(val value: T) : DisjointSet.Partition<T> {
        val children = mutableListOf<Node<T>>()
        private var _parent = this
        var parent
            get() = _parent
            set(parent) {
                _parent = parent
                _parent.children.add(this)
            }
        var rank = 0

        fun find(): Node<T> {
            if (parent !== this) {
                _parent = parent.find()
            }
            return parent
        }

        override fun iterator(): Iterator<T> {
            return NodeIterator(find())
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Node<*>) return false

            return find() === other.find()
        }

        override fun hashCode(): Int {
            return find().value.hashCode()
        }

        override fun toString(): String {
            return find().value.toString()
        }
    }

    private class NodeIterator<T>(root: Node<T>) : Iterator<T> {
        private val queue = ArrayDeque<Node<T>>()

        init {
            queue.add(root)
        }

        override fun hasNext(): Boolean {
            return queue.isNotEmpty()
        }

        override fun next(): T {
            val node = queue.poll() ?: throw NoSuchElementException()
            queue.addAll(node.children)
            return node.value
        }
    }

    private val nodes = mutableMapOf<T, Node<T>>()
    override val elements
        get() = nodes.size
    override var partitions = 0
        private set

    override fun add(x: T): DisjointSet.Partition<T> {
        val node = findNode(x)
        if (node != null) {
            return node
        }

        partitions++

        val newNode = Node(x)
        nodes[x] = newNode
        return newNode
    }

    override fun get(x: T): DisjointSet.Partition<T>? {
        return findNode(x)
    }

    private fun findNode(x: T): Node<T>? {
        val node = nodes[x] ?: return null
        return node.find()
    }

    override fun union(x: DisjointSet.Partition<T>, y: DisjointSet.Partition<T>) {
        require(x is Node<T>)
        require(y is Node<T>)

        val xRoot = x.find()
        val yRoot = y.find()

        if (xRoot == yRoot) {
            return
        }

        when {
            xRoot.rank < yRoot.rank -> {
                xRoot.parent = yRoot
            }
            xRoot.rank > yRoot.rank -> {
                yRoot.parent = xRoot
            }
            else -> {
                yRoot.parent = xRoot
                xRoot.rank++
            }
        }

        partitions--
    }

    override fun iterator(): Iterator<DisjointSet.Partition<T>> {
        return nodes.values.iterator()
    }
}
