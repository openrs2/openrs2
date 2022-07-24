package org.openrs2.protocol.login

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class IpLimitCodec : EmptyPacketCodec<LoginResponse.IpLimit>(
    packet = LoginResponse.IpLimit,
    opcode = 9
)
