package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class LoginServerOfflineCodec : EmptyPacketCodec<LoginResponse.LoginServerOffline>(
    packet = LoginResponse.LoginServerOffline,
    opcode = 8
)
