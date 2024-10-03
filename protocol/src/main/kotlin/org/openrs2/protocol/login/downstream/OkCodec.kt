package org.openrs2.protocol.login.downstream

import io.netty.buffer.ByteBuf
import jakarta.inject.Singleton
import org.openrs2.crypto.StreamCipher
import org.openrs2.protocol.FixedPacketCodec

@Singleton
public class OkCodec : FixedPacketCodec<LoginResponse.Ok>(
    type = LoginResponse.Ok::class.java,
    opcode = 2,
    length = 11
) {
    override fun decode(
        input: ByteBuf,
        cipher: StreamCipher
    ): LoginResponse.Ok {
        val staffModLevel = input.readUnsignedByte().toInt()
        val playerModLevel = input.readUnsignedByte().toInt()
        val playerUnderage = input.readBoolean()
        val parentalChatConsent = input.readBoolean()
        val parentalAdvertConsent = input.readBoolean()
        val mapQuickChat = input.readBoolean()
        val recordMouseMovement = input.readBoolean()
        val playerId = input.readUnsignedShort().toInt()
        val playerMember = input.readBoolean()
        val mapMembers = input.readBoolean()

        return LoginResponse.Ok(
            staffModLevel,
            playerModLevel,
            playerUnderage,
            parentalChatConsent,
            parentalAdvertConsent,
            mapQuickChat,
            recordMouseMovement,
            playerId,
            playerMember,
            mapMembers
        )
    }

    override fun encode(
        input: LoginResponse.Ok,
        output: ByteBuf,
        cipher: StreamCipher
    ) {
        output.writeByte(input.staffModLevel)
        output.writeByte(input.playerModLevel)
        output.writeBoolean(input.playerUnderage)
        output.writeBoolean(input.parentalChatConsent)
        output.writeBoolean(input.parentalAdvertConsent)
        output.writeBoolean(input.mapQuickChat)
        output.writeBoolean(input.recordMouseMovement)
        output.writeShort(input.playerId)
        output.writeBoolean(input.playerMember)
        output.writeBoolean(input.mapMembers)
    }
}
