package org.openrs2.deob.util.module

import org.openrs2.asm.io.JarLibraryReader
import org.openrs2.asm.io.LibraryReader
import org.openrs2.asm.io.Pack200LibraryReader

public enum class ModuleFormat(public val reader: LibraryReader) {
    JAR(JarLibraryReader),
    PACK200(Pack200LibraryReader);

    public companion object {
        public fun fromPath(name: String): ModuleFormat {
            return when (val extension = name.substringAfterLast('.', missingDelimiterValue = "")) {
                "jar" -> JAR
                "pack200" -> PACK200
                else -> error("Failed to identify module format from '$name': unrecognised extension '$extension'")
            }
        }
    }
}
