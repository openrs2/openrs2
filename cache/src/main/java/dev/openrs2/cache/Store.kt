package dev.openrs2.cache

import io.netty.buffer.ByteBuf
import java.io.Closeable
import java.io.FileNotFoundException
import java.io.Flushable
import java.io.IOException

/**
 * A low-level interface for reading and writing raw groups directly to and
 * from a collection of JS5 archives.
 */
interface Store : Flushable, Closeable {
    /**
     * Checks whether an archive exists.
     * @param archive the archive ID.
     * @return `true` if so, `false` otherwise.
     * @throws IllegalArgumentException if the archive ID is out of bounds.
     * @throws IOException if an underlying I/O error occurs.
     */
    fun exists(archive: Int): Boolean

    /**
     * Checks whether a group exists.
     * @param archive the archive ID.
     * @param group the group ID.
     * @return `true` if both the group and archive exist, `false` otherwise.
     * @throws IllegalArgumentException if the archive or group ID is out of
     * bounds.
     * @throws IOException if an underlying I/O error occurs.
     */
    fun exists(archive: Int, group: Int): Boolean

    /**
     * Lists all archives in the store.
     * @return a sorted list of archive IDs.
     * @throws IOException if an underlying I/O error occurs.
     */
    fun list(): List<Int>

    /**
     * Lists all groups in an archive.
     * @param archive the archive ID.
     * @return a sorted list of group IDs.
     * @throws IllegalArgumentException if the archive or group ID is out of
     * bounds.
     * @throws FileNotFoundException if the archive does not exist.
     * @throws IOException if an underlying I/O error occurs.
     */
    fun list(archive: Int): List<Int>

    /**
     * Creates an archive. Does nothing if the archive already exists.
     * @param archive the archive ID.
     * @throws IllegalArgumentException if the archive ID is out of bounds.
     * @throws IOException if an underlying I/O error occurs.
     */
    fun create(archive: Int)

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
    fun read(archive: Int, group: Int): ByteBuf

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
    fun write(archive: Int, group: Int, buf: ByteBuf)

    /**
     * Deletes an archive and all groups contained inside it. Does nothing if
     * the archive does not exist.
     * @param archive the archive ID.
     * @throws IllegalArgumentException if the archive ID is out of bounds.
     * @throws IOException if an underlying I/O error occurs.
     */
    fun remove(archive: Int)

    /**
     * Deletes a group. Does nothing if the archive or group does not exist.
     * @param archive the archive ID.
     * @param group the group ID.
     * @throws IllegalArgumentException if the archive or group ID is out of
     * bounds.
     * @throws IOException if an underlying I/O error occurs.
     */
    fun remove(archive: Int, group: Int)

    companion object {
        /**
         * The maximum archive ID.
         */
        const val MAX_ARCHIVE = 255

        /**
         * The maximum length of a group's contents in bytes.
         */
        const val MAX_GROUP_SIZE = (1 shl 24) - 1
    }
}
