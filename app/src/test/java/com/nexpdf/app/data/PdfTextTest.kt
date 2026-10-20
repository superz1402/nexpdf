package com.nexpdf.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PdfTextTest {

    private fun g(x: Float, yBaseline: Float, w: Float, h: Float = 10f, fontSize: Float = 12f) =
        GlyphVec(x, yBaseline, w, h, fontSize)

    @Test
    fun contiguousGlyphsFormOneWord() {
        val quads = PdfText.groupWords(
            listOf(g(100f, 200f, 6f), g(106f, 200f, 6f), g(112f, 200f, 6f))
        )
        assertEquals(1, quads.size)
        val q = quads[0]
        assertEquals(100f, q.x, 0.01f)
        assertEquals(190f, q.yTop, 0.01f)
        assertEquals(18f, q.w, 0.01f)
        assertEquals(10f, q.h, 0.01f)
    }

    @Test
    fun largeGapSplitsWords() {
        val quads = PdfText.groupWords(
            listOf(g(100f, 200f, 6f), g(200f, 200f, 6f))
        )
        assertEquals(2, quads.size)
        assertEquals(200f, quads[1].x, 0.01f)
    }

    @Test
    fun newLineFlushesEvenWithNegativeGap() {
        val quads = PdfText.groupWords(
            listOf(g(300f, 200f, 400f), g(100f, 220f, 40f))
        )
        assertEquals(2, quads.size)
        assertEquals(210f, quads[1].yTop, 0.01f)
    }

    @Test
    fun heightsGrowToTallestGlyph() {
        val quads = PdfText.groupWords(
            listOf(g(100f, 200f, 6f, h = 10f), g(106f, 200f, 6f, h = 14f))
        )
        assertEquals(14f, quads[0].h, 0.01f)
    }

    @Test
    fun toDisplayRotZeroIsIdentityScaled() {
        val r = PdfText.toDisplay(100f, 100f, 50f, 20f, 400f, 600f, 0)
        assertEquals(0.25f, r.dx, 0.001f)
        assertEquals(100f / 600f, r.dy, 0.001f)
        assertEquals(0.125f, r.dw, 0.001f)
        assertEquals(20f / 600f, r.dh, 0.001f)
    }

    @Test
    fun toDisplayRot180Mirrors() {
        val r = PdfText.toDisplay(100f, 200f, 40f, 20f, 400f, 600f, 180)
        assertEquals((400f - 140f) / 400f, r.dx, 0.001f)
        assertEquals((600f - 220f) / 600f, r.dy, 0.001f)
    }

    @Test
    fun toDisplayRot90SwapsAxes() {
        val r = PdfText.toDisplay(100f, 200f, 60f, 20f, 400f, 800f, 90)
        assertEquals((800f - 220f) / 800f, r.dx, 0.001f)
        assertEquals(100f / 400f, r.dy, 0.001f)
        assertEquals(20f / 800f, r.dw, 0.001f)
        assertEquals(60f / 400f, r.dh, 0.001f)
    }

    @Test
    fun displayRectStaysInUnitSquareAllRotations() {
        for (rot in listOf(0, 90, 180, 270)) {
            val r = PdfText.toDisplay(10f, 20f, 380f, 50f, 400f, 600f, rot)
            assertTrue(r.dx >= -0.001f && r.dx + r.dw <= 1.001f, "rot=$rot dx=${r.dx} dw=${r.dw}")
            assertTrue(r.dy >= -0.001f && r.dy + r.dh <= 1.001f, "rot=$rot dy=${r.dy} dh=${r.dh}")
        }
    }

    @Test
    fun wordBreakOnHorizontalGap() {
        val prev = g(100f, 200f, 8f, fontSize = 12f)
        assertTrue(PdfText.isWordBreak(prev, g(114f, 200f, 8f, fontSize = 12f)))
        assertFalse(PdfText.isWordBreak(prev, g(108f, 200f, 8f, fontSize = 12f)))
    }

    @Test
    fun wordBreakOnBaselineChange() {
        val prev = g(100f, 200f, 8f, fontSize = 12f)
        assertTrue(PdfText.isWordBreak(prev, g(108f, 216f, 8f, fontSize = 12f)))
        assertFalse(PdfText.isWordBreak(prev, g(108f, 204f, 8f, fontSize = 12f)))
    }

    @Test
    fun unionCoversAllQuads() {
        val u = PdfText.union(
            listOf(Quad(10f, 100f, 40f, 10f), Quad(30f, 114f, 50f, 12f))
        )
        assertEquals(10f, u.x, 0.01f)
        assertEquals(100f, u.yTop, 0.01f)
        assertEquals(70f, u.w, 0.01f)
        assertEquals(26f, u.h, 0.01f)
    }
}
