package ru.anikin.intervaltime.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = AppPrimary,
    background = AppBackground,
    surface = AppSurface,
    onBackground = AppOnBackground,
    onSurface = AppOnBackground
)

@Composable
fun IntervalTimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content
    )
}
