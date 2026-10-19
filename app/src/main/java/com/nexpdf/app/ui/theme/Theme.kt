package com.nexpdf.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val Ember = Color(0xFFFF8956)
val EmberDim = Color(0xFF8C4A27)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF8F4C25),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCB),
    onPrimaryContainer = Color(0xFF371000),
    secondary = Color(0xFF765749),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBCB),
    onSecondaryContainer = Color(0xFF2B160B),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF201A17),
    surfaceVariant = Color(0xFFF4DED5),
    onSurfaceVariant = Color(0xFF52443D),
    surfaceContainer = Color(0xFFFCEEE8),
    surfaceContainerHigh = Color(0xFFF7E8E2),
    error = Color(0xFFBA1A1A)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFFB68D),
    onPrimary = Color(0xFF531F00),
    primaryContainer = Color(0xFF703600),
    onPrimaryContainer = Color(0xFFFFDBCB),
    secondary = Color(0xFFE5BFAA),
    onSecondary = Color(0xFF432B1E),
    secondaryContainer = Color(0xFF5C4133),
    onSecondaryContainer = Color(0xFFFFDBCB),
    surface = Color(0xFF201A17),
    onSurface = Color(0xFFECE0DA),
    surfaceVariant = Color(0xFF52443D),
    onSurfaceVariant = Color(0xFFD7C3B9),
    surfaceContainer = Color(0xFF261E1A),
    surfaceContainerHigh = Color(0xFF312824),
    error = Color(0xFFFFB4AB)
)

@Composable
fun NexPdfTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NexTypography,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}
