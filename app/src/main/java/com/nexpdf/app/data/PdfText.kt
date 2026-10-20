package com.nexpdf.app.data

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.IOException
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WordRect(val dx: Float, val dy: Float, val dw: Float, val dh: Float)

internal data class GlyphVec(val x: Float, val yBaseline: Float, val w: Float, val h: Float, val fontSize: Float)

internal data class Quad(val x: Float, val yTop: Float, val w: Float, val h: Float)

object PdfText {

    data class TextMatch(val pageIndex: Int, val rect: WordRect)

    fun wordRects(context: Context, uri: Uri, pageIndex: Int): List<WordRect> {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("Cannot open $uri")
        pfd.use {
            PDDocument.load(ParcelFileDescriptor.AutoCloseInputStream(pfd)).use { doc ->
                val page = doc.getPage(pageIndex)
                val rect = page.mediaBox
                val rot = page.rotation
                return wordsOn(doc, pageIndex).map { (quad, _) ->
                    toDisplay(quad.x, quad.yTop, quad.w, quad.h, rect.width, rect.height, rot)
                }
            }
        }
    }

    suspend fun search(context: Context, uri: Uri, query: String): List<TextMatch> =
        withContext(Dispatchers.IO) {
            val q = query.trim().lowercase().replace(Regex("\\s+"), " ")
            if (q.isEmpty()) return@withContext emptyList()
            val tokens = q.split(' ')
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return@withContext emptyList()
            pfd.use {
                PDDocument.load(ParcelFileDescriptor.AutoCloseInputStream(pfd)).use { doc ->
                    val out = mutableListOf<TextMatch>()
                    for (i in 0 until doc.numberOfPages) {
                        val words = wordsOn(doc, i)
                        when (tokens.size) {
                            1 -> words.forEach { w ->
                                if (w.text.lowercase().contains(q)) out += TextMatch(i, display(w.quad, doc, i))
                            }
                            else -> if (words.size >= tokens.size) {
                                for (j in 0..words.size - tokens.size) {
                                    val window = words.subList(j, j + tokens.size)
                                    val joined = window.joinToString(" ") { g -> g.text.lowercase() }
                                    if (joined == q) out += TextMatch(i, display(union(window.map(WordEntry::quad)), doc, i))
                                }
                            }
                        }
                        if (out.size >= 600) break
                    }
                    out
                }
            }
        }

    internal fun wordsOn(doc: PDDocument, pageIndex: Int): List<WordEntry> {
        val c = WordTextCollector()
        c.setStartPage(pageIndex + 1)
        c.setEndPage(pageIndex + 1)
        c.setSortByPosition(true)
        c.getText(doc)
        return c.words
    }

    private fun display(quad: Quad, doc: PDDocument, pageIndex: Int): WordRect {
        val page = doc.getPage(pageIndex)
        val rect = page.mediaBox
        return toDisplay(quad.x, quad.yTop, quad.w, quad.h, rect.width, rect.height, page.rotation)
    }

    internal fun union(quads: List<Quad>): Quad {
        val x0 = quads.minOf { it.x }
        val y0 = quads.minOf { it.yTop }
        val x1 = quads.maxOf { it.x + it.w }
        val y1 = quads.maxOf { it.yTop + it.h }
        return Quad(x0, y0, x1 - x0, y1 - y0)
    }

    internal fun groupWords(glyphs: List<GlyphVec>): List<Quad> {
        val quads = mutableListOf<Quad>()
        var x0 = 0f
        var yTop = 0f
        var curWidth = 0f
        var curHeight = 0f
        var started = false
        glyphs.forEach { g ->
            val gap = if (started) g.x - (x0 + curWidth) else 0f
            val newLine = started && abs(g.yBaseline - (yTop + curHeight)) > g.fontSize * 0.5f
            if (started && (gap > g.fontSize * 0.45f || newLine)) {
                if (curWidth > 0 && curHeight > 0) quads.add(Quad(x0, yTop, curWidth, curHeight))
                started = false
            }
            if (!started) {
                x0 = g.x
                yTop = g.yBaseline - g.h
                curWidth = 0f
                curHeight = 0f
                started = true
            }
            curWidth = (g.x + g.w) - x0
            yTop = minOf(yTop, g.yBaseline - g.h)
            curHeight = maxOf(curHeight, g.h)
        }
        if (started && curWidth > 0 && curHeight > 0) quads.add(Quad(x0, yTop, curWidth, curHeight))
        return quads
    }

    internal fun isWordBreak(prev: GlyphVec, next: GlyphVec): Boolean =
        next.x - (prev.x + prev.w) > prev.fontSize * 0.45f ||
            abs(next.yBaseline - prev.yBaseline) > prev.fontSize * 0.5f

    private class WordTextCollector : PDFTextStripper() {
        val words = mutableListOf<WordEntry>()
        private val series = mutableListOf<GlyphVec>()
        private val chars = StringBuilder()

        @Throws(IOException::class)
        override fun writeString(text: String, textPositions: List<TextPosition>) {
            textPositions.forEach { p ->
                val g = GlyphVec(
                    p.getXDirAdj(), p.getYDirAdj(), p.getWidthDirAdj(), p.getHeightDir(), p.getFontSize()
                )
                val prev = series.lastOrNull()
                if (prev != null && isWordBreak(prev, g)) flushWord()
                series.add(g)
                chars.append(p.unicode)
            }
        }

        override fun endPage(page: PDPage) {
            flushWord()
            super.endPage(page)
        }

        private fun flushWord() {
            if (series.isEmpty()) return
            val quads = groupWords(series)
            val text = chars.toString().trim()
            if (quads.isNotEmpty() && text.isNotBlank()) words += WordEntry(union(quads), text)
            series.clear()
            chars.clear()
        }
    }

    internal data class WordEntry(val quad: Quad, val text: String)

    internal fun toDisplay(
        x: Float, yTop: Float, w: Float, h: Float,
        wU: Float, hU: Float, rot: Int
    ): WordRect {
        return when (PdfAnnotate.rotIndex(rot)) {
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
