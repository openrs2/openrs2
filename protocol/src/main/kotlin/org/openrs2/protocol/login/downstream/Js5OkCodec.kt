package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class Js5OkCodec : EmptyPacketCodec<LoginResponse.Js5Ok>(
    packet = LoginResponse.Js5Ok,
    opcode = 0
)
