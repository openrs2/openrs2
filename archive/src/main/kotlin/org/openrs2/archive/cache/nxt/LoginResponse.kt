package org.openrs2.archive.cache.nxt

import org.openrs2.protocol.Packet

public sealed class LoginResponse : Packet {
    public object Js5Ok : LoginResponse()
    public object ClientOutOfDate : LoginResponse()
}
