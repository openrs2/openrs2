package org.openrs2.archive.client

import io.ktor.http.ContentType

public enum class ArtifactFormat {
    CAB,
    JAR,
    NATIVE,
    PACK200,
    PACKCLASS;

    public fun getPrefix(os: OperatingSystem): String {
        return when (this) {
            NATIVE -> os.getPrefix()
            else -> ""
        }
    }

    public fun getExtension(os: OperatingSystem): String {
        return when (this) {
            CAB -> "cab"
            JAR -> "jar"
            NATIVE -> os.getExtension()
            PACK200 -> "pack200"
            PACKCLASS -> "js5"
        }
    }

    public fun getContentType(os: OperatingSystem): ContentType {
        return when (this) {
            CAB -> CAB_MIME_TYPE
            JAR -> JAR_MIME_TYPE
            NATIVE -> os.getContentType()
            PACK200, PACKCLASS -> ContentType.Application.OctetStream
        }
    }

    public fun isJar(): Boolean {
        return this != NATIVE
    }

    private companion object {
        private val CAB_MIME_TYPE = ContentType("application", "vnd.ms-cab-compressed")
        private val JAR_MIME_TYPE = ContentType("application", "java-archive")
    }
}
