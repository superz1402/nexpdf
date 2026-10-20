package com.nexpdf.app.data

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDField

object PdfAnnotate {

    sealed interface Op {
        val page: Int
        data class Ink(
            override val page: Int,
            val strokes: List<List<Pair<Float, Float>>>,
            val colorInt: Int,
            val widthNorm: Float
        ) : Op
        data class Note(
            override val page: Int,
            val nx: Float,
            val ny: Float,
            val text: String
        ) : Op
        data class Signature(
            override val page: Int,
            val rectNx: Float,
            val rectNy: Float,
            val rectNw: Float,
            val rectNh: Float,
            val strokes: List<List<Pair<Float, Float>>>,
            val colorInt: Int
        ) : Op
        data class Highlight(
            override val page: Int,
            val rectNx: Float,
            val rectNy: Float,
            val rectNw: Float,
            val rectNh: Float,
            val colorInt: Int
        ) : Op
    }

    data class FormValue(val name: String, val value: String)

    data class FormFieldInfo(val name: String, val value: String, val isText: Boolean, val readOnly: Boolean)

    private fun load(context: Context, uri: Uri): PDDocument {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("Cannot open $uri")
        pfd.use {
            return PDDocument.load(ParcelFileDescriptor.AutoCloseInputStream(pfd))
        }
    }

    private fun pdColor(colorInt: Int, alpha: Float = 1f): PDColor {
        val r = (colorInt shr 16 and 0xFF) / 255f
        val g = (colorInt shr 8 and 0xFF) / 255f
        val b = (colorInt and 0xFF) / 255f
        return PDColor(floatArrayOf(r, g, b), PDDeviceRGB.INSTANCE)
    }

    internal fun pdfPoint(
        dx: Float, dy: Float,
        rect: PDRectangle, rotation: Int
    ): Pair<Float, Float> {
        val w = rect.width
        val h = rect.height
        val llx = rect.lowerLeftX
        val lly = rect.lowerLeftY
        return when (rotation) {
            90 -> llx + (1f - dy) * h to lly + dx * w
            180 -> llx + (1f - dx) * w to lly + dy * h
            270 -> llx + dy * h to lly + (1f - dx) * w
            else -> llx + dx * w to lly + (1f - dy) * h
        }
    }

    internal fun userToDisplay(
        ux: Float, uy: Float,
        rect: PDRectangle, rotation: Int
    ): Pair<Float, Float> {
        val x = ux - rect.lowerLeftX
        val y = uy - rect.lowerLeftY
        val w = rect.width
        val h = rect.height
        return when (rotIndex(rotation)) {
            1 -> y / w to 1f - x / h
            2 -> 1f - x / w to y / h
            3 -> 1f - y / w to x / h
            else -> x / w to 1f - y / h
        }
    }

    internal fun rotIndex(rotation: Int): Int =
        (((rotation % 360) + 360) % 360) / 90

    internal fun strokeWidth(norm: Float, rect: PDRectangle): Float = (norm * rect.width).coerceIn(0.8f, 24f)

    private fun strokePath(
        cs: PDPageContentStream,
        stroke: List<Pair<Float, Float>>,
        rect: PDRectangle,
        rotation: Int,
        colorInt: Int,
        width: Float
    ) {
        val pts = stroke.map { (dx, dy) -> pdfPoint(dx, dy, rect, rotation) }
        if (pts.size < 2) return
        cs.setLineWidth(width)
        cs.setStrokingColor(pdColor(colorInt))
        val (sx, sy) = pts.first()
        cs.moveTo(sx, sy)
        pts.drop(1).forEach { (px, py) -> cs.lineTo(px, py) }
        cs.stroke()
    }

    fun applyAll(
        context: Context,
        uri: Uri,
        ops: List<Op>,
        formValues: List<FormValue> = emptyList(),
        flattenForm: Boolean = false
    ): Result<Uri> {
        if (ops.isEmpty() && formValues.isEmpty()) {
            return Result.failure(IllegalArgumentException("No edits pending"))
        }
        return runCatching {
            val doc = load(context, uri)
            try {
                ops.forEach { op ->
                    val page = doc.getPage(op.page)
                    val rect: PDRectangle = page.mediaBox
                    val rot = page.rotation
                    val cs = PDPageContentStream(
                        doc, page,
                        PDPageContentStream.AppendMode.APPEND, true, true
                    )
                    try {
                        when (op) {
                            is Op.Ink -> {
                                val w = strokeWidth(op.widthNorm, rect)
                                op.strokes.forEach { stroke ->
                                    strokePath(cs, stroke, rect, rot, op.colorInt, w)
                                }
                            }
                            is Op.Note -> {
                                val fontSize = 10f
                                val (x, yStart) = pdfPoint(op.nx, op.ny, rect, rot)
                                val lines = op.text.split("\n")
                                cs.beginText()
                                cs.setFont(PDType1Font.HELVETICA, fontSize)
                                cs.setNonStrokingColor(pdColor(Color.rgb(50, 50, 50)))
                                var y = yStart
                                lines.forEach { line ->
                                    cs.setTextMatrix(com.tom_roush.pdfbox.util.Matrix.getTranslateInstance(x, y))
                                    cs.showText(line)
                                    y -= fontSize * 1.3f
                                }
                                cs.endText()
                            }
                            is Op.Signature -> {
                                drawSignature(cs, op, rect, rot, doc)
                            }
                            is Op.Highlight -> {
                                val gs = PDExtendedGraphicsState()
                                gs.setNonStrokingAlphaConstant(0.35f)
                                cs.setGraphicsStateParameters(gs)
                                cs.setNonStrokingColor(pdColor(op.colorInt))
                                val (ax, ay) = pdfPoint(op.rectNx, op.rectNy, rect, rot)
                                val (bx, by) = pdfPoint(op.rectNx + op.rectNw, op.rectNy + op.rectNh, rect, rot)
                                cs.addRect(
                                    minOf(ax, bx), minOf(ay, by),
                                    Math.abs(bx - ax), Math.abs(by - ay)
                                )
                                cs.fill()
                            }
                        }
                    } finally {
                        cs.close()
                    }
                }
                applyForm(doc, formValues, flattenForm)
                PdfTools.saveTo(context, doc, "NexPDF_annotated_${stampName()}.pdf")
            } finally {
                try { doc.close() } catch (_: Exception) {}
            }
        }
    }

    private fun drawSignature(
        cs: PDPageContentStream,
        op: Op.Signature,
        rect: PDRectangle,
        rotation: Int,
        doc: PDDocument
    ) {
        val (x0, y0) = pdfPoint(op.rectNx, op.rectNy, rect, rotation)
        val (x1, y1) = pdfPoint(op.rectNx + op.rectNw, op.rectNy + op.rectNh, rect, rotation)
        val left = minOf(x0, x1) + 6f
        val bottom = minOf(y0, y1) + 6f
        val right = maxOf(x0, x1) - 6f
        val top = maxOf(y0, y1) - 6f
        val w = right - left
        val h = top - bottom
        if (w <= 0 || h <= 0) return
        op.strokes.forEach { stroke ->
            val pts = stroke.map { (sx, sy) -> left + sx * w to bottom + sy * h }
            if (pts.size >= 2) {
                cs.setLineWidth(2.2f)
                cs.setStrokingColor(pdColor(op.colorInt))
                val (ax, ay) = pts.first()
                cs.moveTo(ax, ay)
                pts.drop(1).forEach { (bx, by) -> cs.lineTo(bx, by) }
                cs.stroke()
            }
        }
    }

    private fun applyForm(
        doc: PDDocument,
        values: List<FormValue>,
        flatten: Boolean
    ) {
        val acroForm = doc.documentCatalog.acroForm ?: return
        values.forEach { v ->
            val field = acroForm.getField(v.name) ?: return@forEach
            if (field is com.tom_roush.pdfbox.pdmodel.interactive.form.PDTextField && !field.isReadOnly) {
                field.value = v.value
            }
        }
        if (flatten) acroForm.flatten()
    }

    fun listFields(context: Context, uri: Uri): List<FormFieldInfo> {
        val doc = load(context, uri)
        try {
            val acro = doc.documentCatalog.acroForm ?: return emptyList()
            return acro.fields.mapNotNull { field: PDField ->
                val name = field.fullyQualifiedName ?: return@mapNotNull null
                val isText = field is com.tom_roush.pdfbox.pdmodel.interactive.form.PDTextField
                val value = field.valueAsString ?: ""
                FormFieldInfo(name, value, isText, field.isReadOnly)
            }
        } finally {
            try { doc.close() } catch (_: Exception) {}
        }
    }

    private fun stampName(): String =
        java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
            .format(java.util.Date())
}
