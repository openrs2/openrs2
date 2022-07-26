package org.openrs2.protocol.js5.downstream

import org.openrs2.protocol.Packet

public sealed class Js5LoginResponse : Packet {
    public object Ok : Js5LoginResponse()
    public object ClientOutOfDate : Js5LoginResponse()
    public object ServerFull : Js5LoginResponse()
    public object IpLimit : Js5LoginResponse()
}
