package org.openrs2.asm

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes

public object NopClassVisitor : ClassVisitor(Opcodes.ASM9)
