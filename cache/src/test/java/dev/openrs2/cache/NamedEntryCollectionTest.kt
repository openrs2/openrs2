package dev.openrs2.cache

import dev.openrs2.util.krHashCode
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

object NamedEntryCollectionTest {
    private class TestEntry(
        parent: NamedEntryCollection<TestEntry>,
        override val id: Int
    ) : NamedEntry {
        private var parent: NamedEntryCollection<TestEntry>? = parent

        override var nameHash: Int = -1
            set(value) {
                parent?.rename(id, field, value)
                field = value
            }

        override fun remove() {
            parent?.remove(this)
            parent = null
        }
    }

    private class TestCollection : NamedEntryCollection<TestEntry>(::TestEntry)

    @Test
    fun testBounds() {
        val collection = TestCollection()

        assertThrows<IllegalArgumentException> {
            collection.contains(-1)
        }

        assertThrows<IllegalArgumentException> {
            collection.containsNamed(-1)
        }

        assertThrows<IllegalArgumentException> {
            collection[-1]
        }

        assertThrows<IllegalArgumentException> {
            collection.getNamed(-1)
        }

        assertThrows<IllegalArgumentException> {
            collection.createOrGet(-1)
        }

        assertThrows<IllegalArgumentException> {
            collection.createOrGetNamed(-1)
        }

        assertThrows<IllegalArgumentException> {
            collection.remove(-1)
        }

        assertThrows<IllegalArgumentException> {
            collection.removeNamed(-1)
        }
    }

    @Test
    fun testEmpty() {
        val collection = TestCollection()
        assertEquals(0, collection.size)
        assertEquals(0, collection.capacity)
        assertEquals(emptyList(), collection.toList())
    }

    @Test
    fun testSingleEntry() {
        val collection = TestCollection()

        val entry = collection.createOrGet(0)
        assertEquals(0, entry.id)
        assertEquals(-1, entry.nameHash)

        assertEquals(1, collection.size)
        assertEquals(1, collection.capacity)

        assertEquals(entry, collection.createOrGet(0))

        assertEquals(1, collection.size)
        assertEquals(1, collection.capacity)

        assertTrue(collection.contains(0))
        assertFalse(collection.contains(1))

        assertEquals(entry, collection[0])
        assertNull(collection[1])

        assertEquals(listOf(entry), collection.toList())
    }

    @Test
    fun testMultipleEntries() {
        val collection = TestCollection()

        val entry0 = collection.createOrGet(0)
        assertEquals(0, entry0.id)
        assertEquals(-1, entry0.nameHash)

        val entry1 = collection.createOrGet(1)
        assertEquals(1, entry1.id)
        assertEquals(-1, entry1.nameHash)

        assertEquals(2, collection.size)
        assertEquals(2, collection.capacity)

        assertEquals(entry0, collection.createOrGet(0))
        assertEquals(entry1, collection.createOrGet(1))

        assertEquals(2, collection.size)
        assertEquals(2, collection.capacity)

        assertTrue(collection.contains(0))
        assertTrue(collection.contains(1))
        assertFalse(collection.contains(2))

        assertEquals(entry0, collection[0])
        assertEquals(entry1, collection[1])
        assertNull(collection[2])

        assertEquals(listOf(entry0, entry1), collection.toList())
    }

    @Test
    fun testSingleNamedEntry() {
        val collection = TestCollection()

        val entry = collection.createOrGet("hello")
        assertEquals(0, entry.id)
        assertEquals("hello".krHashCode(), entry.nameHash)

        assertEquals(1, collection.size)
        assertEquals(1, collection.capacity)

        assertEquals(entry, collection.createOrGet("hello"))

        assertEquals(1, collection.size)
        assertEquals(1, collection.capacity)

        assertTrue(collection.contains("hello"))
        assertFalse(collection.contains("world"))

        assertEquals(entry, collection["hello"])
        assertNull(collection["world"])

        assertEquals(listOf(entry), collection.toList())
    }

    @Test
    fun testMultipleNamedEntries() {
        val collection = TestCollection()

        val entry0 = collection.createOrGet("hello")
        assertEquals(0, entry0.id)
        assertEquals("hello".krHashCode(), entry0.nameHash)

        val entry1 = collection.createOrGet("world")
        assertEquals(1, entry1.id)
        assertEquals("world".krHashCode(), entry1.nameHash)

        assertEquals(2, collection.size)
        assertEquals(2, collection.capacity)

        assertEquals(entry0, collection.createOrGet("hello"))
        assertEquals(entry1, collection.createOrGet("world"))

        assertEquals(2, collection.size)
        assertEquals(2, collection.capacity)

        assertTrue(collection.contains("hello"))
        assertTrue(collection.contains("world"))
        assertFalse(collection.contains("!"))

        assertEquals(entry0, collection["hello"])
        assertEquals(entry1, collection["world"])
        assertNull(collection["!"])

        assertEquals(listOf(entry0, entry1), collection.toList())
    }

    @Test
    fun testRename() {
        val collection = TestCollection()

        val entry = collection.createOrGet("hello")
        assertEquals(0, entry.id)
        assertEquals("hello".krHashCode(), entry.nameHash)

        assertTrue(collection.contains("hello"))
        assertFalse(collection.contains("world"))

        assertEquals(entry, collection["hello"])
        assertNull(collection["world"])

        entry.setName("world")

        assertFalse(collection.contains("hello"))
        assertTrue(collection.contains("world"))

        assertNull(collection["hello"])
        assertEquals(entry, collection["world"])
    }

    @Test
    fun testSingleEntrySetName() {
        val collection = TestCollection()

        val entry = collection.createOrGet(0)
        assertEquals(0, entry.id)
        assertEquals(-1, entry.nameHash)

        assertFalse(collection.contains("hello"))
        assertNull(collection["hello"])

        entry.setName("hello")
        assertEquals("hello".krHashCode(), entry.nameHash)

        assertTrue(collection.contains("hello"))
        assertEquals(entry, collection["hello"])
    }

    @Test
    fun testSingleEntryResetName() {
        val collection = TestCollection()

        val entry = collection.createOrGet("hello")
        assertEquals(0, entry.id)
        assertEquals("hello".krHashCode(), entry.nameHash)

        assertTrue(collection.contains("hello"))
        assertEquals(entry, collection["hello"])

        entry.nameHash = -1
        assertEquals(-1, entry.nameHash)

        assertFalse(collection.contains("hello"))
        assertNull(collection["hello"])
    }

    @Test
    fun testMultipleEntriesSetName() {
        val collection = TestCollection()

        val entry0 = collection.createOrGet(0)
        assertEquals(0, entry0.id)
        assertEquals(-1, entry0.nameHash)

        val entry1 = collection.createOrGet(1)
        assertEquals(1, entry1.id)
        assertEquals(-1, entry1.nameHash)

        assertFalse(collection.contains("hello"))
        assertNull(collection["hello"])

        entry0.setName("hello")
        assertEquals("hello".krHashCode(), entry0.nameHash)

        assertTrue(collection.contains("hello"))
        assertEquals(entry0, collection["hello"])
    }

    @Test
    fun testMultipleEntriesResetName() {
        val collection = TestCollection()

        val entry0 = collection.createOrGet("hello")
        assertEquals(0, entry0.id)
        assertEquals("hello".krHashCode(), entry0.nameHash)

        val entry1 = collection.createOrGet(1)
        assertEquals(1, entry1.id)
        assertEquals(-1, entry1.nameHash)

        assertTrue(collection.contains("hello"))
        assertEquals(entry0, collection["hello"])

        entry0.nameHash = -1
        assertEquals(-1, entry0.nameHash)

        assertFalse(collection.contains("hello"))
        assertNull(collection["hello"])
    }

    @Test
    fun testSingleEntryRemove() {
        val collection = TestCollection()

        val entry = collection.createOrGet(0)
        assertEquals(0, entry.id)
        assertEquals(-1, entry.nameHash)

        assertEquals(1, collection.size)
        assertEquals(1, collection.capacity)
        assertEquals(entry, collection[0])
        assertEquals(listOf(entry), collection.toList())

        entry.remove()

        assertEquals(0, collection.size)
        assertEquals(0, collection.capacity)
        assertNull(collection[0])
        assertEquals(emptyList(), collection.toList())

        entry.setName("hello")
        assertEquals("hello".krHashCode(), entry.nameHash)

        assertFalse(collection.contains("hello"))
        assertNull(collection["hello"])

        entry.remove()
        assertEquals(emptyList(), collection.toList())
    }

    @Test
    fun testSingleEntryRemoveById() {
        val collection = TestCollection()

        val entry = collection.createOrGet(0)
        assertEquals(0, entry.id)
        assertEquals(-1, entry.nameHash)

        assertEquals(1, collection.size)
        assertEquals(1, collection.capacity)
        assertEquals(entry, collection[0])
        assertEquals(listOf(entry), collection.toList())

        collection.remove(0)

        assertEquals(0, collection.size)
        assertEquals(0, collection.capacity)
        assertNull(collection[0])
        assertEquals(emptyList(), collection.toList())

        entry.setName("hello")
        assertEquals("hello".krHashCode(), entry.nameHash)

        assertFalse(collection.contains("hello"))
        assertNull(collection["hello"])

        collection.remove(0)
        assertEquals(emptyList(), collection.toList())
    }

    @Test
    fun testSingleEntryRemoveByName() {
        val collection = TestCollection()

        val entry = collection.createOrGet("hello")
        assertEquals(0, entry.id)
        assertEquals("hello".krHashCode(), entry.nameHash)

        assertEquals(1, collection.size)
        assertEquals(1, collection.capacity)
        assertEquals(entry, collection[0])
        assertEquals(listOf(entry), collection.toList())

        collection.remove("hello")

        assertEquals(0, collection.size)
        assertEquals(0, collection.capacity)
        assertNull(collection[0])
        assertEquals(emptyList(), collection.toList())

        entry.setName("world")
        assertEquals("world".krHashCode(), entry.nameHash)

        assertFalse(collection.contains("world"))
        assertNull(collection["world"])

        collection.remove("hello")
        assertEquals(emptyList(), collection.toList())
    }

    @Test
    fun testMultipleEntriesRemove() {
        val collection = TestCollection()

        val entry0 = collection.createOrGet(0)
        assertEquals(0, entry0.id)
        assertEquals(-1, entry0.nameHash)

        val entry1 = collection.createOrGet(1)
        assertEquals(1, entry1.id)
        assertEquals(-1, entry1.nameHash)

        assertEquals(2, collection.size)
        assertEquals(2, collection.capacity)
        assertEquals(entry0, collection[0])
        assertEquals(entry1, collection[1])
        assertEquals(listOf(entry0, entry1), collection.toList())

        entry0.remove()

        assertEquals(1, collection.size)
        assertEquals(2, collection.capacity)
        assertNull(collection[0])
        assertEquals(entry1, collection[1])
        assertEquals(listOf(entry1), collection.toList())

        entry0.setName("world")
        assertEquals("world".krHashCode(), entry0.nameHash)

        assertFalse(collection.contains("world"))
        assertNull(collection["world"])

        entry0.remove()
        assertEquals(listOf(entry1), collection.toList())
    }

    @Test
    fun testMultipleEntriesRemoveById() {
        val collection = TestCollection()

        val entry0 = collection.createOrGet(0)
        assertEquals(0, entry0.id)
        assertEquals(-1, entry0.nameHash)

        val entry1 = collection.createOrGet(1)
        assertEquals(1, entry1.id)
        assertEquals(-1, entry1.nameHash)

        assertEquals(2, collection.size)
        assertEquals(2, collection.capacity)
        assertEquals(entry0, collection[0])
        assertEquals(entry1, collection[1])
        assertEquals(listOf(entry0, entry1), collection.toList())

        collection.remove(0)

        assertEquals(1, collection.size)
        assertEquals(2, collection.capacity)
        assertNull(collection[0])
        assertEquals(entry1, collection[1])
        assertEquals(listOf(entry1), collection.toList())

        entry0.setName("world")
        assertEquals("world".krHashCode(), entry0.nameHash)

        assertFalse(collection.contains("world"))
        assertNull(collection["world"])

        collection.remove(0)
        assertEquals(listOf(entry1), collection.toList())
    }

    @Test
    fun testMultipleEntriesRemoveByName() {
        val collection = TestCollection()

        val entry0 = collection.createOrGet("hello")
        assertEquals(0, entry0.id)
        assertEquals("hello".krHashCode(), entry0.nameHash)

        val entry1 = collection.createOrGet("world")
        assertEquals(1, entry1.id)
        assertEquals("world".krHashCode(), entry1.nameHash)

        val entry2 = collection.createOrGet("abc")
        assertEquals(2, entry2.id)
        assertEquals("abc".krHashCode(), entry2.nameHash)

        assertEquals(3, collection.size)
        assertEquals(3, collection.capacity)
        assertEquals(entry0, collection["hello"])
        assertEquals(entry1, collection["world"])
        assertEquals(entry2, collection["abc"])
        assertEquals(listOf(entry0, entry1, entry2), collection.toList())

        collection.remove("hello")

        assertEquals(2, collection.size)
        assertEquals(3, collection.capacity)
        assertNull(collection["hello"])
        assertEquals(entry1, collection["world"])
        assertEquals(entry2, collection["abc"])
        assertEquals(listOf(entry1, entry2), collection.toList())

        entry0.setName("!")
        assertEquals("!".krHashCode(), entry0.nameHash)

        assertFalse(collection.contains("!"))
        assertNull(collection["!"])

        collection.remove("hello")
        assertEquals(listOf(entry1, entry2), collection.toList())
    }

    @Test
    fun testRemoveLastNamedEntry() {
        val collection = TestCollection()

        val entry0 = collection.createOrGet("hello")
        assertEquals(0, entry0.id)
        assertEquals("hello".krHashCode(), entry0.nameHash)

        val entry1 = collection.createOrGet(1)
        assertEquals(1, entry1.id)
        assertEquals(-1, entry1.nameHash)

        val entry2 = collection.createOrGet(2)
        assertEquals(2, entry2.id)
        assertEquals(-1, entry2.nameHash)

        assertEquals(3, collection.size)
        assertEquals(3, collection.capacity)
        assertEquals(entry0, collection["hello"])
        assertEquals(entry1, collection[1])
        assertEquals(entry2, collection[2])
        assertEquals(listOf(entry0, entry1, entry2), collection.toList())

        entry0.remove()

        assertNull(collection["hello"])
        assertEquals(entry1, collection[1])
        assertEquals(entry2, collection[2])
        assertEquals(listOf(entry1, entry2), collection.toList())
    }

    @Test
    fun testNonContiguousIds() {
        val collection = TestCollection()

        val entry1 = collection.createOrGet(1)
        assertEquals(1, entry1.id)
        assertEquals(-1, entry1.nameHash)

        val entry4 = collection.createOrGet(4)
        assertEquals(4, entry4.id)
        assertEquals(-1, entry4.nameHash)

        val entry3 = collection.createOrGet(3)
        assertEquals(3, entry3.id)
        assertEquals(-1, entry3.nameHash)

        assertEquals(3, collection.size)
        assertEquals(5, collection.capacity)
        assertEquals(listOf(entry1, entry3, entry4), collection.toList())

        val entry0 = collection.createOrGet("hello")
        assertEquals(0, entry0.id)
        assertEquals("hello".krHashCode(), entry0.nameHash)

        val entry2 = collection.createOrGet("world")
        assertEquals(2, entry2.id)
        assertEquals("world".krHashCode(), entry2.nameHash)

        val entry5 = collection.createOrGet("!")
        assertEquals(5, entry5.id)
        assertEquals("!".krHashCode(), entry5.nameHash)

        assertEquals(6, collection.size)
        assertEquals(6, collection.capacity)
        assertEquals(listOf(entry0, entry1, entry2, entry3, entry4, entry5), collection.toList())
    }

    @Test
    fun testNonContiguousIdsSingleEntry() {
        val collection = TestCollection()

        val entry1 = collection.createOrGet(1)
        assertEquals(1, entry1.id)
        assertEquals(-1, entry1.nameHash)

        assertEquals(1, collection.size)
        assertEquals(2, collection.capacity)
        assertEquals(listOf(entry1), collection.toList())

        val entry0 = collection.createOrGet("hello")
        assertEquals(0, entry0.id)
        assertEquals("hello".krHashCode(), entry0.nameHash)

        assertEquals(2, collection.size)
        assertEquals(2, collection.capacity)
        assertEquals(listOf(entry0, entry1), collection.toList())
    }

    @Test
    fun testEmptyIterator() {
        val collection = TestCollection()

        val it = collection.iterator()

        assertThrows<IllegalStateException> {
            it.remove()
        }

        assertFalse(it.hasNext())

        assertThrows<NoSuchElementException> {
            it.next()
        }

        assertThrows<IllegalStateException> {
            it.remove()
        }
    }

    @Test
    fun testSingleEntryIterator() {
        val collection = TestCollection()

        val entry = collection.createOrGet(0)
        assertEquals(0, entry.id)
        assertEquals(-1, entry.nameHash)

        val it = collection.iterator()

        assertThrows<IllegalStateException> {
            it.remove()
        }

        assertTrue(it.hasNext())
        assertEquals(entry, it.next())

        assertFalse(it.hasNext())

        assertThrows<NoSuchElementException> {
            it.next()
        }

        assertThrows<IllegalStateException> {
            it.remove()
        }
    }

    @Test
    fun testMultipleEntriesIterator() {
        val collection = TestCollection()

        val entry0 = collection.createOrGet(0)
        assertEquals(0, entry0.id)
        assertEquals(-1, entry0.nameHash)

        val entry1 = collection.createOrGet(1)
        assertEquals(1, entry1.id)
        assertEquals(-1, entry1.nameHash)

        val it = collection.iterator()

        assertThrows<IllegalStateException> {
            it.remove()
        }

        assertTrue(it.hasNext())
        assertEquals(entry0, it.next())

        assertTrue(it.hasNext())
        assertEquals(entry1, it.next())

        assertFalse(it.hasNext())

        assertThrows<NoSuchElementException> {
            it.next()
        }

        assertThrows<IllegalStateException> {
            it.remove()
        }
    }

    @Test
    fun testSingleEntryRemoveIterator() {
        val collection = TestCollection()

        val entry = collection.createOrGet(0)
        assertEquals(0, entry.id)
        assertEquals(-1, entry.nameHash)

        val it = collection.iterator()

        assertTrue(it.hasNext())
        assertEquals(entry, it.next())

        it.remove()

        assertThrows<IllegalStateException> {
            it.remove()
        }

        assertFalse(it.hasNext())

        assertEquals(0, collection.size)
        assertEquals(0, collection.capacity)
        assertEquals(emptyList(), collection.toList())
    }

    @Test
    fun testMultipleEntriesRemoveIterator() {
        val collection = TestCollection()

        val entry0 = collection.createOrGet(0)
        assertEquals(0, entry0.id)
        assertEquals(-1, entry0.nameHash)

        val entry1 = collection.createOrGet(1)
        assertEquals(1, entry1.id)
        assertEquals(-1, entry1.nameHash)

        val it = collection.iterator()

        assertTrue(it.hasNext())
        assertEquals(entry0, it.next())

        it.remove()

        assertThrows<IllegalStateException> {
            it.remove()
        }

        assertTrue(it.hasNext())
        assertEquals(entry1, it.next())

        it.remove()

        assertThrows<IllegalStateException> {
            it.remove()
        }

        assertFalse(it.hasNext())

        assertEquals(0, collection.size)
        assertEquals(0, collection.capacity)
        assertEquals(emptyList(), collection.toList())
    }

    @Test
    fun testNameHashCollision() {
        val collection = TestCollection()
        collection.createOrGet("hello")

        val entry = collection.createOrGet(1)
        assertThrows<IllegalStateException> {
            entry.setName("hello")
        }
    }
}
