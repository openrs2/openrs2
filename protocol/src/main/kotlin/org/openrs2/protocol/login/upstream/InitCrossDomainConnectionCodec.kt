package org.openrs2.protocol.login.upstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class InitCrossDomainConnectionCodec : EmptyPacketCodec<LoginRequest.InitCrossDomainConnection>(
    opcode = 'G'.code,
    packet = LoginRequest.InitCrossDomainConnection
)
