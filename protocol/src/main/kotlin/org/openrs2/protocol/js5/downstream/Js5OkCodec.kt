package org.openrs2.protocol.js5.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class Js5OkCodec : EmptyPacketCodec<Js5LoginResponse.Ok>(
    packet = Js5LoginResponse.Ok,
    opcode = 0
)
