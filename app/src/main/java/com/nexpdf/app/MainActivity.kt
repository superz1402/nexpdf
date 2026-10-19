package com.nexpdf.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.nexpdf.app.ui.AppRoot
import com.nexpdf.app.ui.theme.NexPdfTheme

class MainActivity : ComponentActivity() {

    private var pendingViewUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent?.data?.toString())
        setContent {
            NexPdfTheme {
                Surface {
                    var viewerUri by rememberSaveable { mutableStateOf(pendingViewUri) }
                    AppRoot(
                        externalViewerUri = viewerUri,
                        onExternalViewerClosed = { viewerUri = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent.data?.toString())
    }

    private fun handleIntent(uri: String?) {
        if (uri != null) pendingViewUri = uri
    }
}
