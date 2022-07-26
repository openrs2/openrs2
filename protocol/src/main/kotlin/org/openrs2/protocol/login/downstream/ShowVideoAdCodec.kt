package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class ShowVideoAdCodec : EmptyPacketCodec<LoginResponse.ShowVideoAd>(
    packet = LoginResponse.ShowVideoAd,
    opcode = 1
)
