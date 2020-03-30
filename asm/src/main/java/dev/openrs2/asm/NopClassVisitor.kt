package dev.openrs2.asm

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes

object NopClassVisitor : ClassVisitor(Opcodes.ASM8)
