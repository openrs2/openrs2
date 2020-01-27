package dev.openrs2.asm.classpath

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.JSRInlinerAdapter

class JsrInliner(cv: ClassVisitor) : ClassVisitor(Opcodes.ASM7, cv) {
    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<String>?
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        return JSRInlinerAdapter(mv, access, name, descriptor, signature, exceptions)
    }
}