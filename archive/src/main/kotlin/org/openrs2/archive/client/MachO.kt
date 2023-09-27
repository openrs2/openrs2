package org.openrs2.archive.client

import io.netty.buffer.ByteBuf
import org.openrs2.buffer.readString

public data class MachO(
    public val architecture: Architecture,
    public val symbols: Set<String>,
) {
    public companion object {
        private const val MACHO_UNIVERSAL = 0xCAFEBABE.toInt()
        private const val MACHO32BE = 0xFEEDFACE.toInt()
        private const val MACHO32LE = 0xCEFAEDFE.toInt()
        private const val MACHO64BE = 0xFEEDFACF.toInt()
        private const val MACHO64LE = 0xCFFAEDFE.toInt()

        private const val CPU_TYPE_X86 = 0x7
        private const val CPU_TYPE_AMD64 = 0x1000007
        private const val CPU_TYPE_POWERPC = 0x12

        private const val COMMAND_SYMTAB = 0x2

        public fun parse(buf: ByteBuf): MachO {
            val magic = buf.getInt(buf.readerIndex())
            return if (magic == MACHO_UNIVERSAL) {
                parseFat(buf)
            } else {
                parseMachO(buf)
            }
        }

        private fun parseFat(buf: ByteBuf): MachO {
            buf.skipBytes(4)

            val symbols = mutableSetOf<String>()
            val count = buf.readInt()

            for (i in 0 until count) {
                buf.skipBytes(8)

                val offset = buf.readInt()
                val size = buf.readInt()

                buf.skipBytes(4)

                symbols += parseMachO(buf.slice(offset, size)).symbols
            }

            return MachO(Architecture.UNIVERSAL, symbols)
        }

        private fun parseMachO(buf: ByteBuf): MachO {
            val magic = buf.readInt()
            require(magic == MACHO32BE || magic == MACHO32LE || magic == MACHO64BE || magic == MACHO64LE)

            val big = magic == MACHO32BE || magic == MACHO64BE
            val x64 = magic == MACHO64LE || magic == MACHO64BE

            val arch = when (if (big) buf.readInt() else buf.readIntLE()) {
                CPU_TYPE_X86 -> Architecture.X86
                CPU_TYPE_AMD64 -> Architecture.AMD64
                CPU_TYPE_POWERPC -> Architecture.POWERPC
                else -> throw IllegalArgumentException()
            }

            buf.skipBytes(4) // cpuSubType
            buf.skipBytes(4) // fileType

            val nCmds = if (big) buf.readInt() else buf.readIntLE()

            buf.skipBytes(4) // sizeOfCmds
            buf.skipBytes(4) // flags

            if (x64) {
                buf.skipBytes(4) // reserved
            }

            val symbols = parseCommands(buf, big, nCmds)

            return MachO(arch, symbols)
        }

        private fun parseCommands(buf: ByteBuf, big: Boolean, count: Int): Set<String> {
            for (i in 0 until count) {
                val base = buf.readerIndex()

                val command = if (big) buf.readInt() else buf.readIntLE()
                val size = if (big) buf.readInt() else buf.readIntLE()

                if (command == COMMAND_SYMTAB) {
                    buf.skipBytes(8)

                    val strOff = if (big) buf.readInt() else buf.readIntLE()
                    val strSize = if (big) buf.readInt() else buf.readIntLE()

                    return parseStringTable(buf.slice(strOff, strSize))
                }

                buf.readerIndex(base + size)
            }

            return emptySet()
        }

        private fun parseStringTable(buf: ByteBuf): Set<String> {
            return buildSet {
                while (buf.isReadable) {
                    val str = buf.readString(Charsets.US_ASCII)
                    if (str.isNotEmpty()) {
                        add(str)
                    }
                }
            }
        }
    }
}
