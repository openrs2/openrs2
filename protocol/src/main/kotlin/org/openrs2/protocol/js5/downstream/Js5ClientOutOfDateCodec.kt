package org.openrs2.protocol.js5.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class Js5ClientOutOfDateCodec : EmptyPacketCodec<Js5LoginResponse.ClientOutOfDate>(
    packet = Js5LoginResponse.ClientOutOfDate,
    opcode = 6
)
