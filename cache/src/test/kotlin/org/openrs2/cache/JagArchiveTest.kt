package org.openrs2.cache

import io.netty.buffer.Unpooled
import org.openrs2.buffer.copiedBuffer
import org.openrs2.buffer.use
import org.openrs2.util.jagHashCode
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JagArchiveTest {
    @Test
    fun testEmpty() {
        JagArchive().use { archive ->
            assertEquals(0, archive.size)
            assertEquals(emptyList(), archive.list().asSequence().toList())

            packTest("empty-compressed-archive.jag", archive, true)
            packTest("empty-compressed-entries.jag", archive, false)
            packBestTest("empty-compressed-entries.jag", archive)

            unpackTest("empty-compressed-archive.jag", archive)
            unpackTest("empty-compressed-entries.jag", archive)
        }
    }

    @Test
    fun testSingleEntry() {
        JagArchive().use { archive ->
            assertEquals(0, archive.size)
            assertEquals(emptyList(), archive.list().asSequence().toList())
            assertFalse(archive.exists("TEST.TXT"))
            assertFalse(archive.existsNamed("TEST.TXT".jagHashCode()))

            copiedBuffer("OpenRS2").use { expected ->
                archive.write("TEST.TXT", expected)

                archive.read("TEST.TXT").use { actual ->
                    assertEquals(expected, actual)
                }

                archive.readNamed("TEST.TXT".jagHashCode()).use { actual ->
                    assertEquals(expected, actual)
                }
            }

            assertEquals(1, archive.size)
            assertEquals(listOf("TEST.TXT".jagHashCode()), archive.list().asSequence().toList())
            assertTrue(archive.exists("TEST.TXT"))
            assertTrue(archive.existsNamed("TEST.TXT".jagHashCode()))
            assertFalse(archive.exists("HELLO.TXT"))
            assertFalse(archive.existsNamed("HELLO.TXT".jagHashCode()))

            packTest("single-compressed-archive.jag", archive, true)
            packTest("single-compressed-entries.jag", archive, false)
            packBestTest("single-compressed-entries.jag", archive)

            unpackTest("single-compressed-archive.jag", archive)
            unpackTest("single-compressed-entries.jag", archive)
        }
    }

    @Test
    fun testMultipleEntries() {
        JagArchive().use { archive ->
            assertEquals(0, archive.size)
            assertEquals(emptyList(), archive.list().asSequence().toList())
            assertFalse(archive.exists("TEST.TXT"))
            assertFalse(archive.existsNamed("TEST.TXT".jagHashCode()))
            assertFalse(archive.exists("HELLO.TXT"))
            assertFalse(archive.existsNamed("HELLO.TXT".jagHashCode()))

            copiedBuffer("OpenRS2").use { expected ->
                archive.write("TEST.TXT", expected)

                archive.read("TEST.TXT").use { actual ->
                    assertEquals(expected, actual)
                }

                archive.readNamed("TEST.TXT".jagHashCode()).use { actual ->
                    assertEquals(expected, actual)
                }
            }

            copiedBuffer("Hello").use { expected ->
                archive.write("HELLO.TXT", expected)

                archive.read("HELLO.TXT").use { actual ->
                    assertEquals(expected, actual)
                }

                archive.readNamed("HELLO.TXT".jagHashCode()).use { actual ->
                    assertEquals(expected, actual)
                }
            }

            assertEquals(2, archive.size)
            assertEquals(
                listOf(
                    "TEST.TXT".jagHashCode(),
                    "HELLO.TXT".jagHashCode()
                ), archive.list().asSequence().toList()
            )
            assertTrue(archive.exists("TEST.TXT"))
            assertTrue(archive.existsNamed("TEST.TXT".jagHashCode()))
            assertTrue(archive.exists("HELLO.TXT"))
            assertTrue(archive.existsNamed("HELLO.TXT".jagHashCode()))
            assertFalse(archive.exists("OTHER.TXT"))
            assertFalse(archive.existsNamed("OTHER.TXT".jagHashCode()))

            packTest("multiple-compressed-archive.jag", archive, true)
            packTest("multiple-compressed-entries.jag", archive, false)
            packBestTest("multiple-compressed-archive.jag", archive)

            unpackTest("multiple-compressed-archive.jag", archive)
            unpackTest("multiple-compressed-entries.jag", archive)

            archive.remove("TEST.TXT")

            assertEquals(1, archive.size)
            assertEquals(listOf("HELLO.TXT".jagHashCode()), archive.list().asSequence().toList())
            assertFalse(archive.exists("TEST.TXT"))
            assertFalse(archive.existsNamed("TEST.TXT".jagHashCode()))
            assertTrue(archive.exists("HELLO.TXT"))
            assertTrue(archive.existsNamed("HELLO.TXT".jagHashCode()))
            assertFalse(archive.exists("OTHER.TXT"))
            assertFalse(archive.existsNamed("OTHER.TXT".jagHashCode()))

            archive.remove("TEST.TXT")
            archive.removeNamed("HELLO.TXT".jagHashCode()) // check remove a non-existent entry works

            assertEquals(0, archive.size)
            assertEquals(emptyList(), archive.list().asSequence().toList())
            assertFalse(archive.exists("TEST.TXT"))
            assertFalse(archive.existsNamed("TEST.TXT".jagHashCode()))
            assertFalse(archive.exists("HELLO.TXT"))
            assertFalse(archive.existsNamed("HELLO.TXT".jagHashCode()))
            assertFalse(archive.exists("OTHER.TXT"))
            assertFalse(archive.existsNamed("OTHER.TXT".jagHashCode()))

            archive.removeNamed("HELLO.TXT".jagHashCode())

            archive.remove("OTHER.TXT") // check removing an entry that never existed works
            archive.removeNamed("OTHER.TXT".jagHashCode())

            assertEquals(0, archive.size)
            assertEquals(emptyList(), archive.list().asSequence().toList())
            assertFalse(archive.exists("TEST.TXT"))
            assertFalse(archive.existsNamed("TEST.TXT".jagHashCode()))
            assertFalse(archive.exists("HELLO.TXT"))
            assertFalse(archive.existsNamed("HELLO.TXT".jagHashCode()))
            assertFalse(archive.exists("OTHER.TXT"))
            assertFalse(archive.existsNamed("OTHER.TXT".jagHashCode()))
        }
    }

    @Test
    fun testDuplicateEntries() {
        JagArchive().use { archive ->
            copiedBuffer("OpenRS2").use { buf ->
                archive.write("TEST.TXT", buf)
            }

            unpackTest("duplicate-entries.jag", archive)
        }
    }

    private fun packTest(name: String, archive: JagArchive, compressedArchive: Boolean) {
        Unpooled.wrappedBuffer(Files.readAllBytes(ROOT.resolve(name))).use { expected ->
            archive.pack(compressedArchive).use { actual ->
                assertEquals(expected, actual)
            }
        }
    }

    private fun packBestTest(name: String, archive: JagArchive) {
        Unpooled.wrappedBuffer(Files.readAllBytes(ROOT.resolve(name))).use { expected ->
            archive.packBest().use { actual ->
                assertEquals(expected, actual)
            }
        }
    }

    private fun unpackTest(name: String, expected: JagArchive) {
        Unpooled.wrappedBuffer(Files.readAllBytes(ROOT.resolve(name))).use { buf ->
            JagArchive.unpack(buf).use { actual ->
                assertEquals(expected, actual)
            }
        }
    }

    private companion object {
        private val ROOT = Path.of(JagArchiveTest::class.java.getResource("jag").toURI())
    }
}
