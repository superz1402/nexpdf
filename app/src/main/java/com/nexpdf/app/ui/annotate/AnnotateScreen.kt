package com.nexpdf.app.ui.annotate

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nexpdf.app.data.PdfAnnotate
import com.nexpdf.app.data.PdfText
import com.nexpdf.app.data.SignatureStore
import com.nexpdf.app.data.WordRect
import com.nexpdf.app.ui.common.Skeleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

private enum class Tool { INK, NOTE, SIGN, ERASE, HIGHLIGHT }

private val PEN_WIDTHS = listOf(
    0.003f to 9.dp,
    0.005f to 15.dp,
    0.009f to 22.dp
)

private data class InkStroke(val points: List<Pair<Float, Float>>, val colorInt: Int, val widthNorm: Float)

private data class NoteItem(val nx: Float, val ny: Float, val text: String)

private data class SigPlacement(val nx: Float, val ny: Float, val w: Float = 0.28f)

@Composable
private fun SignaturePreview(
    strokes: List<List<Pair<Float, Float>>>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        strokes.forEach { pts ->
            if (pts.size >= 2) {
                val p = Path()
                pts.forEachIndexed { i, (x, y) ->
                    val px = x * size.width
                    val py = y * size.height
                    if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
                }
                drawPath(
                    p,
                    androidx.compose.ui.graphics.Color(0xFF102A43),
                    style = Stroke(width = 4f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnotateScreen(uri: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var busy by remember { mutableStateOf(false) }

    var page by remember { mutableStateOf(0) }
    var pageCount by remember { mutableStateOf(0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var bitmapKey by remember { mutableStateOf(0) }
    var aspect by remember { mutableStateOf(0.75f) }
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }

    var tool by remember { mutableStateOf(Tool.INK) }
    var inkColorInt by remember { mutableStateOf(android.graphics.Color.rgb(211, 47, 47)) }
    var penWidth by remember { mutableStateOf(0.005f) }

    var strokes by remember { mutableStateOf<Map<Int, List<InkStroke>>>(emptyMap()) }
    var notes by remember { mutableStateOf<Map<Int, List<NoteItem>>>(emptyMap()) }
    var sigStrokes by remember { mutableStateOf<List<List<Pair<Float, Float>>>?>(null) }
    var savedSigs by remember { mutableStateOf<List<SignatureStore.Signature>>(emptyList()) }
    var sigPlacements by remember { mutableStateOf<List<SigPlacement>>(emptyList()) }
    var highlights by remember { mutableStateOf<Map<Int, List<WordRect>>>(emptyMap()) }
    var selectRect by remember { mutableStateOf<WordRect?>(null) }
    var extracting by remember { mutableStateOf(false) }
    var dragStart by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var draft by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }

    var noteDialogAt by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var noteText by remember { mutableStateOf("") }
    var sigDialogOpen by remember { mutableStateOf(false) }
    var sigDraft by remember { mutableStateOf<List<List<Pair<Float, Float>>>>(emptyList()) }
    var formDialogOpen by remember { mutableStateOf(false) }
    var formFields by remember { mutableStateOf<List<PdfAnnotate.FormFieldInfo>>(emptyList()) }
    var formValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    BackHandler { onBack() }

    fun renderPage(target: Int, force: Boolean = false) {
        scope.launch(Dispatchers.IO) {
            val bmp = runCatching {
                context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")!!.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        pageCount = renderer.pageCount
                        val pageRef = renderer.openPage(target)
                        try {
                            val w = 720
                            val h = (w.toFloat() * pageRef.height / pageRef.width).toInt()
                            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            Canvas(b).drawColor(Color.WHITE)
                            pageRef.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            b
                        } finally {
                            pageRef.close()
                        }
                    }
                }
            }.getOrNull()
            if (bmp != null) {
                aspect = bmp.height.toFloat() / bmp.width
                bitmap = bmp
            }
        }
    }

    LaunchedEffect(uri, page, bitmapKey) { renderPage(page) }

    fun normFrom(offset: Offset): Pair<Float, Float> =
        offset.x / max(overlaySize.width, 1) to offset.y / max(overlaySize.height, 1)

    fun commitDraft() {
        if (draft.size >= 2) {
            val existing = strokes[page] ?: emptyList()
            strokes = strokes + (page to existing + InkStroke(draft, inkColorInt, penWidth))
        }
        draft = emptyList()
    }

    fun eraseAt(pos: Offset) {
        if (overlaySize.width <= 0 || overlaySize.height <= 0) return
        val cur = strokes[page] ?: return
        val w = overlaySize.width
        val h = overlaySize.height
        val t = (min(w, h) * 0.04f).coerceAtLeast(16f)
        val t2 = t * t
        var clear = false
        val kept = cur.filterNot { stroke ->
            val hit = stroke.points.any { (dx, dy) ->
                val ex = dx * w - pos.x
                val ey = dy * h - pos.y
                ex * ex + ey * ey <= t2
            }
            if (hit) clear = true
            hit
        }
        if (clear) strokes = strokes + (page to kept)
        val curRects = highlights[page]
        if (curRects != null && curRects.isNotEmpty()) {
            val keptRects = curRects.filterNot { r ->
                pos.x >= r.dx * w - t && pos.x <= (r.dx + r.dw) * w + t &&
                    pos.y >= r.dy * h - t && pos.y <= (r.dy + r.dh) * h + t
            }
            if (keptRects.size != curRects.size) highlights = highlights + (page to keptRects)
        }
    }

    fun commitHighlight(rawRect: WordRect) {
        val page0 = page
        if (rawRect.dw < 0.01f || rawRect.dh < 0.01f) return
        extracting = true
        scope.launch(Dispatchers.IO) {
            val hits = runCatching {
                PdfText.wordRects(context, Uri.parse(uri), page0)
            }.getOrDefault(emptyList())
                .filter { r ->
                    r.dx < rawRect.dx + rawRect.dw && rawRect.dx < r.dx + r.dw &&
                        r.dy < rawRect.dy + rawRect.dh && rawRect.dy < r.dy + r.dh
                }
            extracting = false
            if (hits.isEmpty()) {
                snackbar.showSnackbar("No text found under the selection on this page")
            } else {
                highlights = highlights + (page0 to (highlights[page0] ?: emptyList()) + hits)
            }
        }
    }

    fun saveAll() {
        if (busy) return
        val ops = mutableListOf<PdfAnnotate.Op>()
        strokes.forEach { (p, list) ->
            val grouped = list.groupBy { it.colorInt to it.widthNorm }
            grouped.forEach { (key, list2) ->
                ops += PdfAnnotate.Op.Ink(
                    page = p,
                    strokes = list2.map { it.points },
                    colorInt = key.first,
                    widthNorm = key.second
                )
            }
        }
        notes.forEach { (p, list) ->
            list.forEach { n -> ops += PdfAnnotate.Op.Note(p, n.nx, n.ny, n.text) }
        }
        highlights.forEach { (p, list) ->
            list.forEach { r ->
                ops += PdfAnnotate.Op.Highlight(p, r.dx, r.dy, r.dw, r.dh, 0xFFFBC02D.toInt())
            }
        }
        sigPlacements.forEach { s ->
            sigStrokes?.let {
                ops += PdfAnnotate.Op.Signature(page, s.nx, s.ny, s.w, s.w * 0.6f, it, inkColorInt)
            }
        }
        if (ops.isEmpty() && formValues.isEmpty()) {
            scope.launch { snackbar.showSnackbar("Nothing to save yet") }
            return
        }
        busy = true
        scope.launch(Dispatchers.IO) {
            val msg = PdfAnnotate.applyAll(
                context, Uri.parse(uri), ops,
                formValues.map { (name, value) -> PdfAnnotate.FormValue(name, value) }
            ).fold(
                onSuccess = { "Saved copy in Downloads" },
                onFailure = { it.message ?: "Save failed" }
            )
            busy = false
            snackbar.showSnackbar(msg)
            if (msg.startsWith("Saved")) {
                strokes = emptyMap()
                notes = emptyMap()
                sigPlacements = emptyList()
                highlights = emptyMap()
                bitmapKey++
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Annotate", maxLines = 1)
                        Text(
                            "Page ${page + 1} of ${max(pageCount, 1)}",
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
                Column(Modifier.fillMaxWidth().padding(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { if (page > 0) page-- }, enabled = page > 0) { Text("Prev") }
                        TextButton(
                            onClick = { if (pageCount > 0 && page < pageCount - 1) page++ },
                            enabled = page < pageCount - 1
                        ) { Text("Next") }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Tool.INK to Icons.Default.Edit,
                            Tool.NOTE to Icons.Default.StickyNote2,
                            Tool.SIGN to Icons.Default.Draw,
                            Tool.ERASE to Icons.Default.CleaningServices,
                            Tool.HIGHLIGHT to Icons.Default.Highlight
                        ).forEach { (t, icon) ->
                            IconButton(
                                onClick = {
                                    tool = t
                                    if (t == Tool.SIGN) sigDialogOpen = true
                                },
                                modifier = Modifier.then(
                                    if (tool == t) Modifier.background(
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        CircleShape
                                    ) else Modifier
                                )
                            ) {
                                Icon(icon, contentDescription = t.name)
                            }
                        }
                        listOf(
                            0xFFD32F2F.toInt(),
                            0xFF1976D2.toInt(),
                            0xFF212121.toInt()
                        ).forEach { c ->
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(androidx.compose.ui.graphics.Color(c))
                                    .border(
                                        2.dp,
                                        if (inkColorInt == c) MaterialTheme.colorScheme.outline else androidx.compose.ui.graphics.Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { inkColorInt = c }
                            )
                        }
                        if (tool == Tool.INK) {
                            PEN_WIDTHS.forEach { (wn, dot) ->
                                Box(
                                    Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .clickable { penWidth = wn },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        Modifier
                                            .size(dot)
                                            .clip(CircleShape)
                                            .background(
                                                if (penWidth == wn) MaterialTheme.colorScheme.onSurface
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                    )
                                }
                            }
                        }
                        Box(Modifier.weight(1f))
                        IconButton(onClick = { formDialogOpen = true }, enabled = !busy) {
                            Icon(Icons.Default.Edit, contentDescription = "Form fields")
                        }
                        IconButton(onClick = ::saveAll, enabled = !busy) {
                            Icon(Icons.Default.Save, contentDescription = "Save copy to Downloads")
                        }
                    }
                }
            }
        }
    ) { inner ->
        val bmp = bitmap
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .aspectRatio(max(0.4f, 1f / max(aspect, 0.1f)))
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (bmp == null) {
                    Skeleton(Modifier.fillMaxSize())
                } else {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxSize()
                    )
                    Canvas(
                        Modifier
                            .fillMaxSize()
                            .onSizeChanged { overlaySize = it }
                            .pointerInput(tool, page) {
                                when (tool) {
                                    Tool.INK -> detectDragGestures(
                                        onDragStart = { pos -> draft = listOf(normFrom(pos)) },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            draft = draft + normFrom(change.position)
                                        },
                                        onDragEnd = { commitDraft() }
                                    )
                                    Tool.ERASE -> detectDragGestures(
                                        onDragStart = { eraseAt(it) },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            eraseAt(change.position)
                                        },
                                        onDragEnd = {}
                                    )
                                    Tool.HIGHLIGHT -> detectDragGestures(
                                        onDragStart = { pos ->
                                            val (nx, ny) = normFrom(pos)
                                            dragStart = nx to ny
                                            selectRect = WordRect(nx, ny, 0f, 0f)
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            val (sx, sy) = dragStart ?: return@detectDragGestures
                                            val (cx, cy) = normFrom(change.position)
                                            selectRect = WordRect(
                                                min(sx, cx), min(sy, cy),
                                                Math.abs(cx - sx), Math.abs(cy - sy)
                                            )
                                        },
                                        onDragEnd = {
                                            selectRect?.let { commitHighlight(it) }
                                            selectRect = null
                                            dragStart = null
                                        }
                                    )
                                    else -> detectTapGestures { pos ->
                                        val (nx, ny) = normFrom(pos)
                                        if (tool == Tool.NOTE) {
                                            noteDialogAt = nx to ny
                                        } else if (tool == Tool.SIGN) {
                                            sigStrokes?.let {
                                                sigPlacements = sigPlacements + SigPlacement(nx, ny)
                                            } ?: run { sigDialogOpen = true }
                                        }
                                    }
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height
                        val strokePx = (w * 0.005f).coerceAtLeast(3f)
                        fun draw(points: List<Pair<Float, Float>>, color: Int, px: Float = strokePx) {
                            if (points.size < 2) return
                            val path = Path()
                            points.forEachIndexed { i, (dx, dy) ->
                                val x = dx * w
                                val y = dy * h
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(
                                path,
                                color = androidx.compose.ui.graphics.Color(color).copy(alpha = 0.8f),
                                style = Stroke(width = px.coerceAtLeast(3f))
                            )
                        }
                        (strokes[page] ?: emptyList()).forEach {
                            draw(it.points, it.colorInt, w * it.widthNorm)
                        }
                        draw(draft, inkColorInt, w * penWidth)
                        (highlights[page] ?: emptyList()).forEach { r ->
                            drawRect(
                                androidx.compose.ui.graphics.Color(0xFFFBC02D).copy(alpha = 0.35f),
                                topLeft = Offset(r.dx * w, r.dy * h),
                                size = Size(r.dw * w, r.dh * h)
                            )
                        }
                        selectRect?.let { r ->
                            drawRect(
                                androidx.compose.ui.graphics.Color(0xFFFBC02D).copy(alpha = 0.18f),
                                topLeft = Offset(r.dx * w, r.dy * h),
                                size = Size((r.dw * w).coerceAtLeast(2f), (r.dh * h).coerceAtLeast(2f))
                            )
                            drawRect(
                                androidx.compose.ui.graphics.Color(0xFFFBC02D),
                                topLeft = Offset(r.dx * w, r.dy * h),
                                size = Size((r.dw * w).coerceAtLeast(2f), (r.dh * h).coerceAtLeast(2f)),
                                style = Stroke(width = 2f)
                            )
                        }
                        (notes[page] ?: emptyList()).forEach { n ->
                            drawCircle(
                                androidx.compose.ui.graphics.Color(0xFFFDD835),
                                radius = strokePx * 0.9f,
                                center = Offset(n.nx * w, n.ny * h)
                            )
                        }
                        sigPlacements.forEach { p ->
                            sigStrokes?.forEach { stroke ->
                                val sub = stroke.map { (sx, sy) ->
                                    (p.nx + sx * p.w) to (p.ny + sy * (p.w * 0.6f))
                                }
                                draw(sub, inkColorInt)
                            }
                        }
                    }
                }
            }
        }
    }

    if (noteDialogAt != null) {
        val (nx, ny) = noteDialogAt!!
        AlertDialog(
            onDismissRequest = { noteDialogAt = null },
            title = { Text("Add note") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("Type your note") },
                    minLines = 2
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (noteText.isNotBlank()) {
                        val existing = notes[page] ?: emptyList()
                        notes = notes + (page to existing + NoteItem(nx, ny, noteText.trim()))
                        noteText = ""
                    }
                    noteDialogAt = null
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { noteDialogAt = null }) { Text("Cancel") } }
        )
    }

    if (sigDialogOpen) {
        LaunchedEffect(Unit) {
            savedSigs = SignatureStore.list(context)
        }
        Dialog(onDismissRequest = { sigDialogOpen = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (savedSigs.isNotEmpty()) {
                        Text("Saved", style = MaterialTheme.typography.labelMedium)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            savedSigs.forEach { sig ->
                                Box(
                                    Modifier
                                        .size(110.dp, 56.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(androidx.compose.ui.graphics.Color.White)
                                        .clickable {
                                            sigStrokes = sig.strokes
                                            sigDialogOpen = false
                                        }
                                ) {
                                    SignaturePreview(
                                        strokes = sig.strokes,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .size(20.dp)
                                            .clickable {
                                                scope.launch { SignatureStore.delete(context, sig.id) }
                                                savedSigs = savedSigs.filterNot { it.id == sig.id }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Delete saved signature",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            "Draw a new signature",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    } else {
                        Text("Draw your signature", style = MaterialTheme.typography.titleMedium)
                    }
                    var sizeSig by remember { mutableStateOf(IntSize(1, 1)) }
                    Canvas(
                        Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(androidx.compose.ui.graphics.Color.White)
                            .onSizeChanged { sizeSig = it }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { pos ->
                                        sigDraft = sigDraft + listOf(
                                            listOf(
                                                pos.x / max(sizeSig.width, 1) to
                                                    pos.y / max(sizeSig.height, 1)
                                            )
                                        )
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val last = sigDraft.lastOrNull() ?: return@detectDragGestures
                                        sigDraft = sigDraft.dropLast(1) + listOf(
                                            last + listOf(
                                                change.position.x / max(sizeSig.width, 1) to
                                                    change.position.y / max(sizeSig.height, 1)
                                            )
                                        )
                                    }
                                )
                            }
                    ) {
                        sigDraft.forEach { pts ->
                            if (pts.size >= 2) {
                                val p = Path()
                                pts.forEachIndexed { i, (x, y) ->
                                    val px = x * size.width
                                    val py = y * size.height
                                    if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
                                }
                                drawPath(
                                    p,
                                    androidx.compose.ui.graphics.Color(0xFF102A43),
                                    style = Stroke(width = 5f)
                                )
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { sigDraft = emptyList() }) { Text("Clear") }
                        TextButton(
                            onClick = {
                                sigStrokes = sigDraft
                                sigDialogOpen = false
                            },
                            enabled = sigDraft.isNotEmpty()
                        ) { Text("Use signature") }
                        TextButton(
                            onClick = {
                                scope.launch {
                                    val saved = SignatureStore.save(context, sigDraft)
                                    savedSigs = savedSigs + saved
                                }
                                sigStrokes = sigDraft
                                sigDialogOpen = false
                            },
                            enabled = sigDraft.isNotEmpty()
                        ) { Text("Save & use") }
                    }
                }
            }
        }
    }

    if (formDialogOpen) {
        Dialog(onDismissRequest = { formDialogOpen = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Form fields", style = MaterialTheme.typography.titleMedium)
                    LaunchedEffect(Unit) {
                        formFields = withContext(Dispatchers.IO) {
                            PdfAnnotate.listFields(context, Uri.parse(uri))
                        }
                    }
                    if (formFields.isEmpty()) {
                        Text(
                            "This document has no fillable form fields.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(formFields, key = { it.name }) { f ->
                                if (f.isText && !f.readOnly) {
                                    Column {
                                        Text(f.name, style = MaterialTheme.typography.labelSmall)
                                        OutlinedTextField(
                                            value = formValues[f.name] ?: f.value,
                                            onValueChange = { formValues = formValues + (f.name to it) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                } else {
                                    Text(
                                        "${f.name} = ${f.value}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { formDialogOpen = false }) { Text("Close") }
                        TextButton(
                            onClick = {
                                formDialogOpen = false
                                if (formValues.isNotEmpty()) {
                                    busy = true
                                    scope.launch(Dispatchers.IO) {
                                        val msg = PdfAnnotate.applyAll(
                                            context, Uri.parse(uri),
                                            emptyList(),
                                            formValues.map { PdfAnnotate.FormValue(it.key, it.value) },
                                            flattenForm = true
                                        ).fold(
                                            onSuccess = { "Form filled and flattened — copy in Downloads" },
                                            onFailure = { it.message ?: "Save failed" }
                                        )
                                        busy = false
                                        snackbar.showSnackbar(msg)
                                        bitmapKey++
                                    }
                                }
                            },
                            enabled = formFields.isNotEmpty()
                        ) { Text("Apply & flatten") }
                    }
                }
            }
        }
    }
}

