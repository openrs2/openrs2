package dev.openrs2.deob

import dev.openrs2.asm.MemberRef
import dev.openrs2.util.collect.DisjointSet

data class ArgRef(val method: DisjointSet.Partition<MemberRef>, val arg: Int)
