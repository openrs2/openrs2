package dev.openrs2.util.io

import java.io.BufferedWriter
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption

inline fun <T> Path.atomicWrite(f: (Path) -> T): T {
    val tempFile = Files.createTempFile(parent, ".$fileName", ".tmp")
    try {
        val result = f(tempFile)
        Files.move(tempFile, this, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        return result
    } finally {
        Files.deleteIfExists(tempFile)
    }
}

inline fun <T> Path.useAtomicBufferedWriter(vararg options: OpenOption, f: (BufferedWriter) -> T): T {
    return atomicWrite { path ->
        Files.newBufferedWriter(path, *options).use { writer ->
            f(writer)
        }
    }
}

inline fun <T> Path.useAtomicBufferedWriter(cs: Charset, vararg options: OpenOption, f: (BufferedWriter) -> T): T {
    return atomicWrite { path ->
        Files.newBufferedWriter(path, cs, *options).use { writer ->
            f(writer)
        }
    }
}

inline fun <T> Path.useAtomicOutputStream(vararg options: OpenOption, f: (OutputStream) -> T): T {
    return atomicWrite { path ->
        Files.newOutputStream(path, *options).use { output ->
            f(output)
        }
    }
}
