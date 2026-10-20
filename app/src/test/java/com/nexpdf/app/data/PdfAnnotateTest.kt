package com.nexpdf.app.data

import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PdfAnnotateTest {

    private val rect = PDRectangle(0f, 0f, 500f, 400f)

    @Test
    fun rotZeroDisplayTopLeftMapsToUserTopLeft() {
        val (x, y) = PdfAnnotate.pdfPoint(0.25f, 0.25f, rect, 0)
        assertEquals(125f, x, 0.01f)
        assertEquals(300f, y, 0.01f)
    }

    @Test
    fun rotZeroDisplayBottomRightMapsToUserBottomRight() {
        val (x, y) = PdfAnnotate.pdfPoint(1f, 1f, rect, 0)
        assertEquals(500f, x, 0.01f)
        assertEquals(0f, y, 0.01f)
    }

    @Test
    fun rot90DisplayedWidthIsPageHeight() {
        val (x0d, _) = PdfAnnotate.pdfPoint(0f, 0.5f, rect, 90)
        val (x1d, _) = PdfAnnotate.pdfPoint(0f, 0f, rect, 90)
        assertEquals(200f, x1d - x0d, 0.01f)
    }

    @Test
    fun rot180MirrorsBothAxes() {
        val (x, y) = PdfAnnotate.pdfPoint(0.2f, 0.1f, rect, 180)
        assertEquals(400f, x, 0.01f)
        assertEquals(40f, y, 0.01f)
    }

    @Test
    fun rotationCornersStayInsidePage() {
        for (rot in listOf(0, 90, 180, 270)) {
            val swapped = rot % 180 != 0
            val xMax = if (swapped) 400f else 500f
            val yMax = if (swapped) 500f else 400f
            for ((dx, dy) in listOf(0f to 0f, 1f to 0f, 0f to 1f, 1f to 1f, 0.5f to 0.5f)) {
                val (x, y) = PdfAnnotate.pdfPoint(dx, dy, rect, rot)
                assertTrue(x >= -0.01f && x <= xMax + 0.01f, "rot=$rot x=$x")
                assertTrue(y >= -0.01f && y <= yMax + 0.01f, "rot=$rot y=$y")
            }
        }
    }

    @Test
    fun rotationRoundTripsThroughDisplayConversion() {
        for (rot in 0..3) {
            val display = 0.3f to 0.6f
            val (ux, uy) = PdfAnnotate.pdfPoint(display.first, display.second, rect, rot)
            val back = PdfAnnotate.userToDisplay(ux, uy, rect, rot)
            assertEquals(display.first, back.first, 0.002f, "dx rot=$rot")
            assertEquals(display.second, back.second, 0.002f, "dy rot=$rot")
        }
    }

    @Test
    fun strokeWidthClamped() {
        assertEquals(0.8f, PdfAnnotate.strokeWidth(0.0001f, rect), 0.001f)
        assertEquals(24f, PdfAnnotate.strokeWidth(1f, rect), 0.001f)
        assertEquals(2.5f, PdfAnnotate.strokeWidth(0.005f, rect), 0.001f)
    }

    @Test
    fun rotIndexMapsDegreesWithWraparound() {
        assertEquals(0, PdfAnnotate.rotIndex(0))
        assertEquals(1, PdfAnnotate.rotIndex(90))
        assertEquals(2, PdfAnnotate.rotIndex(180))
        assertEquals(3, PdfAnnotate.rotIndex(270))
        assertEquals(1, PdfAnnotate.rotIndex(450))
        assertEquals(3, PdfAnnotate.rotIndex(-90))
    }
}
