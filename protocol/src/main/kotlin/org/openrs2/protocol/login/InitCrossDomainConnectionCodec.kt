package org.openrs2.protocol.login

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class InitCrossDomainConnectionCodec : EmptyPacketCodec<LoginRequest.InitCrossDomainConnection>(
    opcode = 'G'.code,
    packet = LoginRequest.InitCrossDomainConnection
)
