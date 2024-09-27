package org.openrs2.cache.midi

import io.netty.buffer.ByteBuf
import org.openrs2.buffer.Arena
import org.openrs2.buffer.readVarInt
import org.openrs2.buffer.writeVarInt
import javax.sound.midi.MetaMessage
import javax.sound.midi.MidiEvent
import javax.sound.midi.Sequence
import javax.sound.midi.ShortMessage

public object Song {
    private const val NOTE_ON = 0
    private const val NOTE_OFF = 1
    private const val CONTROL_CHANGE = 2
    private const val PITCH_WHEEL_CHANGE = 3
    private const val CHANNEL_PRESSURE_CHANGE = 4
    private const val KEY_PRESSURE_CHANGE = 5
    private const val PROGRAM_CHANGE = 6
    private const val END_OF_TRACK = 7
    private const val SET_TEMPO = 23

    private const val BANK_SELECT_MSB = 0
    private const val BANK_SELECT_LSB = 32
    private const val MODULATION_WHEEL_MSB = 1
    private const val MODULATION_WHEEL_LSB = 33
    private const val CHANNEL_VOLUME_MSB = 7
    private const val CHANNEL_VOLUME_LSB = 39
    private const val PAN_MSB = 10
    private const val PAN_LSB = 42
    private const val NON_REGISTERED_MSB = 99
    private const val NON_REGISTERED_LSB = 98
    private const val REGISTERED_MSB = 101
    private const val REGISTERED_LSB = 100
    private const val DAMPER = 64
    private const val PORTAMENTO = 65
    private const val ALL_SOUND_OFF = 120
    private const val RESET_CONTROLLERS = 121
    private const val ALL_NOTES_OFF = 123

    private const val META_END_OF_TRACK = 47
    private const val META_SET_TEMPO = 81

    private const val CONTROLLERS = 128

    public fun read(buf: ByteBuf): Sequence {
        buf.markReaderIndex()

        val tracks = buf.getUnsignedByte(buf.writerIndex() - 3).toInt()
        val division = buf.getUnsignedShort(buf.writerIndex() - 2)

        if (division and 0x8000 != 0) {
            throw IllegalArgumentException("SMPTE unsupported")
        }

        val sequence = Sequence(Sequence.PPQ, division and 0x7FFF)

        var events = 0
        var tempoChanges = 0
        var noteOnEvents = 0
        var noteOffEvents = 0
        var controllerEvents = 0
        var pitchWheelEvents = 0
        var channelPressureEvents = 0
        var keyPressureEvents = 0
        var bankSelectEvents = 0

        for (i in 0 until tracks) {
            while (true) {
                events++

                val statusAndChannel = buf.readUnsignedByte().toInt()
                if (statusAndChannel == END_OF_TRACK) {
                    break
                } else if (statusAndChannel == SET_TEMPO) {
                    tempoChanges++
                    continue
                }

                when (val status = statusAndChannel and 0xF) {
                    NOTE_ON -> noteOnEvents++
                    NOTE_OFF -> noteOffEvents++
                    CONTROL_CHANGE -> controllerEvents++
                    PITCH_WHEEL_CHANGE -> pitchWheelEvents++
                    CHANNEL_PRESSURE_CHANGE -> channelPressureEvents++
                    KEY_PRESSURE_CHANGE -> keyPressureEvents++
                    PROGRAM_CHANGE -> bankSelectEvents++
                    else -> throw IllegalArgumentException("Unsupported status: $status")
                }
            }
        }

        val deltaTimeBuf = buf.slice()
        for (i in 0 until events) {
            buf.readVarInt()
        }

        var modulationWheelMsbEvents = 0
        var modulationWheelLsbEvents = 0
        var channelVolumeMsbEvents = 0
        var channelVolumeLsbEvents = 0
        var panMsbEvents = 0
        var panLsbEvents = 0
        var nonRegisteredMsbEvents = 0
        var nonRegisteredLsbEvents = 0
        var registeredMsbEvents = 0
        var registeredLsbEvents = 0
        var otherKnownControllerEvents = 0
        var unknownControllerEvents = 0

        val controllerBuf = buf.slice()

        var controller = 0
        for (i in 0 until controllerEvents) {
            controller = (controller + buf.readUnsignedByte()) and 0x7F

            when (controller) {
                BANK_SELECT_MSB, BANK_SELECT_LSB -> bankSelectEvents++
                MODULATION_WHEEL_MSB -> modulationWheelMsbEvents++
                MODULATION_WHEEL_LSB -> modulationWheelLsbEvents++
                CHANNEL_VOLUME_MSB -> channelVolumeMsbEvents++
                CHANNEL_VOLUME_LSB -> channelVolumeLsbEvents++
                PAN_MSB -> panMsbEvents++
                PAN_LSB -> panLsbEvents++
                NON_REGISTERED_MSB -> nonRegisteredMsbEvents++
                NON_REGISTERED_LSB -> nonRegisteredLsbEvents++
                REGISTERED_MSB -> registeredMsbEvents++
                REGISTERED_LSB -> registeredLsbEvents++
                DAMPER, PORTAMENTO, ALL_SOUND_OFF, RESET_CONTROLLERS, ALL_NOTES_OFF -> otherKnownControllerEvents++
                else -> unknownControllerEvents++
            }
        }

        val otherKnownControllerBuf = buf.readSlice(otherKnownControllerEvents)
        val keyPressureBuf = buf.readSlice(keyPressureEvents)
        val channelPressureBuf = buf.readSlice(channelPressureEvents)
        val pitchWheelMsbBuf = buf.readSlice(pitchWheelEvents)
        val modulationWheelMsbBuf = buf.readSlice(modulationWheelMsbEvents)
        val channelVolumeMsbBuf = buf.readSlice(channelVolumeMsbEvents)
        val panMsbBuf = buf.readSlice(panMsbEvents)
        val keyBuf = buf.readSlice(noteOnEvents + noteOffEvents + keyPressureEvents)
        val onVelocityBuf = buf.readSlice(noteOnEvents)
        val unknownControllerBuf = buf.readSlice(unknownControllerEvents)
        val offVelocityBuf = buf.readSlice(noteOffEvents)
        val modulationWheelLsbBuf = buf.readSlice(modulationWheelLsbEvents)
        val channelVolumeLsbBuf = buf.readSlice(channelVolumeLsbEvents)
        val panLsbBuf = buf.readSlice(panLsbEvents)
        val bankSelectBuf = buf.readSlice(bankSelectEvents)
        val pitchWheelLsbBuf = buf.readSlice(pitchWheelEvents)
        val nonRegisteredMsbBuf = buf.readSlice(nonRegisteredMsbEvents)
        val nonRegisteredLsbBuf = buf.readSlice(nonRegisteredLsbEvents)
        val registeredMsbBuf = buf.readSlice(registeredMsbEvents)
        val registeredLsbBuf = buf.readSlice(registeredLsbEvents)
        val tempoBuf = buf.readSlice(tempoChanges * 3)

        // check only the three trailer bytes are remaining
        assert(buf.readableBytes() == 3)

        buf.resetReaderIndex()

        var channel = 0
        var key = 0
        var onVelocity = 0
        var offVelocity = 0
        controller = 0
        val values = IntArray(CONTROLLERS)
        var pitchWheel = 0
        var channelPressure = 0
        var keyPressure = 0

        for (i in 0 until tracks) {
            val track = sequence.createTrack()

            var time = 0L

            while (true) {
                time += deltaTimeBuf.readVarInt()

                val statusAndChannel = buf.readUnsignedByte().toInt()
                if (statusAndChannel == END_OF_TRACK) {
                    track.add(MidiEvent(MetaMessage(META_END_OF_TRACK, null, 0), time))
                    break
                } else if (statusAndChannel == SET_TEMPO) {
                    val tempo = ByteArray(3)
                    tempoBuf.readBytes(tempo)
                    track.add(MidiEvent(MetaMessage(META_SET_TEMPO, tempo, tempo.size), time))
                    continue
                }

                val status = statusAndChannel and 0xF
                channel = channel xor (statusAndChannel shr 4)

                val message = when (status) {
                    NOTE_ON -> {
                        key = (key + keyBuf.readUnsignedByte().toInt()) and 0x7F
                        onVelocity = (onVelocity + onVelocityBuf.readUnsignedByte().toInt()) and 0x7F
                        ShortMessage(ShortMessage.NOTE_ON, channel, key, onVelocity)
                    }

                    NOTE_OFF -> {
                        key = (key + keyBuf.readUnsignedByte().toInt()) and 0x7F
                        offVelocity = (offVelocity + offVelocityBuf.readUnsignedByte().toInt()) and 0x7F
                        ShortMessage(ShortMessage.NOTE_OFF, channel, key, offVelocity)
                    }

                    CONTROL_CHANGE -> {
                        controller = (controller + controllerBuf.readUnsignedByte()) and 0x7F

                        val valueDelta = when (controller) {
                            BANK_SELECT_MSB, BANK_SELECT_LSB -> bankSelectBuf.readUnsignedByte().toInt()
                            MODULATION_WHEEL_MSB -> modulationWheelMsbBuf.readUnsignedByte().toInt()
                            MODULATION_WHEEL_LSB -> modulationWheelLsbBuf.readUnsignedByte().toInt()
                            CHANNEL_VOLUME_MSB -> channelVolumeMsbBuf.readUnsignedByte().toInt()
                            CHANNEL_VOLUME_LSB -> channelVolumeLsbBuf.readUnsignedByte().toInt()
                            PAN_MSB -> panMsbBuf.readUnsignedByte().toInt()
                            PAN_LSB -> panLsbBuf.readUnsignedByte().toInt()
                            NON_REGISTERED_MSB -> nonRegisteredMsbBuf.readUnsignedByte().toInt()
                            NON_REGISTERED_LSB -> nonRegisteredLsbBuf.readUnsignedByte().toInt()
                            REGISTERED_MSB -> registeredMsbBuf.readUnsignedByte().toInt()
                            REGISTERED_LSB -> registeredLsbBuf.readUnsignedByte().toInt()
                            DAMPER, PORTAMENTO, ALL_SOUND_OFF, RESET_CONTROLLERS, ALL_NOTES_OFF ->
                                otherKnownControllerBuf.readUnsignedByte().toInt()

                            else -> unknownControllerBuf.readUnsignedByte().toInt()
                        }

                        val value = values[controller] + valueDelta
                        values[controller] = value
                        ShortMessage(ShortMessage.CONTROL_CHANGE, channel, controller, value and 0x7F)
                    }

                    PITCH_WHEEL_CHANGE -> {
                        pitchWheel += pitchWheelLsbBuf.readUnsignedByte().toInt()
                        pitchWheel += (pitchWheelMsbBuf.readUnsignedByte().toInt() shl 7)
                        pitchWheel = pitchWheel and 0x3FFF
                        ShortMessage(ShortMessage.PITCH_BEND, channel, pitchWheel and 0x7F, pitchWheel shr 7)
                    }

                    CHANNEL_PRESSURE_CHANGE -> {
                        channelPressure = (channelPressure + channelPressureBuf.readUnsignedByte().toInt()) and 0x7F
                        ShortMessage(ShortMessage.CHANNEL_PRESSURE, channel, channelPressure, 0)
                    }

                    KEY_PRESSURE_CHANGE -> {
                        key = (key + keyBuf.readUnsignedByte().toInt()) and 0x7F
                        keyPressure = (keyPressure + keyPressureBuf.readUnsignedByte().toInt()) and 0x7F
                        ShortMessage(ShortMessage.POLY_PRESSURE, channel, key, keyPressure)
                    }

                    PROGRAM_CHANGE -> {
                        val bankSelect = bankSelectBuf.readUnsignedByte().toInt()
                        ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, bankSelect, 0)
                    }

                    else -> throw IllegalStateException()
                }

                track.add(MidiEvent(message, time))
            }
        }

        // we've consumed the entire buf (checked by an assertion above)
        buf.readerIndex(buf.writerIndex())

        return sequence
    }

    public fun write(sequence: Sequence, buf: ByteBuf) {
        if (sequence.divisionType != Sequence.PPQ) {
            throw IllegalArgumentException("SMPTE unsupported")
        }

        Arena(buf.alloc()).use { alloc ->
            val deltaTimeBuf = alloc.buffer()
            val controllerBuf = alloc.buffer()
            val otherKnownControllerBuf = alloc.buffer()
            val keyPressureBuf = alloc.buffer()
            val channelPressureBuf = alloc.buffer()
            val pitchWheelMsbBuf = alloc.buffer()
            val modulationWheelMsbBuf = alloc.buffer()
            val channelVolumeMsbBuf = alloc.buffer()
            val panMsbBuf = alloc.buffer()
            val keyBuf = alloc.buffer()
            val onVelocityBuf = alloc.buffer()
            val unknownControllerBuf = alloc.buffer()
            val offVelocityBuf = alloc.buffer()
            val modulationWheelLsbBuf = alloc.buffer()
            val channelVolumeLsbBuf = alloc.buffer()
            val panLsbBuf = alloc.buffer()
            val bankSelectBuf = alloc.buffer()
            val pitchWheelLsbBuf = alloc.buffer()
            val nonRegisteredMsbBuf = alloc.buffer()
            val nonRegisteredLsbBuf = alloc.buffer()
            val registeredMsbBuf = alloc.buffer()
            val registeredLsbBuf = alloc.buffer()
            val tempoBuf = alloc.buffer()

            var prevChannel = 0
            var prevKey = 0
            var prevOnVelocity = 0
            var prevOffVelocity = 0
            var prevController = 0
            var prevPitchWheel = 0
            var prevChannelPressure = 0
            var prevKeyPressure = 0
            val prevValues = IntArray(CONTROLLERS)

            for (track in sequence.tracks) {
                var prevTime = 0L

                for (i in 0 until track.size()) {
                    val event = track[i]

                    val time = event.tick
                    deltaTimeBuf.writeVarInt((time - prevTime).toInt())
                    prevTime = time

                    when (val message = event.message) {
                        is MetaMessage -> {
                            when (val type = message.type) {
                                META_END_OF_TRACK -> buf.writeByte(END_OF_TRACK)
                                META_SET_TEMPO -> {
                                    buf.writeByte(SET_TEMPO)
                                    require(message.data.size == 3)
                                    tempoBuf.writeBytes(message.data)
                                }

                                else -> throw IllegalArgumentException("Unsupported meta type: $type")
                            }
                        }

                        is ShortMessage -> {
                            val command = message.status and 0xF0
                            val channel = message.status and 0xF

                            val status = when (command) {
                                ShortMessage.NOTE_ON -> NOTE_ON
                                ShortMessage.NOTE_OFF -> NOTE_OFF
                                ShortMessage.CONTROL_CHANGE -> CONTROL_CHANGE
                                ShortMessage.PITCH_BEND -> PITCH_WHEEL_CHANGE
                                ShortMessage.CHANNEL_PRESSURE -> CHANNEL_PRESSURE_CHANGE
                                ShortMessage.POLY_PRESSURE -> KEY_PRESSURE_CHANGE
                                ShortMessage.PROGRAM_CHANGE -> PROGRAM_CHANGE
                                else -> throw IllegalArgumentException("Unsupported command: $command")
                            }

                            buf.writeByte(((channel xor prevChannel) shl 4) or status)
                            prevChannel = channel

                            if (
                                command == ShortMessage.NOTE_ON ||
                                command == ShortMessage.NOTE_OFF ||
                                command == ShortMessage.POLY_PRESSURE
                            ) {
                                val key = message.data1
                                keyBuf.writeByte((key - prevKey) and 0x7F)
                                prevKey = key
                            }

                            when (command) {
                                ShortMessage.NOTE_ON -> {
                                    val onVelocity = message.data2
                                    onVelocityBuf.writeByte((onVelocity - prevOnVelocity) and 0x7F)
                                    prevOnVelocity = onVelocity
                                }

                                ShortMessage.NOTE_OFF -> {
                                    val offVelocity = message.data2
                                    offVelocityBuf.writeByte((offVelocity - prevOffVelocity) and 0x7F)
                                    prevOffVelocity = offVelocity
                                }

                                ShortMessage.CONTROL_CHANGE -> {
                                    val controller = message.data1
                                    controllerBuf.writeByte((controller - prevController) and 0x7F)
                                    prevController = controller

                                    val value = message.data2
                                    val valueDelta = (value - prevValues[controller]) and 0x7F

                                    when (controller) {
                                        BANK_SELECT_MSB, BANK_SELECT_LSB -> bankSelectBuf.writeByte(valueDelta)
                                        MODULATION_WHEEL_MSB -> modulationWheelMsbBuf.writeByte(valueDelta)
                                        MODULATION_WHEEL_LSB -> modulationWheelLsbBuf.writeByte(valueDelta)
                                        CHANNEL_VOLUME_MSB -> channelVolumeMsbBuf.writeByte(valueDelta)
                                        CHANNEL_VOLUME_LSB -> channelVolumeLsbBuf.writeByte(valueDelta)
                                        PAN_MSB -> panMsbBuf.writeByte(valueDelta)
                                        PAN_LSB -> panLsbBuf.writeByte(valueDelta)
                                        NON_REGISTERED_MSB -> nonRegisteredMsbBuf.writeByte(valueDelta)
                                        NON_REGISTERED_LSB -> nonRegisteredLsbBuf.writeByte(valueDelta)
                                        REGISTERED_MSB -> registeredMsbBuf.writeByte(valueDelta)
                                        REGISTERED_LSB -> registeredLsbBuf.writeByte(valueDelta)
                                        DAMPER, PORTAMENTO, ALL_SOUND_OFF, RESET_CONTROLLERS, ALL_NOTES_OFF ->
                                            otherKnownControllerBuf.writeByte(valueDelta)

                                        else -> unknownControllerBuf.writeByte(valueDelta)
                                    }

                                    prevValues[controller] = value
                                }

                                ShortMessage.PITCH_BEND -> {
                                    val pitchWheel = message.data1 or (message.data2 shl 7)
                                    val pitchWheelDelta = (pitchWheel - prevPitchWheel) and 0x3FFF
                                    pitchWheelLsbBuf.writeByte(pitchWheelDelta and 0x7F)
                                    pitchWheelMsbBuf.writeByte(pitchWheelDelta shr 7)
                                    prevPitchWheel = pitchWheel
                                }

                                ShortMessage.CHANNEL_PRESSURE -> {
                                    val channelPressure = message.data1
                                    channelPressureBuf.writeByte((channelPressure - prevChannelPressure) and 0x7F)
                                    prevChannelPressure = channelPressure
                                }

                                ShortMessage.POLY_PRESSURE -> {
                                    val keyPressure = message.data2
                                    keyPressureBuf.writeByte((keyPressure - prevKeyPressure) and 0x7F)
                                    prevKeyPressure = keyPressure
                                }

                                ShortMessage.PROGRAM_CHANGE -> {
                                    bankSelectBuf.writeByte(message.data1)
                                }

                                else -> throw IllegalStateException()
                            }
                        }

                        else -> throw IllegalArgumentException("Unsupported message type: ${message.javaClass.name}")
                    }
                }
            }

            buf.writeBytes(deltaTimeBuf)
            buf.writeBytes(controllerBuf)
            buf.writeBytes(otherKnownControllerBuf)
            buf.writeBytes(keyPressureBuf)
            buf.writeBytes(channelPressureBuf)
            buf.writeBytes(pitchWheelMsbBuf)
            buf.writeBytes(modulationWheelMsbBuf)
            buf.writeBytes(channelVolumeMsbBuf)
            buf.writeBytes(panMsbBuf)
            buf.writeBytes(keyBuf)
            buf.writeBytes(onVelocityBuf)
            buf.writeBytes(unknownControllerBuf)
            buf.writeBytes(offVelocityBuf)
            buf.writeBytes(modulationWheelLsbBuf)
            buf.writeBytes(channelVolumeLsbBuf)
            buf.writeBytes(panLsbBuf)
            buf.writeBytes(bankSelectBuf)
            buf.writeBytes(pitchWheelLsbBuf)
            buf.writeBytes(nonRegisteredMsbBuf)
            buf.writeBytes(nonRegisteredLsbBuf)
            buf.writeBytes(registeredMsbBuf)
            buf.writeBytes(registeredLsbBuf)
            buf.writeBytes(tempoBuf)
        }

        buf.writeByte(sequence.tracks.size)
        buf.writeShort(sequence.resolution and 0x7FFF)
    }
}
