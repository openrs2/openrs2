package org.openrs2.deob.bytecode

import org.openrs2.asm.MemberRef

public data class ArgRef(val method: MemberRef, val index: Int)
