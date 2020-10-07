package org.openrs2.asm.filter

public interface ClassFilter {
    public fun matches(name: String): Boolean
}
