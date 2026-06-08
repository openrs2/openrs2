package org.openrs2.cache

public class Js5IndexConfig {

    public var protocol: Js5Protocol = PROTOCOL

    public var version: Int = VERSION

    public var hasNames: Boolean = HAS_NAMES

    public var hasDigests: Boolean = HAS_DIGESTS

    public var hasLengths: Boolean = HAS_LENGTHS

    public var hasUncompressedChecksums: Boolean = HAS_UNCOMPRESSED_CHECKSUMS

    public companion object Defaults {

        public val PROTOCOL: Js5Protocol = Js5Protocol.VERSIONED

        public val VERSION: Int = 0

        public val HAS_NAMES: Boolean = false

        public val HAS_DIGESTS: Boolean = false

        public val HAS_LENGTHS: Boolean = false

        public val HAS_UNCOMPRESSED_CHECKSUMS: Boolean = false

    }

}
