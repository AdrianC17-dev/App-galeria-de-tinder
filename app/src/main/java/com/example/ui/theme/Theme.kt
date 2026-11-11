package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SwipeFotoColorScheme = darkColorScheme(
    primary = KeepEmerald,
    onPrimary = Color(0xFF042F2E),
    primaryContainer = KeepEmeraldContainer,
    onPrimaryContainer = KeepEmeraldLight,
    secondary = DiscardCrimson,
    onSecondary = Color.White,
    secondaryContainer = DiscardCrimsonContainer,
    onSecondaryContainer = DiscardCrimsonLight,
    tertiary = UndoAmber,
    onTertiary = Color(0xFF1E1300),
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianBorder,
    error = DiscardCrimson
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SwipeFotoColorScheme,
        typography = Typography,
        content = content
    )
}
