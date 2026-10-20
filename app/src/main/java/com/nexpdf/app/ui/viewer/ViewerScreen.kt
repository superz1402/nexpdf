package com.nexpdf.app.ui.viewer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nexpdf.app.ui.common.Skeleton
import com.nexpdf.app.data.ReadingPositionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class DocumentHolder(
    val renderer: PdfRenderer,
    private val pfd: ParcelFileDescriptor
) : AutoCloseable {
    val pageCount: Int = renderer.pageCount
    override fun close() {
        try { renderer.close() } catch (_: Exception) {}
        try { pfd.close() } catch (_: Exception) {}
    }
}

private sealed interface ViewerUi {
    data class Loading(val title: String) : ViewerUi
    data class Error(val title: String, val message: String) : ViewerUi
    data class Ready(val title: String, val uri: String, val holder: DocumentHolder) : ViewerUi
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(uri: String, onClose: () -> Unit, onAnnotate: (String) -> Unit = {}) {
    val context = LocalContext.current
    val title = remember(uri) { uri.substringAfterLast('/') }

    var uiState by remember { mutableStateOf<ViewerUi>(ViewerUi.Loading(title)) }
    var holder by remember { mutableStateOf<DocumentHolder?>(null) }

    BackHandler { onClose() }

    DisposableEffect(Unit) {
        onDispose { holder?.close() }
    }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            runCatching {
                val pfd = context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")!!
                DocumentHolder(PdfRenderer(pfd), pfd)
            }.onSuccess {
                holder = it
                uiState = ViewerUi.Ready(title, uri, it)
            }.onFailure {
                uiState = ViewerUi.Error(title, it.message ?: "Could not open document")
            }
        }
    }

    when (val state = uiState) {
        is ViewerUi.Ready -> Pager(state, onAnnotate = { onAnnotate(uri) })
        is ViewerUi.Error -> ErrorPage(state, onClose)
        is ViewerUi.Loading -> LoadingPage(title)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Pager(state: ViewerUi.Ready, onAnnotate: () -> Unit) {
    val context = LocalContext.current
    val state2 = state.holder
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var anyPageZoomed by remember { mutableStateOf(false) }
    var jumpSheetOpen by remember { mutableStateOf(false) }
    var positionRestored by remember { mutableStateOf(false) }

    val currentPage by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val visible = info.visibleItemsInfo
            if (visible.isEmpty()) 0 else {
                val dy = listState.firstVisibleItemScrollOffset.toFloat() /
                    (info.viewportSize.height.coerceAtLeast(1)).toFloat()
                (listState.firstVisibleItemIndex + if (dy > 0.6f && visible.size > 1) 1 else 0)
                    .coerceIn(0, state2.pageCount - 1)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        LaunchedEffect(state2) {
            val saved = ReadingPositionStore.load(context, state.uri, state.title)
            if (saved in 1 until state2.pageCount) {
                listState.scrollToItem(saved)
            }
            positionRestored = true
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column {
                TopAppBar(
                    title = {
                        Column(
                            Modifier.then(
                                if (state2.pageCount > 1) Modifier.clickable { jumpSheetOpen = true }
                                else Modifier
                            )
                        ) {
                            Text(state.title, maxLines = 1)
                            Text(
                                "Page ${currentPage + 1} of ${state2.pageCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                     actions = {
                         IconButton(onClick = onAnnotate) {
                             Icon(Icons.Default.Edit, contentDescription = "Annotate")
                         }
                     },
                     colors = TopAppBarDefaults.topAppBarColors(
                         containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                     )
                 )
                val progressTarget = (currentPage + 1f) / state2.pageCount
                val progress by animateFloatAsState(
                    targetValue = progressTarget,
                    animationSpec = tween(240)
                )
                LaunchedEffect(currentPage) {
                    if (positionRestored) {
                        ReadingPositionStore.save(context, state.uri, state.title, currentPage)
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Reading progress: page ${currentPage + 1} of ${state2.pageCount}"
                        }
                )
            }
        }
        LazyColumn(
            state = listState,
            userScrollEnabled = !anyPageZoomed,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed((0 until state2.pageCount).toList()) { index, _ ->
                ZoomablePage(
                    renderer = state2,
                    pageIndex = index,
                    onZoom = {
                        if (!anyPageZoomed && it) anyPageZoomed = true
                    },
                    onUnzoom = {
                        if (anyPageZoomed && !it) anyPageZoomed = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (jumpSheetOpen) {
        var selection by remember { mutableIntStateOf(currentPage) }
        ModalBottomSheet(
            onDismissRequest = { jumpSheetOpen = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text("Jump to page", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Page ${selection + 1} of ${state2.pageCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Slider(
                    value = selection.toFloat(),
                    onValueChange = { selection = it.toInt() },
                    onValueChangeFinished = {
                        scope.launch { listState.animateScrollToItem(selection) }
                    },
                    valueRange = 0f..(state2.pageCount - 1).toFloat()
                )
            }
        }
    }
}

@Composable
private fun ZoomablePage(
    renderer: DocumentHolder,
    pageIndex: Int,
    onZoom: (Boolean) -> Unit,
    onUnzoom: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(pageIndex, renderer) { mutableStateOf<Bitmap?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(pageIndex, renderer) {
        withContext(Dispatchers.IO) {
            runCatching {
                synchronized(renderer) {
                    val page = renderer.renderer.openPage(pageIndex)
                    try {
                        val targetW = 1080
                        val targetH = (targetW.toFloat() * page.height / page.width).toInt()
                        val bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                        Canvas(bmp).drawColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    } finally {
                        page.close()
                    }
                }
            }.getOrNull()?.let { bitmap = it }
        }
    }

    LaunchedEffect(scale) {
        if (scale > 1.02f) onZoom(true) else onUnzoom(false)
    }

    val transformState = rememberTransformableState { change, pan, _ ->
        scale = (scale * change).coerceIn(1f, 4f)
        offset = if (scale <= 1.01f) Offset.Zero else offset + pan
    }

    val doubleTapScale = 2.2f
    val gestureModifier = Modifier
        .pointerInput(Unit) {
            detectTapGestures(onDoubleTap = {
                if (scale > 1.05f) {
                    scale = 1f
                    offset = Offset.Zero
                } else {
                    scale = doubleTapScale
                }
            })
        }
        .transformable(transformState)

    Box(
        modifier
            .clipToBounds()
    ) {
        if (bitmap == null) {
            Skeleton(Modifier.fillMaxWidth().aspectRatio(1f / 1.4142f))
        }
        AnimatedVisibility(visible = bitmap != null, enter = fadeIn(), exit = fadeOut()) {
            bitmap?.let {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                        .then(gestureModifier)
                        .semantics {
                            contentDescription = "PDF page ${pageIndex + 1}, pinch to zoom, double tap to zoom"
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            val maxX = size.width * (scale - 1f) / 2f
                            val maxY = size.height * (scale - 1f) / 2f
                            translationX = offset.x.coerceIn(-maxX, maxX)
                            translationY = offset.y.coerceIn(-maxY, maxY)
                        }
                ) {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingPage(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Opening $title…", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ErrorPage(state: ViewerUi.Error, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Failed to open ${state.title}", style = MaterialTheme.typography.titleMedium)
            Text(
                state.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )
        }
    }
}
