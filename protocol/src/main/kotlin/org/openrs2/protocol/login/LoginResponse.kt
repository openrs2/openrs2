package org.openrs2.protocol.login

import org.openrs2.protocol.Packet

public sealed class LoginResponse : Packet {
    public object Js5Ok : LoginResponse()
    public object ClientOutOfDate : LoginResponse()
    public object ServerFull : LoginResponse()
    public object IpLimit : LoginResponse()
}
