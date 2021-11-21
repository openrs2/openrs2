package org.openrs2.cache

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import java.io.Closeable
import java.io.FileNotFoundException
import java.io.Flushable
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * A low-level interface for reading and writing raw groups directly to and
 * from a collection of JS5 archives.
 */
public interface Store : Flushable, Closeable {
    /**
     * Checks whether an archive exists.
     * @param archive the archive ID.
     * @return `true` if so, `false` otherwise.
     * @throws IllegalArgumentException if the archive ID is out of bounds.
     * @throws IOException if an underlying I/O error occurs.
     */
    public fun exists(archive: Int): Boolean

    /**
     * Checks whether a group exists.
     * @param archive the archive ID.
     * @param group the group ID.
     * @return `true` if both the group and archive exist, `false` otherwise.
     * @throws IllegalArgumentException if the archive or group ID is out of
     * bounds.
     * @throws IOException if an underlying I/O error occurs.
     */
    public fun exists(archive: Int, group: Int): Boolean

    /**
     * Lists all archives in the store.
     * @return a sorted list of archive IDs.
     * @throws IOException if an underlying I/O error occurs.
     */
    public fun list(): List<Int>

    /**
     * Lists all groups in an archive.
     * @param archive the archive ID.
     * @return a sorted list of group IDs.
     * @throws IllegalArgumentException if the archive or group ID is out of
     * bounds.
     * @throws FileNotFoundException if the archive does not exist.
     * @throws IOException if an underlying I/O error occurs.
     */
    public fun list(archive: Int): List<Int>

    /**
     * Creates an archive. Does nothing if the archive already exists.
     * @param archive the archive ID.
     * @throws IllegalArgumentException if the archive ID is out of bounds.
     * @throws IOException if an underlying I/O error occurs.
     */
    public fun create(archive: Int)

    /**
     * Reads a group.
     *
     * This method allocates and returns a new [ByteBuf]. It is the caller's
     * responsibility to release the [ByteBuf].
     * @param archive the archive ID.
     * @param group the group ID.
     * @return the contents of the group.
     * @throws IllegalArgumentException if the archive or group ID is out of
     * bounds.
     * @throws FileNotFoundException if the archive or group does not exist.
     * @throws StoreCorruptException if the store is corrupt.
     * @throws IOException if an underlying I/O error occurs.
     */
    public fun read(archive: Int, group: Int): ByteBuf

    /**
     * Writes a group. If the archive does not exist, it is created first. If a
     * group with the same ID already exists, it is overwritten.
     *
     * This method consumes the readable portion of the given [ByteBuf]. It
     * does not modify the [ByteBuf]'s reference count.
     * @param archive the archive ID.
     * @param group the group ID.
     * @param buf the new contents of the group.
     * @throws IllegalArgumentException if the archive or group ID is out of
     * bounds, or if [buf] is too long (see [MAX_GROUP_SIZE]).
     * @throws StoreFullException if the store is full.
     * @throws IOException if an underlying I/O error occurs.
     */
    public fun write(archive: Int, group: Int, buf: ByteBuf)

    /**
     * Deletes an archive and all groups contained inside it. Does nothing if
     * the archive does not exist.
     * @param archive the archive ID.
     * @throws IllegalArgumentException if the archive ID is out of bounds.
     * @throws IOException if an underlying I/O error occurs.
     */
    public fun remove(archive: Int)

    /**
     * Deletes a group. Does nothing if the archive or group does not exist.
     * @param archive the archive ID.
     * @param group the group ID.
     * @throws IllegalArgumentException if the archive or group ID is out of
     * bounds.
     * @throws IOException if an underlying I/O error occurs.
     */
    public fun remove(archive: Int, group: Int)

    public companion object {
        /**
         * The maximum archive ID.
         */
        public const val MAX_ARCHIVE: Int = 255

        /**
         * The maximum length of a group's contents in bytes.
         */
        public const val MAX_GROUP_SIZE: Int = (1 shl 24) - 1

        /**
         * The ID of the ARCHIVESET archive.
         */
        public const val ARCHIVESET: Int = 255

        /**
         * Opens a [Store], automatically detecting the type based on the
         * presence or absence of the `main_file_cache.dat2` file.
         * @param root the store's root directory.
         * @param alloc the [ByteBufAllocator] used to allocate buffers for
         * groups read from the [Store] and for temporary internal use.
         * @throws IOException if an underlying I/O error occurs.
         */
        public fun open(root: Path, alloc: ByteBufAllocator = ByteBufAllocator.DEFAULT): Store {
            val hasDataFile = Files.isRegularFile(DiskStore.dataPath(root))
            val hasLegacyDataFile = Files.isRegularFile(DiskStore.legacyDataPath(root))
            return if (hasDataFile || hasLegacyDataFile) {
                DiskStore.open(root, alloc)
            } else {
                FlatFileStore.open(root, alloc)
            }
        }
    }
}
