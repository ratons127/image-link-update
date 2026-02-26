package com.qtiqo.share.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = Coral,
    background = Sand,
    surface = SurfaceSoft,
    onSurface = Ink,
    outline = OutlineSoft
)

private val DarkColors = darkColorScheme(
    primary = Teal,
    secondary = Coral,
    background = androidx.compose.ui.graphics.Color(0xFF101214),
    surface = androidx.compose.ui.graphics.Color(0xFF171A1D),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF4F4F4),
    outline = androidx.compose.ui.graphics.Color(0xFF40464D)
)

@Composable
fun QtiqoShareTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
