package dev.openrs2.deob.analysis

import dev.openrs2.asm.MemberRef
import dev.openrs2.common.collect.DisjointSet

data class SourcedIntValue(val source: DisjointSet.Partition<MemberRef>, val intValue: IntValue)
