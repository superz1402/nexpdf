package com.nexpdf.app.data

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.IOException

data class WordRect(val dx: Float, val dy: Float, val dw: Float, val dh: Float)

object PdfText {

    fun wordRects(context: Context, uri: Uri, pageIndex: Int): List<WordRect> {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("Cannot open $uri")
        pfd.use {
            PDDocument.load(ParcelFileDescriptor.AutoCloseInputStream(pfd)).use { doc ->
                val page = doc.getPage(pageIndex)
                val rect = page.mediaBox
                val rot = page.rotation
                val wU = rect.width
                val hU = rect.height
                val collector = WordCollector()
                collector.setStartPage(pageIndex + 1)
                collector.setEndPage(pageIndex + 1)
                collector.setSortByPosition(true)
                collector.getText(doc)
                return collector.words.map { (x, yTop, w, h) ->
                    toDisplay(x, yTop, w, h, wU, hU, rot)
                }
            }
        }
    }

    private data class Quad(val x: Float, val yTop: Float, val w: Float, val h: Float)

    private class WordCollector : PDFTextStripper() {
        val words = mutableListOf<Quad>()
        var x0 = 0f
        var yTop = 0f
        var curWidth = 0f
        var curHeight = 0f
        var started = false

        @Throws(IOException::class)
        override fun writeString(text: String, textPositions: List<TextPosition>) {            textPositions.forEach { p ->
                val x = p.getXDirAdj()
                val y = p.getYDirAdj()
                val w = p.getWidthDirAdj()
                val h = p.getHeightDir()
                val gap = if (started) x - (x0 + curWidth) else 0f
                if (started && gap > p.getFontSize() * 0.45f) flush()
                if (!started) {
                    x0 = x
                    yTop = y - h
                    curWidth = 0f
                    curHeight = 0f
                    started = true
                }
                curWidth = (x + w) - x0
                curHeight = maxOf(curHeight, h)
            }
        }

        override fun endPage(page: PDPage) {
            flush()
            super.endPage(page)
        }

        fun flush() {
            if (started && curWidth > 0 && curHeight > 0) {
                words.add(Quad(x0, yTop, curWidth, curHeight))
            }
            started = false
        }
    }

    private fun toDisplay(
        x: Float, yTop: Float, w: Float, h: Float,
        wU: Float, hU: Float, rot: Int
    ): WordRect {
        return when (rot % 4) {
            1 -> WordRect(
                (hU - yTop - h) / hU, x / wU, h / hU, w / wU
            )
            2 -> WordRect(
                1f - (x + w) / wU, 1f - (yTop + h) / hU, w / wU, h / hU
            )
            3 -> WordRect(
                yTop / hU, 1f - (x + w) / wU, h / hU, w / wU
            )
            else -> WordRect(x / wU, yTop / hU, w / wU, h / hU)
        }
    }
}
