package org.openrs2.protocol.login.upstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class InitJaggrabConnectionCodec : EmptyPacketCodec<LoginRequest.InitJaggrabConnection>(
    packet = LoginRequest.InitJaggrabConnection,
    opcode = 17
)
