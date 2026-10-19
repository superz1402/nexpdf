package com.nexpdf.app.ui.tools

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import com.nexpdf.app.data.PdfTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private class ToolJob(
    var label: () -> String = { "" },
    var fraction: Float = 0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onBack: () -> Unit,
    onOrganize: (android.net.Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var job by remember { mutableStateOf<ToolJob?>(null) }

    fun start(label: String, block: suspend (ToolJob) -> String) {
        if (job != null) return
        scope.launch(Dispatchers.IO) {
            val j = ToolJob(label = { label })
            job = j
            val message = runCatching { block(j) }.getOrElse { it.message ?: "Something went wrong" }
            job = null
            snackbar.showSnackbar(message)
        }
    }

    val mergePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNullOrEmpty()) return@rememberLauncherForActivityResult
        start("Merging ${uris.size} documents") { j ->
            val result = PdfTools.merge(context, uris) { done, total ->
                j.label = { "Merging documents $done of $total" }
                j.fraction = done.toFloat() / total
            }
            result.fold(
                onSuccess = { "Merged copy saved to Downloads" },
                onFailure = { it.message ?: "Merge failed" }
            )
        }
    }

    val imagesPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNullOrEmpty()) return@rememberLauncherForActivityResult
        start("Building PDF from ${uris.size} images") { j ->
            val result = PdfTools.imagesToPdf(context, uris) { done, total ->
                j.label = { "Adding images $done of $total" }
                j.fraction = done.toFloat() / total
            }
            result.fold(
                onSuccess = { "PDF saved to Downloads" },
                onFailure = { it.message ?: "Conversion failed" }
            )
        }
    }

    val splitLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        start("Splitting document") { j ->
            val result = PdfTools.split(context, uri, pagesPerPart = 1) { done, total ->
                j.label = { "Creating part $done of $total" }
                j.fraction = if (total > 0) done.toFloat() / total else 0f
            }
            result.fold(
                onSuccess = { "Created ${it.size} parts in Downloads" },
                onFailure = { it.message ?: "Split failed" }
            )
        }
    }

    val organizeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onOrganize(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tools") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(
                visible = job != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val j = job
                if (j != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(Modifier.padding(16.dp).fillMaxWidth()) {
                            Text(j.label(), style = MaterialTheme.typography.titleMedium)
                            LinearProgressIndicator(
                                progress = { j.fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            )
                        }
                    }
                }
            }
            ToolCard(
                title = "Merge PDFs",
                description = "Combine several PDFs into one document.",
                icon = { Icon(Icons.AutoMirrored.Filled.CallMerge, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                label = "Choose PDFs",
                enabled = job == null,
                onClick = { mergePicker.launch(arrayOf("application/pdf")) }
            )
            ToolCard(
                title = "Images → PDF",
                description = "Turn photos and scans into a single PDF.",
                icon = { Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                label = "Choose images",
                enabled = job == null,
                onClick = { imagesPicker.launch(arrayOf("image/*")) }
            )
            ToolCard(
                title = "Split document",
                description = "Break a PDF into one file per page.",
                icon = { Icon(Icons.Default.AutoAwesomeMotion, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                label = "Choose PDF",
                enabled = job == null,
                onClick = { splitLauncher.launch(arrayOf("application/pdf")) }
            )
            ToolCard(
                title = "Organize pages",
                description = "Rotate, delete and reorder pages visually.",
                icon = { Icon(Icons.Default.CropRotate, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                label = "Choose PDF",
                enabled = job == null,
                onClick = { organizeLauncher.launch(arrayOf("application/pdf")) }
            )
        }
    }
}

@Composable
private fun ToolCard(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    androidx.compose.material3.ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = enabled
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            icon()
            Column(Modifier.padding(start = 20.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
