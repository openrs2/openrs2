package dev.openrs2.deob.processor

import com.sun.source.tree.VariableTree
import com.sun.source.util.TreePathScanner
import com.sun.source.util.Trees
import dev.openrs2.deob.annotation.Pc

class LocalVariableScanner(private val trees: Trees) : TreePathScanner<Void, MutableMap<Int, String>>() {
    override fun visitVariable(node: VariableTree, p: MutableMap<Int, String>): Void? {
        val element = trees.getElement(currentPath)

        val pc = element.getAnnotation(Pc::class.java)
        if (pc != null) {
            p[pc.value] = element.simpleName.toString()
        }

        return super.visitVariable(node, p)
    }
}
