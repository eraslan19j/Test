package com.redhawk.code.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.redhawk.code.data.prefs.PrefsStore

private val RedHawkColorScheme = darkColorScheme(
    primary = RedHawkRed,
    onPrimary = Color.White,
    primaryContainer = RedHawkRedDark,
    onPrimaryContainer = Color.White,
    secondary = RedHawkRedLight,
    onSecondary = Color.Black,
    background = RedHawkBg,
    onBackground = RedHawkText,
    surface = RedHawkSurface,
    onSurface = RedHawkText,
    surfaceVariant = RedHawkSurfaceV2,
    onSurfaceVariant = RedHawkTextDim,
    outline = RedHawkOutline,
    error = RedHawkRed,
)

private val RedHawkLightScheme = lightColorScheme(
    primary = RedHawkRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD4),
    onPrimaryContainer = Color(0xFF410001),
    secondary = RedHawkRedDark,
    onSecondary = Color.White,
    background = Color(0xFFFBF8F8),
    onBackground = Color(0xFF1A1111),
    surface = Color.White,
    onSurface = Color(0xFF1A1111),
    surfaceVariant = Color(0xFFF3EDED),
    onSurfaceVariant = Color(0xFF6B5B5B),
    outline = Color(0xFFD8C2C2),
    error = Color(0xFFBA1A1A),
)

@Composable
fun RedHawkTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { PrefsStore(ctx) }
    val theme by prefs.theme.collectAsState(initial = "dark")
    val dark = when (theme) {
        "light" -> false
        "system" -> isSystemInDarkTheme()
        else -> true
    }
    MaterialTheme(
        colorScheme = if (dark) RedHawkColorScheme else RedHawkLightScheme,
        content = content
    )
}
