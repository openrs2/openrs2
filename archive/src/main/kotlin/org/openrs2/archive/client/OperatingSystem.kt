package org.openrs2.archive.client

import io.ktor.http.ContentType

public enum class OperatingSystem {
    INDEPENDENT,
    WINDOWS,
    MACOS,
    LINUX,
    SOLARIS;

    public fun getPrefix(): String {
        return when (this) {
            INDEPENDENT -> throw IllegalArgumentException()
            WINDOWS -> ""
            else -> "lib"
        }
    }

    public fun getExtension(): String {
        return when (this) {
            INDEPENDENT -> throw IllegalArgumentException()
            WINDOWS -> "dll"
            MACOS -> "dylib"
            LINUX, SOLARIS -> "so"
        }
    }

    public fun getContentType(): ContentType {
        return when (this) {
            INDEPENDENT -> throw IllegalArgumentException()
            WINDOWS -> PE
            MACOS -> MACHO
            LINUX, SOLARIS -> ELF_SHARED
        }
    }

    private companion object {
        private val ELF_SHARED = ContentType("application", "x-sharedlib")
        private val MACHO = ContentType("application", "x-mach-binary")
        private val PE = ContentType("application", "vnd.microsoft.portable-executable")
    }
}
