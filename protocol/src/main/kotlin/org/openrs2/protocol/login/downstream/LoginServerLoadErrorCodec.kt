package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class LoginServerLoadErrorCodec : EmptyPacketCodec<LoginResponse.LoginServerLoadError>(
    packet = LoginResponse.LoginServerLoadError,
    opcode = 24
)
