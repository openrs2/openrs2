package dev.openrs2.deob.ast.util

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.Node.TreeTraversal

inline fun <reified T : Node> Node.walk(traversal: TreeTraversal, crossinline consumer: (T) -> Unit) {
    this.walk(traversal) {
        if (it is T) {
            consumer(it)
        }
    }
}
