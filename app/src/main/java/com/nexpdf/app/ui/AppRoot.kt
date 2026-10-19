package com.nexpdf.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.nexpdf.app.ui.home.HomeScreen
import com.nexpdf.app.ui.viewer.ViewerScreen

@Composable
fun AppRoot(
    externalViewerUri: String?,
    onExternalViewerClosed: () -> Unit
) {
    var internalUri by rememberSaveable { mutableStateOf<String?>(null) }
    val activeUri = externalViewerUri ?: internalUri

    if (activeUri == null) {
        HomeScreen(onOpenDocument = { internalUri = it })
    } else {
        ViewerScreen(
            uri = activeUri,
            onClose = {
                if (externalViewerUri != null) onExternalViewerClosed() else internalUri = null
            }
        )
    }
}
