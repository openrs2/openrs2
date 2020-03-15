package dev.openrs2.deob.ir.translation.decompiler

import dev.openrs2.deob.ir.Expr
import org.objectweb.asm.Type
import org.objectweb.asm.tree.analysis.Value

class IrValue(val expr: Expr?, val type: Type? = null) : Value {
    override fun getSize() = type?.size ?: 1
}
