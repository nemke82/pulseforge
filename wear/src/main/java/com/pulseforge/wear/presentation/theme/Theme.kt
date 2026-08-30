package com.pulseforge.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

val WearPrimary = Color(0xFF00E676)
val WearPrimaryVariant = Color(0xFF00B0FF)
val WearSecondary = Color(0xFFFF5252)
val WearBackground = Color(0xFF0A0E17)
val WearSurface = Color(0xFF161E2E)

val WearColorPalette = Colors(
    primary = WearPrimary,
    primaryVariant = WearPrimaryVariant,
    secondary = WearSecondary,
    background = WearBackground,
    surface = WearSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun PulseForgeWearTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = WearColorPalette,
        content = content
    )
}
