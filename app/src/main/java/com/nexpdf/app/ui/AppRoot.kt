package com.nexpdf.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.nexpdf.app.ui.home.HomeScreen
import com.nexpdf.app.ui.organize.OrganizerScreen
import com.nexpdf.app.ui.tools.ToolsScreen
import com.nexpdf.app.ui.viewer.ViewerScreen

@Composable
fun AppRoot(
    externalViewerUri: String?,
    onExternalViewerClosed: () -> Unit
) {
    var internalRoute by rememberSaveable { mutableStateOf("home") }

    val mode = externalUriMode(externalViewerUri, internalRoute)
    val arg = internalRoute.substringAfter(':', "")

    when (mode) {
        "viewer" -> ViewerScreen(
            uri = if (externalViewerUri != null) externalViewerUri else arg,
            onClose = { if (externalViewerUri != null) onExternalViewerClosed() else internalRoute = "home" }
        )
        "tools" -> ToolsScreen(
            onBack = { internalRoute = "home" },
            onOrganize = { internalRoute = "organizer:$it" }
        )
        "organizer" -> OrganizerScreen(
            uri = arg,
            onBack = { internalRoute = "tools" }
        )
        else -> HomeScreen(
            onOpenDocument = { internalRoute = "viewer:$it" },
            onOpenTools = { internalRoute = "tools" }
        )
    }
}

private fun externalUriMode(externalViewerUri: String?, internalRoute: String): String {
    if (externalViewerUri != null) return "viewer"
    return internalRoute.substringBefore(':')
}
