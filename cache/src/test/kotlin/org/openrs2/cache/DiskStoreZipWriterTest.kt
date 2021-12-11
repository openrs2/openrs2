package org.openrs2.cache

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.netty.buffer.Unpooled
import org.openrs2.buffer.copiedBuffer
import org.openrs2.buffer.use
import org.openrs2.util.io.recursiveEquals
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DiskStoreZipWriterTest {
    @Test
    fun testBounds() {
        DiskStoreZipWriter(ZipOutputStream(OutputStream.nullOutputStream())).use { store ->
            // create
            assertFailsWith<IllegalArgumentException> {
                store.create(-1)
            }

            store.create(0)
            store.create(1)
            store.create(254)
            store.create(255)

            assertFailsWith<IllegalArgumentException> {
                store.create(256)
            }

            // write archive
            assertFailsWith<IllegalArgumentException> {
                store.write(-1, 0, Unpooled.EMPTY_BUFFER)
            }

            store.write(0, 0, Unpooled.EMPTY_BUFFER)
            store.write(1, 0, Unpooled.EMPTY_BUFFER)
            store.write(254, 0, Unpooled.EMPTY_BUFFER)
            store.write(255, 0, Unpooled.EMPTY_BUFFER)

            assertFailsWith<IllegalArgumentException> {
                store.write(256, 0, Unpooled.EMPTY_BUFFER)
            }

            // write group
            assertFailsWith<IllegalArgumentException> {
                store.write(0, -1, Unpooled.EMPTY_BUFFER)
            }

            store.write(0, 0, Unpooled.EMPTY_BUFFER)
            store.write(0, 1, Unpooled.EMPTY_BUFFER)
        }
    }

    @Test
    fun testUnsupported() {
        DiskStoreZipWriter(ZipOutputStream(OutputStream.nullOutputStream())).use { store ->
            assertFailsWith<UnsupportedOperationException> {
                store.exists(0)
            }

            assertFailsWith<UnsupportedOperationException> {
                store.exists(0, 0)
            }

            assertFailsWith<UnsupportedOperationException> {
                store.list()
            }

            assertFailsWith<UnsupportedOperationException> {
                store.list(0)
            }

            assertFailsWith<UnsupportedOperationException> {
                store.read(0, 0)
            }

            assertFailsWith<UnsupportedOperationException> {
                store.remove(0)
            }

            assertFailsWith<UnsupportedOperationException> {
                store.remove(0, 0)
            }
        }
    }

    private fun testWrite(expected: String, legacy: Boolean) {
        Jimfs.newFileSystem(Configuration.forCurrentPlatform()).use { fs ->
            val actual = fs.rootDirectories.first().resolve("zip")
            Files.createDirectories(actual)

            Files.newOutputStream(actual.resolve("cache.zip")).use { out ->
                DiskStoreZipWriter(ZipOutputStream(out), legacy = legacy).use { store ->
                    store.create(0)

                    copiedBuffer("OpenRS2").use { buf ->
                        store.write(2, 0, buf)
                    }

                    copiedBuffer("OpenRS2".repeat(100)).use { buf ->
                        store.write(2, 65535, buf)
                    }

                    copiedBuffer("OpenRS2".repeat(100)).use { buf ->
                        store.write(2, 65536, buf)
                    }
                }
            }

            assertTrue(ROOT.resolve(expected).recursiveEquals(actual))
        }
    }

    @Test
    fun testWrite() {
        testWrite("cache", legacy = false)
    }

    @Test
    fun testWriteLegacy() {
        testWrite("cache-legacy", legacy = true)
    }

    private companion object {
        private val ROOT = Path.of(DiskStoreZipWriterTest::class.java.getResource("disk-store-zip").toURI())
    }
}
