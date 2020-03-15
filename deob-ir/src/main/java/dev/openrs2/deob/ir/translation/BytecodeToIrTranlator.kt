package dev.openrs2.deob.ir.translation

import dev.openrs2.deob.ir.Method
import dev.openrs2.deob.ir.translation.decompiler.IrAnalyzer
import dev.openrs2.deob.ir.translation.decompiler.IrInterpreter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class BytecodeToIrTranlator {
    fun decompile(owner: ClassNode, method: MethodNode): Method {
        val irAnalyzer = IrAnalyzer(IrInterpreter())
        val entryBlock = irAnalyzer.decode(owner.name, method)

        return Method(owner, method, entryBlock)
    }
}
