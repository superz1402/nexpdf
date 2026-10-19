package com.nexpdf.app.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

data class PdfDoc(
    val uri: Uri,
    val title: String,
    val sizeBytes: Long,
    val lastModified: Long
)

object PdfLibrary {

    private val COLLECTION: Uri = MediaStore.Files.getContentUri("external")

    fun query(context: Context): List<PdfDoc> {
        val docs = mutableListOf<PdfDoc>()
        runCatching {
            context.contentResolver.query(
                COLLECTION,
                arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.DATE_MODIFIED
                ),
                "${MediaStore.MediaColumns.MIME_TYPE} = ?",
                arrayOf("application/pdf"),
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                while (c.moveToNext()) {
                    val title = c.getString(nameCol) ?: continue
                    docs += PdfDoc(
                        uri = ContentUris.withAppendedId(COLLECTION, c.getLong(idCol)),
                        title = title,
                        sizeBytes = c.getLong(sizeCol),
                        lastModified = c.getLong(dateCol) * 1000L
                    )
                }
            }
        }
        return docs
    }
}
