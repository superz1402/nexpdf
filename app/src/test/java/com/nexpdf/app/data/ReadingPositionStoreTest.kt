package com.nexpdf.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReadingPositionStoreTest {

    private fun pos(key: String, title: String, page: Int, at: Long) =
        ReadingPositionStore.Position(key, title, page, at)

    @Test
    fun mostRecentFirst() {
        val docs = mapOf(
            "a" to pos("a", "Freshly created1.pdf", 3, 100L),
            "b" to pos("b", "Freshly created2.pdf", 5, 300L),
            "c" to pos("c", "Freshly created3.pdf", 2, 200L)
        )
        val top = ReadingPositionStore.topEntries(docs)
        assertEquals(listOf("b", "c", "a"), top.map { it.key })
        assertEquals(5, top[0].page)
    }

    @Test
    fun cappedAtHundred() {
        val docs = (0 until 150).associate {
            "k$it" to pos("k$it", "file$it.pdf", it, it.toLong())
        }
        val top = ReadingPositionStore.topEntries(docs)
        assertEquals(100, top.size)
        assertEquals(149, top.first().updatedAt)
        assertEquals(50, top.last().updatedAt)
    }

    @Test
    fun capKeepsNewestWhenOverLimit() {
        val docs = mapOf(
            "old" to pos("old", "doc.pdf".repeat(30), 1, 1L),
            "new" to pos("new", "ndf.pdf", 9, 100L)
        )
        val top = ReadingPositionStore.topEntries(docs)
        assertTrue(top.all { it.title.length < 500 })
        assertEquals(2, top.size)
    }
}
