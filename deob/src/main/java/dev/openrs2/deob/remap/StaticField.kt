package dev.openrs2.deob.remap

import org.objectweb.asm.tree.AbstractInsnNode

class StaticField(val owner: String, val initializer: List<AbstractInsnNode>?)
