package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class LoginServerOfflineCodec : EmptyPacketCodec<LoginResponse.LoginServerOffline>(
    packet = LoginResponse.LoginServerOffline,
    opcode = 8
)
