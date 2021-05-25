package org.openrs2.protocol.login

import org.openrs2.protocol.Packet

public sealed class LoginRequest : Packet {
    public data class InitJs5RemoteConnection(public val build: Int) : LoginRequest()
    public object InitJaggrabConnection : LoginRequest()
    public data class RequestWorldList(public val checksum: Int) : LoginRequest()
    public object InitCrossDomainConnection : LoginRequest()
}
