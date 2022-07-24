package org.openrs2.protocol.login

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class InitJaggrabConnectionCodec : EmptyPacketCodec<LoginRequest.InitJaggrabConnection>(
    packet = LoginRequest.InitJaggrabConnection,
    opcode = 17
)
