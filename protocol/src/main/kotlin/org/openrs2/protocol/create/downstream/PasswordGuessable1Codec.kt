package org.openrs2.protocol.create.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class PasswordGuessable1Codec : EmptyPacketCodec<CreateResponse.PasswordGuessable1>(
    packet = CreateResponse.PasswordGuessable1,
    opcode = 33
)
