package org.openrs2.cache

import org.openrs2.util.krHashCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

        assertFailsWith<IllegalArgumentException> {
            -1 in collection
        }

        assertFailsWith<IllegalArgumentException> {
            collection.containsNamed(-1)
        }

        assertFailsWith<IllegalArgumentException> {
            collection[-1]
        }

        assertFailsWith<IllegalArgumentException> {
            collection.getNamed(-1)
        }

        assertFailsWith<IllegalArgumentException> {
            collection.createOrGet(-1)
        }

        assertFailsWith<IllegalArgumentException> {
            collection.createOrGetNamed(-1)
        }

        assertFailsWith<IllegalArgumentException> {
            collection.remove(-1)
        }

        assertFailsWith<IllegalArgumentException> {
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

        assertTrue(0 in collection)
        assertFalse(1 in collection)

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

        assertTrue(0 in collection)
        assertTrue(1 in collection)
        assertFalse(2 in collection)

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

        assertTrue("hello" in collection)
        assertFalse("world" in collection)

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

        assertTrue("hello" in collection)
        assertTrue("world" in collection)
        assertFalse("!" in collection)

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

        assertTrue("hello" in collection)
        assertFalse("world" in collection)

        assertEquals(entry, collection["hello"])
        assertNull(collection["world"])

        entry.setName("world")

        assertFalse("hello" in collection)
        assertTrue("world" in collection)

        assertNull(collection["hello"])
        assertEquals(entry, collection["world"])
    }

    @Test
    fun testSingleEntrySetName() {
        val collection = TestCollection()

        val entry = collection.createOrGet(0)
        assertEquals(0, entry.id)
        assertEquals(-1, entry.nameHash)

        assertFalse("hello" in collection)
        assertNull(collection["hello"])

        entry.setName("hello")
        assertEquals("hello".krHashCode(), entry.nameHash)

        assertTrue("hello" in collection)
        assertEquals(entry, collection["hello"])
    }

    @Test
    fun testSingleEntryResetName() {
        val collection = TestCollection()

        val entry = collection.createOrGet("hello")
        assertEquals(0, entry.id)
        assertEquals("hello".krHashCode(), entry.nameHash)

        assertTrue("hello" in collection)
        assertEquals(entry, collection["hello"])

        entry.nameHash = -1
        assertEquals(-1, entry.nameHash)

        assertFalse("hello" in collection)
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

        assertFalse("hello" in collection)
        assertNull(collection["hello"])

        entry0.setName("hello")
        assertEquals("hello".krHashCode(), entry0.nameHash)

        assertTrue("hello" in collection)
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

        assertTrue("hello" in collection)
        assertEquals(entry0, collection["hello"])

        entry0.nameHash = -1
        assertEquals(-1, entry0.nameHash)

        assertFalse("hello" in collection)
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

        assertFalse("hello" in collection)
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

        assertEquals(entry, collection.remove(0))

        assertEquals(0, collection.size)
        assertEquals(0, collection.capacity)
        assertNull(collection[0])
        assertEquals(emptyList(), collection.toList())

        entry.setName("hello")
        assertEquals("hello".krHashCode(), entry.nameHash)

        assertFalse("hello" in collection)
        assertNull(collection["hello"])

        assertNull(collection.remove(0))
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

        assertEquals(entry, collection.remove("hello"))

        assertEquals(0, collection.size)
        assertEquals(0, collection.capacity)
        assertNull(collection[0])
        assertEquals(emptyList(), collection.toList())

        entry.setName("world")
        assertEquals("world".krHashCode(), entry.nameHash)

        assertFalse("world" in collection)
        assertNull(collection["world"])

        assertNull(collection.remove("hello"))
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

        assertFalse("world" in collection)
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

        assertEquals(entry0, collection.remove(0))

        assertEquals(1, collection.size)
        assertEquals(2, collection.capacity)
        assertNull(collection[0])
        assertEquals(entry1, collection[1])
        assertEquals(listOf(entry1), collection.toList())

        entry0.setName("world")
        assertEquals("world".krHashCode(), entry0.nameHash)

        assertFalse("world" in collection)
        assertNull(collection["world"])

        assertNull(collection.remove(0))
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

        assertEquals(entry0, collection.remove("hello"))

        assertEquals(2, collection.size)
        assertEquals(3, collection.capacity)
        assertNull(collection["hello"])
        assertEquals(entry1, collection["world"])
        assertEquals(entry2, collection["abc"])
        assertEquals(listOf(entry1, entry2), collection.toList())

        entry0.setName("!")
        assertEquals("!".krHashCode(), entry0.nameHash)

        assertFalse("!" in collection)
        assertNull(collection["!"])

        assertNull(collection.remove("hello"))
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

        assertFailsWith<IllegalStateException> {
            it.remove()
        }

        assertFalse(it.hasNext())

        assertFailsWith<NoSuchElementException> {
            it.next()
        }

        assertFailsWith<IllegalStateException> {
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

        assertFailsWith<IllegalStateException> {
            it.remove()
        }

        assertTrue(it.hasNext())
        assertEquals(entry, it.next())

        assertFalse(it.hasNext())

        assertFailsWith<NoSuchElementException> {
            it.next()
        }

        assertFailsWith<IllegalStateException> {
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

        assertFailsWith<IllegalStateException> {
            it.remove()
        }

        assertTrue(it.hasNext())
        assertEquals(entry0, it.next())

        assertTrue(it.hasNext())
        assertEquals(entry1, it.next())

        assertFalse(it.hasNext())

        assertFailsWith<NoSuchElementException> {
            it.next()
        }

        assertFailsWith<IllegalStateException> {
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

        assertFailsWith<IllegalStateException> {
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

        assertFailsWith<IllegalStateException> {
            it.remove()
        }

        assertTrue(it.hasNext())
        assertEquals(entry1, it.next())

        it.remove()

        assertFailsWith<IllegalStateException> {
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

        val entry0 = collection.createOrGet(0)
        val entry1 = collection.createOrGet(1)
        val entry2 = collection.createOrGet(2)

        entry0.setName("hello")
        entry1.setName("hello")
        entry2.setName("hello")

        assertEquals(entry0, collection["hello"])

        entry0.clearName()
        assertEquals(entry1, collection["hello"])

        entry1.clearName()
        assertEquals(entry2, collection["hello"])

        entry2.clearName()
        assertNull(collection["hello"])
    }

    @Test
    fun testNameHashCollisionOppositeOrder() {
        val collection = TestCollection()

        val entry0 = collection.createOrGet(0)
        val entry1 = collection.createOrGet(1)
        val entry2 = collection.createOrGet(2)

        entry2.setName("hello")
        entry1.setName("hello")
        entry0.setName("hello")

        assertEquals(entry0, collection["hello"])

        entry2.clearName()
        assertEquals(entry0, collection["hello"])

        entry1.clearName()
        assertEquals(entry0, collection["hello"])

        entry0.clearName()
        assertNull(collection["hello"])
    }

    @Test
    fun testNameHashCollisionRemove() {
        val collection = TestCollection()

        val entry0 = collection.createOrGet(0)
        val entry1 = collection.createOrGet(1)
        val entry2 = collection.createOrGet(2)

        entry0.setName("hello")
        entry1.setName("hello")
        entry2.setName("hello")

        assertEquals(entry0, collection["hello"])

        entry0.remove()
        assertEquals(entry1, collection["hello"])

        entry1.remove()
        assertEquals(entry2, collection["hello"])

        entry2.remove()
        assertNull(collection["hello"])
    }

    @Test
    fun testNameHashCollisionOppositeOrderRemove() {
        val collection = TestCollection()

        val entry0 = collection.createOrGet(0)
        val entry1 = collection.createOrGet(1)
        val entry2 = collection.createOrGet(2)

        entry2.setName("hello")
        entry1.setName("hello")
        entry0.setName("hello")

        assertEquals(entry0, collection["hello"])

        entry2.remove()
        assertEquals(entry0, collection["hello"])

        entry1.remove()
        assertEquals(entry0, collection["hello"])

        entry0.remove()
        assertNull(collection["hello"])
    }

    @Test
    fun testNameHashCollisionRename() {
        val collection = TestCollection()

        val entry0 = collection.createOrGet(0)
        val entry1 = collection.createOrGet(1)
        val entry2 = collection.createOrGet(2)

        entry0.setName("hello")
        entry1.setName("hello")
        entry2.setName("hello")

        assertEquals(entry0, collection["hello"])

        entry0.setName("entry0")
        assertEquals(entry0, collection["entry0"])
        assertEquals(entry1, collection["hello"])

        entry1.setName("entry1")
        assertEquals(entry0, collection["entry0"])
        assertEquals(entry1, collection["entry1"])
        assertEquals(entry2, collection["hello"])

        entry2.setName("entry2")
        assertEquals(entry0, collection["entry0"])
        assertEquals(entry1, collection["entry1"])
        assertEquals(entry2, collection["entry2"])
        assertNull(collection["hello"])
    }

    @Test
    fun testNameHashCollisionOppositeOrderRename() {
        val collection = TestCollection()

        val entry0 = collection.createOrGet(0)
        val entry1 = collection.createOrGet(1)
        val entry2 = collection.createOrGet(2)

        entry2.setName("hello")
        entry1.setName("hello")
        entry0.setName("hello")

        assertEquals(entry0, collection["hello"])

        entry2.setName("entry2")
        assertEquals(entry2, collection["entry2"])
        assertEquals(entry0, collection["hello"])

        entry1.setName("entry1")
        assertEquals(entry1, collection["entry1"])
        assertEquals(entry2, collection["entry2"])
        assertEquals(entry0, collection["hello"])

        entry0.setName("entry0")
        assertEquals(entry0, collection["entry0"])
        assertEquals(entry1, collection["entry1"])
        assertEquals(entry2, collection["entry2"])
        assertNull(collection["hello"])
    }
}
