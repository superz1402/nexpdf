package com.nexpdf.app.ui.viewer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class DocumentHolder(
    val renderer: PdfRenderer,
    private val pfd: ParcelFileDescriptor
) : AutoCloseable {
    override fun close() {
        try { renderer.close() } catch (_: Exception) {}
        try { pfd.close() } catch (_: Exception) {}
    }
}

private sealed interface ViewerUi {
    data class Loading(val title: String) : ViewerUi
    data class Error(val title: String, val message: String) : ViewerUi
    data class Ready(val title: String, val holder: DocumentHolder) : ViewerUi
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(uri: String, onClose: () -> Unit) {
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
                uiState = ViewerUi.Ready(title, it)
            }.onFailure {
                uiState = ViewerUi.Error(title, it.message ?: "Could not open document")
            }
        }
    }

    when (val state = uiState) {
        is ViewerUi.Ready -> Pager(state)
        is ViewerUi.Error -> ErrorPage(state, onClose)
        is ViewerUi.Loading -> LoadingPage(title)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Pager(state: ViewerUi.Ready) {
    val renderer = state.holder.renderer
    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            TopAppBar(
                title = {
                    Column {
                        Text(state.title, maxLines = 1)
                        Text(
                            "${renderer.pageCount} page" + if (renderer.pageCount == 1) "" else "s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items((0 until renderer.pageCount).toList()) { index ->
                PageBitmap(
                    renderer = renderer,
                    pageIndex = index,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PageBitmap(
    renderer: PdfRenderer,
    pageIndex: Int,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(pageIndex, renderer) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(pageIndex, renderer) {
        withContext(Dispatchers.IO) {
            runCatching {
                synchronized(renderer) {
                    val page = renderer.openPage(pageIndex)
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
    Box(modifier, contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = bitmap != null, enter = fadeIn(), exit = fadeOut()) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "PDF page ${pageIndex + 1}",
                    contentScale = ContentScale.FillWidth
                )
            }
        }
        if (bitmap == null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 220.dp)
                )
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
            Text(
                "Failed to open ${state.title}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                state.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(16.dp)
                    .width(280.dp)
            )
        }
    }
}
