package org.openrs2.cache

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.netty.buffer.Unpooled
import org.openrs2.buffer.copiedBuffer
import org.openrs2.buffer.use
import org.openrs2.util.io.recursiveCopy
import org.openrs2.util.io.recursiveEquals
import java.io.FileNotFoundException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

object FlatFileStoreTest {
    private val IGNORE_GIT_EMPTY = { path: Path -> path.fileName.toString() != ".gitempty" }
    private val ROOT = Paths.get(FlatFileStoreTest::class.java.getResource("flat-file-store").toURI())

    @Test
    fun testBounds() {
        readTest("empty") { store ->
            assertFailsWith<IllegalArgumentException> {
                store.exists(-1)
            }

            assertFailsWith<IllegalArgumentException> {
                store.exists(256)
            }

            assertFailsWith<IllegalArgumentException> {
                store.list(-1)
            }

            assertFailsWith<IllegalArgumentException> {
                store.list(256)
            }

            assertFailsWith<IllegalArgumentException> {
                store.exists(-1, 0)
            }

            assertFailsWith<IllegalArgumentException> {
                store.exists(256, 0)
            }

            assertFailsWith<IllegalArgumentException> {
                store.exists(0, -1)
            }

            assertFailsWith<IllegalArgumentException> {
                store.read(-1, 0).release()
            }

            assertFailsWith<IllegalArgumentException> {
                store.read(256, 0).release()
            }

            assertFailsWith<IllegalArgumentException> {
                store.read(0, -1).release()
            }
        }

        writeTest("empty") { store ->
            assertFailsWith<IllegalArgumentException> {
                store.create(-1)
            }

            assertFailsWith<IllegalArgumentException> {
                store.create(256)
            }

            assertFailsWith<IllegalArgumentException> {
                store.write(-1, 0, Unpooled.EMPTY_BUFFER)
            }

            assertFailsWith<IllegalArgumentException> {
                store.write(256, 0, Unpooled.EMPTY_BUFFER)
            }

            assertFailsWith<IllegalArgumentException> {
                store.write(0, -1, Unpooled.EMPTY_BUFFER)
            }

            assertFailsWith<IllegalArgumentException> {
                store.remove(-1)
            }

            assertFailsWith<IllegalArgumentException> {
                store.remove(256)
            }

            assertFailsWith<IllegalArgumentException> {
                store.remove(-1, 0)
            }

            assertFailsWith<IllegalArgumentException> {
                store.remove(256, 0)
            }

            assertFailsWith<IllegalArgumentException> {
                store.remove(0, -1)
            }
        }
    }

    @Test
    fun testExists() {
        readTest("multiple-groups") { store ->
            assertTrue(store.exists(0))
            assertFalse(store.exists(1))
            assertFalse(store.exists(254))
            assertTrue(store.exists(255))

            assertTrue(store.exists(0, 0))
            assertTrue(store.exists(0, 1))
            assertFalse(store.exists(0, 2))

            assertFalse(store.exists(1, 0))
            assertFalse(store.exists(254, 0))

            assertTrue(store.exists(255, 0))
            assertFalse(store.exists(255, 1))
            assertFalse(store.exists(255, 65535))
            assertTrue(store.exists(255, 65536))
            assertFalse(store.exists(255, 65537))
        }
    }

    @Test
    fun testList() {
        readTest("empty") { store ->
            assertEquals(emptyList(), store.list())
        }

        readTest("multiple-groups") { store ->
            assertEquals(listOf(0, 255), store.list())

            assertEquals(listOf(0, 1), store.list(0))
            assertEquals(listOf(0, 65536), store.list(255))

            assertFailsWith<FileNotFoundException> {
                store.list(1)
            }

            assertFailsWith<FileNotFoundException> {
                store.list(254)
            }
        }
    }

    @Test
    fun testCreate() {
        writeTest("empty-archive") { store ->
            store.create(255)
        }

        overwriteTest("empty-archive", "empty-archive") { store ->
            store.create(255)
        }
    }

    @Test
    fun testRead() {
        readTest("multiple-groups") { store ->
            store.read(0, 0).use { actual ->
                assertEquals(Unpooled.EMPTY_BUFFER, actual)
            }

            store.read(0, 1).use { actual ->
                assertEquals(Unpooled.EMPTY_BUFFER, actual)
            }

            assertFailsWith<FileNotFoundException> {
                store.read(0, 2).release()
            }

            assertFailsWith<FileNotFoundException> {
                store.read(1, 0).release()
            }

            copiedBuffer("OpenRS2").use { expected ->
                store.read(255, 0).use { actual ->
                    assertEquals(expected, actual)
                }

                store.read(255, 65536).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testWrite() {
        writeTest("single-group") { store ->
            copiedBuffer("OpenRS2").use { buf ->
                store.write(255, 0, buf)
            }
        }

        overwriteTest("empty-archive", "single-group") { store ->
            copiedBuffer("OpenRS2").use { buf ->
                store.write(255, 0, buf)
            }
        }

        overwriteTest("single-group", "single-group-overwritten") { store ->
            copiedBuffer("Hello, world!").use { buf ->
                store.write(255, 0, buf)
            }
        }
    }

    @Test
    fun testRemoveArchive() {
        overwriteTest("multiple-groups", "empty") { store ->
            store.remove(0)
            store.remove(255)
        }

        writeTest("empty") { store ->
            store.remove(0)
            store.remove(255)
        }
    }

    @Test
    fun testRemoveGroup() {
        overwriteTest("single-group", "empty-archive") { store ->
            store.remove(255, 0)
        }

        overwriteTest("empty-archive", "empty-archive") { store ->
            store.remove(255, 0)
        }

        writeTest("empty") { store ->
            store.remove(255, 0)
        }
    }

    private fun readTest(name: String, f: (Store) -> Unit) {
        FlatFileStore.open(ROOT.resolve(name)).use { store ->
            f(store)
        }
    }

    private fun writeTest(name: String, f: (Store) -> Unit) {
        Jimfs.newFileSystem(Configuration.forCurrentPlatform()).use { fs ->
            val actual = fs.rootDirectories.first().resolve("cache")
            FlatFileStore.create(actual).use { store ->
                f(store)
            }

            val expected = ROOT.resolve(name)
            assertTrue(expected.recursiveEquals(actual, filter = IGNORE_GIT_EMPTY))
        }
    }

    private fun overwriteTest(src: String, name: String, f: (Store) -> Unit) {
        Jimfs.newFileSystem(Configuration.forCurrentPlatform()).use { fs ->
            val actual = fs.rootDirectories.first().resolve("cache")
            ROOT.resolve(src).recursiveCopy(actual)

            FlatFileStore.open(actual).use { store ->
                f(store)
            }

            val expected = ROOT.resolve(name)
            assertTrue(expected.recursiveEquals(actual, filter = IGNORE_GIT_EMPTY))
        }
    }
}
