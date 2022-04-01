package org.openrs2.cache

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import org.openrs2.buffer.use
import org.openrs2.util.io.useAtomicOutputStream
import java.io.FileNotFoundException
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path

/**
 * A [Store] implementation that represents archives as file system directories
 * and groups as file system files. This format is much friendlier to
 * content-addressable version control systems, such as Git, than the native
 * format used by the client.
 *
 * Multiple read threads may use this class simultaneously. However, only a
 * single thread may write at a time. Reads and writes must not happen
 * simultaneously.
 */
public class FlatFileStore private constructor(
    private val root: Path,
    private val alloc: ByteBufAllocator
) : Store {
    private fun archivePath(archive: Int): Path {
        require(archive in 0..Store.MAX_ARCHIVE)
        return root.resolve(archive.toString())
    }

    private fun groupPath(archive: Int, group: Int): Path {
        // no upper bound on the range check here, as newer caches support 4 byte group IDs
        require(group >= 0)
        return archivePath(archive).resolve("$group$GROUP_EXTENSION")
    }

    override fun exists(archive: Int): Boolean {
        return Files.isDirectory(archivePath(archive))
    }

    override fun exists(archive: Int, group: Int): Boolean {
        return Files.isRegularFile(groupPath(archive, group))
    }

    override fun list(): List<Int> {
        Files.newDirectoryStream(root).use { stream ->
            return stream.filter { Files.isDirectory(it) && ARCHIVE_NAME.matches(it.fileName.toString()) }
                .map { Integer.parseInt(it.fileName.toString()) }
                .sorted()
        }
    }

    override fun list(archive: Int): List<Int> {
        val path = archivePath(archive)
        if (!Files.isDirectory(path)) {
            throw FileNotFoundException()
        }

        Files.newDirectoryStream(path).use { stream ->
            return stream.filter { Files.isRegularFile(it) && GROUP_NAME.matches(it.fileName.toString()) }
                .map { Integer.parseInt(it.fileName.toString().removeSuffix(GROUP_EXTENSION)) }
                .sorted()
        }
    }

    override fun create(archive: Int) {
        Files.createDirectories(archivePath(archive))
    }

    override fun read(archive: Int, group: Int): ByteBuf {
        val path = groupPath(archive, group)
        if (!Files.isRegularFile(path)) {
            throw FileNotFoundException()
        }

        FileChannel.open(path).use { channel ->
            val size = channel.size()
            if (size > Store.MAX_GROUP_SIZE) {
                throw StoreCorruptException("Group too large")
            }

            alloc.buffer(size.toInt(), size.toInt()).use { buf ->
                buf.writeBytes(channel, 0, buf.writableBytes())
                return buf.retain()
            }
        }
    }

    override fun write(archive: Int, group: Int, buf: ByteBuf) {
        require(buf.readableBytes() <= Store.MAX_GROUP_SIZE)

        val path = groupPath(archive, group)
        Files.createDirectories(path.parent)

        path.useAtomicOutputStream { output ->
            buf.readBytes(output, buf.readableBytes())
        }
    }

    override fun remove(archive: Int) {
        val path = archivePath(archive)
        if (!Files.isDirectory(path)) {
            return
        }

        Files.newDirectoryStream(path).use { stream ->
            stream.filter { Files.isRegularFile(it) && GROUP_NAME.matches(it.fileName.toString()) }
                .forEach { Files.deleteIfExists(it) }
        }

        Files.deleteIfExists(path)
    }

    override fun remove(archive: Int, group: Int) {
        Files.deleteIfExists(groupPath(archive, group))
    }

    override fun flush() {
        // no-op
    }

    override fun close() {
        // no-op
    }

    public companion object {
        private val ARCHIVE_NAME = Regex("[0-9]+")
        private val GROUP_NAME = Regex("[0-9]+[.]dat")
        private const val GROUP_EXTENSION = ".dat"

        @JvmOverloads
        public fun open(root: Path, alloc: ByteBufAllocator = ByteBufAllocator.DEFAULT): Store {
            if (!Files.isDirectory(root)) {
                throw FileNotFoundException()
            }

            return FlatFileStore(root, alloc)
        }

        @JvmOverloads
        public fun create(root: Path, alloc: ByteBufAllocator = ByteBufAllocator.DEFAULT): Store {
            Files.createDirectories(root)
            return FlatFileStore(root, alloc)
        }
    }
}
