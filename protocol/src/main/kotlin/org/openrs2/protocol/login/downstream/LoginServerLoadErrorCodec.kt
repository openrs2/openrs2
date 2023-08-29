package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class LoginServerLoadErrorCodec : EmptyPacketCodec<LoginResponse.LoginServerLoadError>(
    packet = LoginResponse.LoginServerLoadError,
    opcode = 24
)
