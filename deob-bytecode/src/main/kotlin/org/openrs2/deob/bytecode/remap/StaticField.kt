package org.openrs2.deob.bytecode.remap

import org.objectweb.asm.tree.AbstractInsnNode

public class StaticField(public val owner: String, public val initializer: List<AbstractInsnNode>?)
