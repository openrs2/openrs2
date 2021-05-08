package org.openrs2.buffer.generator

import kotlin.reflect.KClass

public enum class IntType(
    public val width: Int,
    private val signedReadType: KClass<*>,
    private val unsignedReadType: KClass<*>,
    public val writeType: KClass<*>
) {
    BYTE(1, Byte::class, Short::class, Int::class),
    SHORT(2, Short::class, Int::class, Int::class),
    INT(4, Int::class, Long::class, Int::class);

    public val prettyName: String = name.lowercase().capitalize()

    public fun getReadType(signedness: Signedness): KClass<*> {
        return if (signedness == Signedness.SIGNED) {
            signedReadType
        } else {
            unsignedReadType
        }
    }
}
