package com.nexpdf.app.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfTools {

    private fun stamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    private fun load(context: Context, uri: Uri): PDDocument {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("Cannot open $uri")
        pfd.use {
            return PDDocument.load(ParcelFileDescriptor.AutoCloseInputStream(pfd))
        }
    }

    private class Output(
        context: Context,
        private val baseName: String
    ) : Closeable {
        private val resolver = context.contentResolver
        private val mediaStore = Build.VERSION.SDK_INT >= 29
        private var storedUri: Uri? = null
        private var file: File? = null

        val stream: OutputStream =
            if (mediaStore) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, baseName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val collection = MediaStore.Downloads.getContentUri("external")
                storedUri = resolver.insert(collection, values)
                    ?: throw IllegalStateException("MediaStore insert failed")
                resolver.openOutputStream(storedUri!!)
                    ?: throw IllegalStateException("Cannot open output")
            } else {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "NexPDF")
                dir.mkdirs()
                File(dir, baseName).also { file = it }.outputStream()
            }

        fun finish(): Uri {
            if (mediaStore) {
                val uri = storedUri!!
                resolver.update(uri, ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }, null, null)
                return uri
            }
            return Uri.fromFile(file!!)
        }

        override fun close() {
            try { stream.close() } catch (_: Exception) {}
        }
    }

    private fun saveTo(context: Context, doc: PDDocument, baseName: String): Uri {
        val output = Output(context, baseName)
        try {
            doc.save(output.stream)
            return output.finish()
        } finally {
            output.close()
        }
    }

    fun merge(
        context: Context,
        uris: List<Uri>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<Uri> {
        if (uris.isEmpty()) return Result.failure(IllegalArgumentException("No documents selected"))
        return runCatching {
            val out = PDDocument()
            try {
                val merger = PDFMergerUtility()
                uris.forEachIndexed { index, u ->
                    val src = load(context, u)
                    try {
                        merger.appendDocument(out, src)
                    } finally {
                        try { src.close() } catch (_: Exception) {}
                    }
                    onProgress(index + 1, uris.size)
                }
                saveTo(context, out, "NexPDF_merge_${stamp()}.pdf")
            } finally {
                try { out.close() } catch (_: Exception) {}
            }
        }
    }

    private fun assemblePages(
        context: Context,
        uri: Uri,
        order: List<Int>,
        baseName: String
    ): Result<Uri> = runCatching {
        val doc = load(context, uri)
        val singles = Splitter().split(doc)
        try {
            require(order.isNotEmpty()) { "No pages selected" }
            require(order.all { it in singles.indices }) { "Page out of range" }
            val out = PDDocument()
            try {
                val merger = PDFMergerUtility()
                order.forEach { index ->
                    merger.appendDocument(out, singles[index])
                }
                saveTo(context, out, baseName)
            } finally {
                try { out.close() } catch (_: Exception) {}
            }
        } finally {
            try { doc.close() } catch (_: Exception) {}
            singles.forEach { s -> try { s.close() } catch (_: Exception) {} }
        }
    }

    fun extractPages(
        context: Context,
        uri: Uri,
        pageIndices: List<Int>
    ): Result<Uri> = assemblePages(context, uri, pageIndices, "NexPDF_pages_${stamp()}.pdf")

    fun deletePages(
        context: Context,
        uri: Uri,
        deleteIndices: Set<Int>,
        pageCount: Int
    ): Result<Uri> {
        require(deleteIndices.size < pageCount) { "Cannot delete all pages" }
        val keep = (0 until pageCount).filter { it !in deleteIndices }
        return assemblePages(context, uri, keep, "NexPDF_trimmed_${stamp()}.pdf")
    }

    fun reorder(
        context: Context,
        uri: Uri,
        order: List<Int>
    ): Result<Uri> {
        require(order.isNotEmpty()) { "Empty order" }
        return assemblePages(context, uri, order, "NexPDF_reordered_${stamp()}.pdf")
    }

    fun rotate(
        context: Context,
        uri: Uri,
        pageIndices: List<Int>,
        degrees: Int
    ): Result<Uri> = runCatching {
        require(degrees % 90 == 0) { "Rotation must be a multiple of 90" }
        require(pageIndices.isNotEmpty()) { "No pages selected" }
        val doc = load(context, uri)
        try {
            require(pageIndices.all { it in (0 until doc.pages.size) }) { "Page out of range" }
            val mod = ((degrees % 360) + 360) % 360
            pageIndices.forEach { index ->
                val page = doc.getPage(index)
                page.rotation = (page.rotation + mod) % 360
            }
            saveTo(context, doc, "NexPDF_rotated_${stamp()}.pdf")
        } finally {
            try { doc.close() } catch (_: Exception) {}
        }
    }

    fun split(
        context: Context,
        uri: Uri,
        pagesPerPart: Int,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<List<Uri>> = runCatching {
        val doc = load(context, uri)
        val singles = Splitter().split(doc)
        try {
            require(singles.size >= 2) { "Document must have at least 2 pages" }
            val per = pagesPerPart.coerceIn(1, singles.size)
            val expected = (singles.size + per - 1) / per
            val created = mutableListOf<Uri>()
            var bucket = PDDocument()
            var count = 0
            fun flush() {
                if (count > 0) {
                    created += saveTo(context, bucket, "NexPDF_part${created.size + 1}of${expected}_${stamp()}.pdf")
                    onProgress(created.size, expected)
                }
                try { bucket.close() } catch (_: Exception) {}
                bucket = PDDocument()
                count = 0
            }
            singles.forEach { single ->
                single.use {
                    PDFMergerUtility().appendDocument(bucket, single)
                    count++
                    if (count == per) flush()
                }
            }
            singles.forEach { s -> try { s.close() } catch (_: Exception) {} }
            flush()
            created
        } finally {
            try { doc.close() } catch (_: Exception) {}
        }
    }

    fun imagesToPdf(
        context: Context,
        imageUris: List<Uri>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<Uri> {
        if (imageUris.isEmpty()) return Result.failure(IllegalArgumentException("No images selected"))
        return runCatching {
            val doc = PDDocument()
            try {
                imageUris.forEachIndexed { index, u ->
                    val bmp = decode(context, u) ?: error("Cannot decode image ${index + 1}")
                    try {
                        val bytes = ByteArrayOutputStream().also { bos ->
                            bmp.compress(Bitmap.CompressFormat.JPEG, 92, bos)
                        }.toByteArray()
                        val image = PDImageXObject.createFromByteArray(doc, bytes, "image$index")
                        val rect: PDRectangle = PDRectangle.LETTER
                        val page = PDPage(rect)
                        doc.addPage(page)
                        val margin = 24f
                        val fit = minOf(
                            (rect.width - 2 * margin) / image.width,
                            (rect.height - 2 * margin) / image.height
                        )
                        val w = image.width * fit
                        val h = image.height * fit
                        val x = (rect.width - w) / 2
                        val y = (rect.height - h) / 2
                        val cs = PDPageContentStream(doc, page)
                        try {
                            cs.drawImage(image, x, y, w, h)
                        } finally {
                            cs.close()
                        }
                        onProgress(index + 1, imageUris.size)
                    } finally {
                        bmp.recycle()
                    }
                }
                saveTo(context, doc, "NexPDF_images_${stamp()}.pdf")
            } finally {
                try { doc.close() } catch (_: Exception) {}
            }
        }
    }

    private fun decode(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }
}
