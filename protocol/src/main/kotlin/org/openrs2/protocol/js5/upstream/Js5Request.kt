package org.openrs2.protocol.js5.upstream

public sealed class Js5Request {
    public data class Group(
        public val prefetch: Boolean,
        public val archive: Int,
        public val group: Int
    ) : Js5Request()

    public object LoggedIn : Js5Request()
    public object LoggedOut : Js5Request()
    public data class Rekey(public val key: Int) : Js5Request()
    public object Connected : Js5Request()
    public object Disconnect : Js5Request()
}
