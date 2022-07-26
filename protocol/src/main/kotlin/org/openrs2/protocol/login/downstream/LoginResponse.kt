package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.Packet

public sealed class LoginResponse : Packet {
    public object ClientOutOfDate : LoginResponse()
    public object ServerFull : LoginResponse()
    public object IpLimit : LoginResponse()
}
