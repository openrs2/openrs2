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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiskStoreTest {
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
    fun testCreateArchive() {
        writeTest("empty-archive") { store ->
            store.create(255)
        }

        writeTest("empty-archives") { store ->
            store.create(0)
            store.create(255)
        }
    }

    @Test
    fun testArchiveExists() {
        readTest("empty") { store ->
            assertFalse(store.exists(0))
            assertFalse(store.exists(1))
            assertFalse(store.exists(254))
            assertFalse(store.exists(255))
        }

        readTest("empty-archive") { store ->
            assertFalse(store.exists(0))
            assertFalse(store.exists(1))
            assertFalse(store.exists(254))
            assertTrue(store.exists(255))
        }

        readTest("empty-archives") { store ->
            assertTrue(store.exists(0))
            assertFalse(store.exists(1))
            assertFalse(store.exists(254))
            assertTrue(store.exists(255))
        }
    }

    @Test
    fun testListArchives() {
        readTest("empty") { store ->
            assertEquals(emptyList(), store.list())
        }

        readTest("empty-archive") { store ->
            assertEquals(listOf(255), store.list())
        }

        readTest("empty-archives") { store ->
            assertEquals(listOf(0, 255), store.list())
        }
    }

    @Test
    fun testRemoveArchive() {
        overwriteTest("empty-archives", "empty-archive") { store ->
            store.remove(0)
        }

        overwriteTest("empty-archive", "empty") { store ->
            store.remove(255)
        }

        overwriteTest("empty", "empty") { store ->
            store.remove(0)
            store.remove(255)
        }
    }

    @Test
    fun testGroupExists() {
        readTest("single-block") { store ->
            assertFalse(store.exists(0, 0))
            assertFalse(store.exists(255, 0))
            assertTrue(store.exists(255, 1))
            assertFalse(store.exists(255, 2))
        }
    }

    @Test
    fun testListGroups() {
        readTest("single-block") { store ->
            assertEquals(listOf(1), store.list(255))
        }

        readTest("fragmented") { store ->
            assertEquals(listOf(0, 1), store.list(255))
        }

        readTest("single-block-extended") { store ->
            assertEquals(listOf(65536), store.list(255))
        }
    }

    @Test
    fun testListNonExistent() {
        readTest("empty") { store ->
            assertFailsWith<FileNotFoundException> {
                store.list(0)
            }
        }
    }

    @Test
    fun testReadSingleBlock() {
        readTest("single-block") { store ->
            copiedBuffer("OpenRS2").use { expected ->
                store.read(255, 1).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testReadSingleBlockExtended() {
        readTest("single-block-extended") { store ->
            copiedBuffer("OpenRS2").use { expected ->
                store.read(255, 65536).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testReadTwoBlocks() {
        readTest("two-blocks") { store ->
            copiedBuffer("OpenRS2".repeat(100)).use { expected ->
                store.read(255, 1).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testReadTwoBlocksExtended() {
        readTest("two-blocks-extended") { store ->
            copiedBuffer("OpenRS2".repeat(100)).use { expected ->
                store.read(255, 65536).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testReadMultipleBlocks() {
        readTest("multiple-blocks") { store ->
            copiedBuffer("OpenRS2".repeat(1000)).use { expected ->
                store.read(255, 1).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testReadMultipleBlocksExtended() {
        readTest("multiple-blocks-extended") { store ->
            copiedBuffer("OpenRS2".repeat(1000)).use { expected ->
                store.read(255, 65536).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testReadNonExistent() {
        readTest("single-block") { store ->
            assertFailsWith<FileNotFoundException> {
                store.read(0, 0)
            }

            assertFailsWith<FileNotFoundException> {
                store.read(255, 0)
            }

            assertFailsWith<FileNotFoundException> {
                store.read(255, 2)
            }
        }
    }

    @Test
    fun testReadOverwritten() {
        readTest("single-block-overwritten") { store ->
            store.read(255, 1).use { actual ->
                copiedBuffer("Hello").use { expected ->
                    assertEquals(expected, actual)
                }
            }
        }

        readTest("two-blocks-overwritten") { store ->
            store.read(255, 1).use { actual ->
                copiedBuffer("Hello").use { expected ->
                    assertEquals(expected, actual)
                }
            }
        }

        readTest("multiple-blocks-overwritten") { store ->
            store.read(255, 1).use { actual ->
                copiedBuffer("Hello".repeat(200)).use { expected ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testReadFragmented() {
        readTest("fragmented") { store ->
            copiedBuffer("OpenRS2".repeat(100)).use { expected ->
                for (j in 0 until 2) {
                    store.read(255, j).use { actual ->
                        assertEquals(expected, actual)
                    }
                }
            }
        }
    }

    @Test
    fun testWriteSingleBlock() {
        writeTest("single-block") { store ->
            copiedBuffer("OpenRS2").use { buf ->
                store.write(255, 1, buf)
            }
        }
    }

    @Test
    fun testWriteSingleBlockExtended() {
        writeTest("single-block-extended") { store ->
            copiedBuffer("OpenRS2").use { buf ->
                store.write(255, 65536, buf)
            }
        }
    }

    @Test
    fun testWriteTwoBlocks() {
        writeTest("two-blocks") { store ->
            copiedBuffer("OpenRS2".repeat(100)).use { buf ->
                store.write(255, 1, buf)
            }
        }
    }

    @Test
    fun testWriteTwoBlocksExtended() {
        writeTest("two-blocks-extended") { store ->
            copiedBuffer("OpenRS2".repeat(100)).use { buf ->
                store.write(255, 65536, buf)
            }
        }
    }

    @Test
    fun testWriteMultipleBlocks() {
        writeTest("multiple-blocks") { store ->
            copiedBuffer("OpenRS2".repeat(1000)).use { buf ->
                store.write(255, 1, buf)
            }
        }
    }

    @Test
    fun testWriteMultipleBlocksExtended() {
        writeTest("multiple-blocks-extended") { store ->
            copiedBuffer("OpenRS2".repeat(1000)).use { buf ->
                store.write(255, 65536, buf)
            }
        }
    }

    @Test
    fun testWriteFragmented() {
        writeTest("fragmented") { store ->
            for (i in 1..2) {
                copiedBuffer("OpenRS2".repeat(i * 50)).use { buf ->
                    for (j in 0 until 2) {
                        store.write(255, j, buf.slice())
                    }
                }
            }
        }
    }

    @Test
    fun testOverwriteShorter() {
        overwriteTest("single-block", "single-block-overwritten") { store ->
            copiedBuffer("Hello").use { buf ->
                store.write(255, 1, buf)
            }
        }

        overwriteTest("two-blocks", "two-blocks-overwritten") { store ->
            copiedBuffer("Hello").use { buf ->
                store.write(255, 1, buf)
            }
        }

        overwriteTest("multiple-blocks", "multiple-blocks-overwritten") { store ->
            copiedBuffer("Hello".repeat(200)).use { buf ->
                store.write(255, 1, buf)
            }
        }
    }

    @Test
    fun testOverwriteShorterExtended() {
        overwriteTest("single-block-extended", "single-block-extended-overwritten") { store ->
            copiedBuffer("Hello").use { buf ->
                store.write(255, 65536, buf)
            }
        }

        overwriteTest("two-blocks-extended", "two-blocks-extended-overwritten") { store ->
            copiedBuffer("Hello").use { buf ->
                store.write(255, 65536, buf)
            }
        }

        overwriteTest("multiple-blocks-extended", "multiple-blocks-extended-overwritten") { store ->
            copiedBuffer("Hello".repeat(200)).use { buf ->
                store.write(255, 65536, buf)
            }
        }
    }

    @Test
    fun testOverwriteLonger() {
        overwriteTest("single-block", "two-blocks") { store ->
            copiedBuffer("OpenRS2".repeat(100)).use { buf ->
                store.write(255, 1, buf)
            }
        }

        overwriteTest("single-block", "multiple-blocks") { store ->
            copiedBuffer("OpenRS2".repeat(1000)).use { buf ->
                store.write(255, 1, buf)
            }
        }

        overwriteTest("two-blocks", "multiple-blocks") { store ->
            copiedBuffer("OpenRS2".repeat(1000)).use { buf ->
                store.write(255, 1, buf)
            }
        }
    }

    @Test
    fun testOverwriteLongerExtended() {
        overwriteTest("single-block-extended", "two-blocks-extended") { store ->
            copiedBuffer("OpenRS2".repeat(100)).use { buf ->
                store.write(255, 65536, buf)
            }
        }

        overwriteTest("single-block-extended", "multiple-blocks-extended") { store ->
            copiedBuffer("OpenRS2".repeat(1000)).use { buf ->
                store.write(255, 65536, buf)
            }
        }

        overwriteTest("two-blocks-extended", "multiple-blocks-extended") { store ->
            copiedBuffer("OpenRS2".repeat(1000)).use { buf ->
                store.write(255, 65536, buf)
            }
        }
    }

    @Test
    fun testRemoveGroup() {
        overwriteTest("empty", "empty") { store ->
            store.remove(0, 0)
        }

        overwriteTest("single-block", "single-block") { store ->
            store.remove(255, 2)
        }

        overwriteTest("single-block", "single-block-removed") { store ->
            store.remove(255, 1)
        }
    }

    @Test
    fun testReadCorrupt() {
        readTest("corrupt-first-eof-early") { store ->
            assertFailsWith<StoreCorruptException> {
                store.read(255, 1).release()
            }
        }

        readTest("corrupt-first-invalid-archive") { store ->
            assertFailsWith<StoreCorruptException> {
                store.read(255, 1).release()
            }
        }

        readTest("corrupt-first-invalid-block-number") { store ->
            assertFailsWith<StoreCorruptException> {
                store.read(255, 1).release()
            }
        }

        readTest("corrupt-first-invalid-group") { store ->
            assertFailsWith<StoreCorruptException> {
                store.read(255, 1).release()
            }
        }

        readTest("corrupt-first-outside-data-file") { store ->
            assertFailsWith<StoreCorruptException> {
                store.read(255, 1).release()
            }
        }

        readTest("corrupt-second-eof-early") { store ->
            assertFailsWith<StoreCorruptException> {
                store.read(255, 1).release()
            }
        }

        readTest("corrupt-second-invalid-archive") { store ->
            assertFailsWith<StoreCorruptException> {
                store.read(255, 1).release()
            }
        }

        readTest("corrupt-second-invalid-block-number") { store ->
            assertFailsWith<StoreCorruptException> {
                store.read(255, 1).release()
            }
        }

        readTest("corrupt-second-invalid-group") { store ->
            assertFailsWith<StoreCorruptException> {
                store.read(255, 1).release()
            }
        }

        readTest("corrupt-second-outside-data-file") { store ->
            assertFailsWith<StoreCorruptException> {
                store.read(255, 1).release()
            }
        }
    }

    @Test
    fun testReadCorruptEofLate() {
        readTest("corrupt-eof-late") { store ->
            copiedBuffer("OpenRS2".repeat(1000)).use { expected ->
                store.read(255, 1).use { actual ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testOverwriteCorrupt() {
        overwriteTest("corrupt-eof-late", "corrupt-eof-late-overwritten") { store ->
            copiedBuffer("OpenRS2".repeat(1050)).use { buf ->
                store.write(255, 1, buf)
            }
        }

        overwriteTest("corrupt-first-eof-early", "corrupt-first-eof-early-overwritten") { store ->
            copiedBuffer("Hello".repeat(300)).use { buf ->
                store.write(255, 1, buf)
            }
        }

        overwriteTest("corrupt-first-invalid-archive", "corrupt-first-invalid-archive-overwritten") { store ->
            copiedBuffer("Hello".repeat(300)).use { buf ->
                store.write(255, 1, buf)
            }
        }

        overwriteTest("corrupt-first-invalid-block-number", "corrupt-first-invalid-block-number-overwritten") { store ->
            copiedBuffer("Hello".repeat(300)).use { buf ->
                store.write(255, 1, buf)
            }
        }

        overwriteTest("corrupt-first-invalid-group", "corrupt-first-invalid-group-overwritten") { store ->
            copiedBuffer("Hello".repeat(300)).use { buf ->
                store.write(255, 1, buf)
            }
        }

        overwriteTest("corrupt-first-outside-data-file", "corrupt-first-outside-data-file-overwritten") { store ->
            copiedBuffer("Hello".repeat(300)).use { buf ->
                store.write(255, 1, buf)
            }
        }

        overwriteTest("corrupt-second-eof-early", "corrupt-second-eof-early-overwritten") { store ->
            copiedBuffer("Hello".repeat(300)).use { buf ->
                store.write(255, 1, buf)
            }
        }

        overwriteTest("corrupt-second-invalid-archive", "corrupt-second-invalid-archive-overwritten") { store ->
            copiedBuffer("Hello".repeat(300)).use { buf ->
                store.write(255, 1, buf)
            }
        }

        overwriteTest(
            "corrupt-second-invalid-block-number",
            "corrupt-second-invalid-block-number-overwritten"
        ) { store ->
            copiedBuffer("Hello".repeat(300)).use { buf ->
                store.write(255, 1, buf)
            }
        }

        overwriteTest("corrupt-second-invalid-group", "corrupt-second-invalid-group-overwritten") { store ->
            copiedBuffer("Hello".repeat(300)).use { buf ->
                store.write(255, 1, buf)
            }
        }

        overwriteTest("corrupt-second-outside-data-file", "corrupt-second-outside-data-file-overwritten") { store ->
            copiedBuffer("Hello".repeat(300)).use { buf ->
                store.write(255, 1, buf)
            }
        }
    }

    @Test
    fun testReadOverwrittenCorrupt() {
        readTest("corrupt-eof-late-overwritten") { store ->
            store.read(255, 1).use { actual ->
                copiedBuffer("OpenRS2".repeat(1050)).use { expected ->
                    assertEquals(expected, actual)
                }
            }
        }

        readTest("corrupt-first-eof-early-overwritten") { store ->
            store.read(255, 1).use { actual ->
                copiedBuffer("Hello".repeat(300)).use { expected ->
                    assertEquals(expected, actual)
                }
            }
        }

        readTest("corrupt-first-invalid-archive-overwritten") { store ->
            store.read(255, 1).use { actual ->
                copiedBuffer("Hello".repeat(300)).use { expected ->
                    assertEquals(expected, actual)
                }
            }
        }

        readTest("corrupt-first-invalid-block-number-overwritten") { store ->
            store.read(255, 1).use { actual ->
                copiedBuffer("Hello".repeat(300)).use { expected ->
                    assertEquals(expected, actual)
                }
            }
        }

        readTest("corrupt-first-invalid-group-overwritten") { store ->
            store.read(255, 1).use { actual ->
                copiedBuffer("Hello".repeat(300)).use { expected ->
                    assertEquals(expected, actual)
                }
            }
        }

        readTest("corrupt-first-outside-data-file-overwritten") { store ->
            store.read(255, 1).use { actual ->
                copiedBuffer("Hello".repeat(300)).use { expected ->
                    assertEquals(expected, actual)
                }
            }
        }

        readTest("corrupt-second-eof-early-overwritten") { store ->
            store.read(255, 1).use { actual ->
                copiedBuffer("Hello".repeat(300)).use { expected ->
                    assertEquals(expected, actual)
                }
            }
        }

        readTest("corrupt-second-invalid-archive-overwritten") { store ->
            store.read(255, 1).use { actual ->
                copiedBuffer("Hello".repeat(300)).use { expected ->
                    assertEquals(expected, actual)
                }
            }
        }

        readTest("corrupt-second-invalid-block-number-overwritten") { store ->
            store.read(255, 1).use { actual ->
                copiedBuffer("Hello".repeat(300)).use { expected ->
                    assertEquals(expected, actual)
                }
            }
        }

        readTest("corrupt-second-invalid-group-overwritten") { store ->
            store.read(255, 1).use { actual ->
                copiedBuffer("Hello".repeat(300)).use { expected ->
                    assertEquals(expected, actual)
                }
            }
        }

        readTest("corrupt-second-outside-data-file-overwritten") { store ->
            store.read(255, 1).use { actual ->
                copiedBuffer("Hello".repeat(300)).use { expected ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testDat2mRead() {
        readTest("dat2m") { store ->
            store.read(0, 0).use { actual ->
                copiedBuffer("Open").use { expected ->
                    assertEquals(expected, actual)
                }
            }

            store.read(40, 0).use { actual ->
                copiedBuffer("RS2").use { expected ->
                    assertEquals(expected, actual)
                }
            }
        }
    }

    @Test
    fun testDat2mWrite() {
        overwriteTest("dat2m-empty", "dat2m") { store ->
            copiedBuffer("Open").use { buf ->
                store.write(0, 0, buf)
            }

            copiedBuffer("RS2").use { buf ->
                store.write(40, 0, buf)
            }
        }
    }

    private fun readTest(name: String, f: (Store) -> Unit) {
        DiskStore.open(ROOT.resolve(name)).use { store ->
            f(store)
        }
    }

    private fun writeTest(name: String, f: (Store) -> Unit) {
        Jimfs.newFileSystem(Configuration.forCurrentPlatform()).use { fs ->
            val actual = fs.rootDirectories.first().resolve("cache")
            DiskStore.create(actual).use { store ->
                f(store)
            }

            val expected = ROOT.resolve(name)
            assertTrue(expected.recursiveEquals(actual))
        }
    }

    private fun overwriteTest(src: String, name: String, f: (Store) -> Unit) {
        Jimfs.newFileSystem(Configuration.forCurrentPlatform()).use { fs ->
            val actual = fs.rootDirectories.first().resolve("cache")
            ROOT.resolve(src).recursiveCopy(actual)

            DiskStore.open(actual).use { store ->
                f(store)
            }

            val expected = ROOT.resolve(name)
            assertTrue(expected.recursiveEquals(actual))
        }
    }

    private companion object {
        private val ROOT = Path.of(DiskStoreTest::class.java.getResource("disk-store").toURI())
    }
}
