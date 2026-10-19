package com.nexpdf.app.ui.organize

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexpdf.app.data.PdfTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class PageCell(val index: Int, val rotation: Int = 0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizerScreen(uri: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var busy by remember { mutableStateOf(false) }

    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var originalCount by remember { mutableStateOf(0) }
    var pages by remember { mutableStateOf<List<PageCell>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var thumbnails by remember { mutableStateOf<Map<Int, Bitmap>>(emptyMap()) }

    BackHandler { onBack() }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")!!.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        originalCount = renderer.pageCount
                        sourceUri = Uri.parse(uri)
                        pages = (0 until renderer.pageCount).map { PageCell(it) }
                    }
                }
            }.onFailure { snackbar.showSnackbar(it.message ?: "Could not open document") }
        }
    }

    fun toggle(index: Int) {
        selected = if (index in selected) selected - index else selected + index
    }

    fun rotateSelection() {
        val targets = if (selected.isEmpty()) pages.map { it.index }.toSet() else selected
        pages = pages.map { cell ->
            if (cell.index in targets) cell.copy(rotation = (cell.rotation + 90) % 360) else cell
        }
    }

    fun deleteSelection() {
        if (selected.size >= pages.size) return
        pages = pages.filter { it.index !in selected }
        selected = emptySet()
    }

    fun moveSelection() {
        val anchor = selected.firstOrNull() ?: return
        val cellIndex = pages.indexOfFirst { it.index == anchor }
        if (cellIndex <= 0) return
        val list = pages.toMutableList()
        val cell = list.removeAt(cellIndex)
        list.add(cellIndex - 1, cell)
        pages = list
    }

    fun saveCopy() {
        val source = sourceUri ?: return
        if (busy || pages.isEmpty()) return
        busy = true
        scope.launch(Dispatchers.IO) {
            val message = runCatching {
                var work = source

                if (pages.size < originalCount) {
                    val kept = pages.map { it.index }.toSet()
                    val removed = (0 until originalCount).filter { it !in kept }.toSet()
                    work = PdfTools.deletePages(context, work, removed, originalCount).getOrThrow()
                }

                val keptAsc = pages.map { it.index }.sorted()
                val workingPos: Map<Int, Int> = keptAsc
                    .withIndex().associate { (pos, original) -> original to pos }

                val rotations = pages.filter { it.rotation != 0 }
                if (rotations.isNotEmpty()) {
                    rotations.groupBy { it.rotation }.forEach { (degree, cells) ->
                        val positions = cells.mapNotNull { workingPos[it.index] }
                        work = PdfTools.rotate(context, work, positions, degree).getOrThrow()
                    }
                }

                val natural = (0 until pages.size).toList()
                if (workingPos.size == pages.size) {
                    val desired: List<Int>? = pages.map { cell -> workingPos[cell.index] }
                    if (desired != null && desired != natural) {
                        work = PdfTools.reorder(context, work, desired).getOrThrow()
                    }
                }
                "Saved copy in Downloads"
            }.fold(
                onSuccess = { it },
                onFailure = { it.message ?: "Save failed" }
            )
            busy = false
            snackbar.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Organize pages", maxLines = 1)
                        Text(
                            if (selected.isEmpty()) "${pages.size} pages"
                            else "${selected.size} of ${pages.size} selected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilledTonalIconButton(
                        onClick = ::rotateSelection,
                        enabled = !busy && pages.isNotEmpty()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = "Rotate selection, or all pages"
                        )
                    }
                    FilledTonalIconButton(
                        onClick = ::deleteSelection,
                        enabled = !busy && selected.isNotEmpty() && pages.size > selected.size
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete selected pages")
                    }
                    FilledTonalIconButton(
                        onClick = ::moveSelection,
                        enabled = !busy && selected.size == 1
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move selected page up")
                    }
                    FilledTonalIconButton(onClick = ::saveCopy, enabled = !busy && pages.isNotEmpty()) {
                        Icon(Icons.Default.Save, contentDescription = "Save copy to Downloads")
                    }
                }
            }
        }
    ) { inner ->
        if (sourceUri == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading pages…", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(inner)
            ) {
                itemsIndexed(pages, key = { _, cell -> cell.index }) { _, cell ->
                    val d = sourceUri
                    if (d != null) {
                        OrganizerCell(
                            cell = cell,
                            sourceUri = d,
                            thumbnails = thumbnails,
                            onThumb = { index, bmp -> thumbnails = thumbnails + (index to bmp) },
                            selected = cell.index in selected,
                            onClick = { toggle(cell.index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrganizerCell(
    cell: PageCell,
    sourceUri: Uri,
    thumbnails: Map<Int, Bitmap>,
    onThumb: (Int, Bitmap) -> Unit,
    selected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(cell.index, sourceUri) {
        if (!thumbnails.containsKey(cell.index)) {
            withContext(Dispatchers.IO) {
                renderThumbnail(context, sourceUri, cell.index) { onThumb(cell.index, it) }
            }
        }
    }
    val bmp = thumbnails[cell.index]
    val rotated = cell.rotation % 180 != 0
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = Modifier.clickable(onClick = onClick)
        ) {
            Box(Modifier.fillMaxWidth()) {
                if (bmp == null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.75f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    )
                } else {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Page ${cell.index + 1}" +
                            if (selected) ", selected" else "",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (rotated) Modifier
                                    .aspectRatio(0.75f)
                                    .graphicsLayer {
                                        rotationZ = cell.rotation.toFloat()
                                    }
                                else Modifier
                            )
                    )
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text((cell.index + 1).toString())
                    }
                }
            }
        }
    }
}

private fun renderThumbnail(
    context: Context,
    uri: Uri,
    pageIndex: Int,
    onBitmap: (Bitmap) -> Unit
) {
    runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")!!.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                val page = renderer.openPage(pageIndex)
                try {
                    val w = 480
                    val h = (w.toFloat() * page.height / page.width).toInt()
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    Canvas(bmp).drawColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    onBitmap(bmp)
                } finally {
                    page.close()
                }
            }
        }
    }
}
