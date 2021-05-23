package org.openrs2.game.net

import io.netty.channel.DefaultFileRegion
import io.netty.channel.FileRegion
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Singleton

@Singleton
public class FileProvider {
    public fun get(uri: String): FileRegion? {
        var path = ROOT.resolve(uri).toAbsolutePath().normalize()
        if (!path.startsWith(ROOT)) {
            return null
        }

        if (!Files.exists(path)) {
            path = stripChecksum(path)
        }

        if (!Files.isRegularFile(path)) {
            return null
        }

        val channel = FileChannel.open(path)
        return DefaultFileRegion(channel, 0, channel.size())
    }

    private fun stripChecksum(path: Path): Path {
        val name = path.fileName.toString()

        val extensionIndex = name.lastIndexOf('.')
        if (extensionIndex == -1) {
            return path
        }

        val checksumIndex = name.lastIndexOf('_', extensionIndex)
        if (checksumIndex == -1) {
            return path
        }

        return path.resolveSibling(name.substring(0, checksumIndex) + name.substring(extensionIndex))
    }

    private companion object {
        private val ROOT = Path.of("nonfree/var/cache/client").toAbsolutePath().normalize()
    }
}
