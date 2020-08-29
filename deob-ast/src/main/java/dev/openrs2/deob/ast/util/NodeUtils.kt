package dev.openrs2.deob.ast.util

import com.github.javaparser.ast.Node

public inline fun <reified T : Node> Node.walk(crossinline consumer: (T) -> Unit) {
    this.walk(Node.TreeTraversal.POSTORDER) {
        if (it is T) {
            consumer(it)
        }
    }
}
