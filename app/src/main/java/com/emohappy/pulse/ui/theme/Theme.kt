package com.emohappy.pulse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = HsbcRed,
    onPrimary = Color.White,
    primaryContainer = HsbcPrimaryLight,
    onPrimaryContainer = HsbcDarkRed,
    secondary = BlueSky,
    onSecondary = Color.White,
    background = BgLight,
    onBackground = TextPrimary,
    surface = CardBg,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextMuted,
    outline = BorderLight
)

@Composable
fun PulseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
