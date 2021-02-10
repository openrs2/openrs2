package org.openrs2.util.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class ForestDisjointSetTest {
    @Test
    fun testAdd() {
        val set = ForestDisjointSet<Int>()
        assertEquals(0, set.elements)
        assertEquals(0, set.partitions)

        val set1 = set.add(1)
        assertEquals(1, set.elements)
        assertEquals(1, set.partitions)

        assertEquals(set1, set.add(1))
        assertEquals(1, set.elements)
        assertEquals(1, set.partitions)

        set.add(2)
        assertEquals(2, set.elements)
        assertEquals(2, set.partitions)
    }

    @Test
    fun testUnionEqualRank() {
        val set = ForestDisjointSet<Int>()

        val set1 = set.add(1)
        val set2 = set.add(2)
        set.union(set1, set2)

        assertEquals(2, set.elements)
        assertEquals(1, set.partitions)
    }

    @Test
    fun testUnionSameRoot() {
        val set = ForestDisjointSet<Int>()

        val set1 = set.add(1)
        val set2 = set.add(2)
        set.union(set1, set2)
        set.union(set1, set2)

        assertEquals(2, set.elements)
        assertEquals(1, set.partitions)
    }

    @Test
    fun testUnionXRankGreater() {
        val set = ForestDisjointSet<Int>()

        val set1 = set.add(1)
        val set2 = set.add(2)
        val set3 = set.add(3)
        set.union(set1, set2)
        set.union(set1, set3)

        assertEquals(3, set.elements)
        assertEquals(1, set.partitions)
    }

    @Test
    fun testUnionYRankGreater() {
        val set = ForestDisjointSet<Int>()

        val set1 = set.add(1)
        val set2 = set.add(2)
        val set3 = set.add(3)
        set.union(set1, set2)
        set.union(set3, set1)

        assertEquals(3, set.elements)
        assertEquals(1, set.partitions)
    }

    @Test
    fun testPartitionEquals() {
        val set = ForestDisjointSet<Int>()

        val set1 = set.add(1)
        val set2 = set.add(2)
        assertEquals(set1, set1)
        assertEquals(set2, set2)
        assertNotEquals(set1, set2)
        assertNotEquals(set2, set1)
    }

    @Test
    fun testPartitionHashCode() {
        val set = ForestDisjointSet<Int>()

        assertEquals(1.hashCode(), set.add(1).hashCode())
        assertEquals(2.hashCode(), set.add(2).hashCode())
    }

    @Test
    fun testPartitionToString() {
        val set = ForestDisjointSet<Int>()
        assertEquals("1", set.add(1).toString())
    }

    @Test
    fun testPartitionIterator() {
        val set = ForestDisjointSet<Int>()

        val set1 = set.add(1)
        set.union(set1, set.add(2))
        set.union(set1, set.add(3))

        assertEquals(setOf(1, 2, 3), set1.toSet())
    }

    @Test
    fun testIterator() {
        val set = ForestDisjointSet<Int>()

        set.add(1)
        set.union(set.add(2), set.add(3))

        assertEquals(setOf(setOf(1), setOf(2, 3)), set.map { it.toSet() }.toSet())
    }

    @Test
    fun testGet() {
        val set = ForestDisjointSet<Int>()

        val set1 = set.add(1)
        assertEquals(set1, set[1])

        assertNull(set[2])
    }
}
