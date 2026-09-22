package com.redhawk.code.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

@Composable
fun RedHawkTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RedHawkColorScheme, content = content)
}
