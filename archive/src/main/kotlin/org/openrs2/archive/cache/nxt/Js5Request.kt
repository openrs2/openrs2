package org.openrs2.archive.cache.nxt

public sealed class Js5Request {
    public data class Group(
        public val prefetch: Boolean,
        public val archive: Int,
        public val group: Int,
        public val build: Int
    ) : Js5Request()

    public data class Connected(
        public val build: Int
    ) : Js5Request()
}
