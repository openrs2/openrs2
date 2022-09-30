package org.openrs2.compress.bzip2

import jnr.ffi.LibraryLoader
import jnr.ffi.LibraryOption
import jnr.ffi.Runtime
import jnr.ffi.Struct
import jnr.ffi.annotations.Direct

public interface LibBzip2 {
    public class BzStream(runtime: Runtime) : Struct(runtime) {
        public val nextIn: Pointer = Pointer()
        public val availIn: Unsigned32 = Unsigned32()
        public val totalInLo32: Unsigned32 = Unsigned32()
        public val totalInHi32: Unsigned32 = Unsigned32()

        public val nextOut: Pointer = Pointer()
        public val availOut: Unsigned32 = Unsigned32()
        public val totalOutLo32: Unsigned32 = Unsigned32()
        public val totalOutHi32: Unsigned32 = Unsigned32()

        public val state: Pointer = Pointer()

        public val alloc: Pointer = Pointer()
        public val free: Pointer = Pointer()
        public val opaque: Pointer = Pointer()
    }

    public fun BZ2_bzCompressInit(@Direct stream: BzStream, blockSize100k: Int, verbosity: Int, workFactor: Int): Int
    public fun BZ2_bzCompress(stream: BzStream, action: Int): Int
    public fun BZ2_bzCompressEnd(stream: BzStream): Int

    public fun BZ2_bzDecompressInit(@Direct stream: BzStream, blockSize100k: Int, verbosity: Int, small: Int): Int
    public fun BZ2_bzDecompress(stream: BzStream): Int
    public fun BZ2_bzDecompressEnd(stream: BzStream): Int

    public companion object {
        public const val BZ_RUN: Int = 0
        public const val BZ_FLUSH: Int = 1
        public const val BZ_FINISH: Int = 2

        public const val BZ_OK: Int = 0
        public const val BZ_RUN_OK: Int = 1
        public const val BZ_FLUSH_OK: Int = 2
        public const val BZ_FINISH_OK: Int = 3
        public const val BZ_STREAM_END: Int = 4
        public const val BZ_SEQUENCE_ERROR: Int = -1
        public const val BZ_PARAM_ERROR: Int = -2
        public const val BZ_MEM_ERROR: Int = -3
        public const val BZ_DATA_ERROR: Int = -4
        public const val BZ_DATA_ERROR_MAGIC: Int = -5
        public const val BZ_IO_ERROR: Int = -6
        public const val BZ_UNEXPECTED_EOF: Int = -7
        public const val BZ_OUTBUFF_FULL: Int = -8
        public const val BZ_CONFIG_ERROR: Int = -9

        public fun load(): LibBzip2 {
            return LibraryLoader.loadLibrary(
                LibBzip2::class.java, mapOf(
                    LibraryOption.LoadNow to true
                ), "bz2"
            )
        }
    }
}
