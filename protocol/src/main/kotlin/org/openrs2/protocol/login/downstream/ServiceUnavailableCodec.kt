package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class ServiceUnavailableCodec : EmptyPacketCodec<LoginResponse.ServiceUnavailable>(
    packet = LoginResponse.ServiceUnavailable,
    opcode = 27
)
