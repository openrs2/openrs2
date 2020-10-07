package org.openrs2.deob

import org.openrs2.asm.MemberRef
import org.openrs2.util.collect.DisjointSet

public data class ArgPartition(val method: DisjointSet.Partition<MemberRef>, val index: Int)
