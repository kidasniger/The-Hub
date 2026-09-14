package com.thehub.hb.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HubDarkColorScheme = darkColorScheme(
    primary = HubWhite,
    onPrimary = HubBlack,
    primaryContainer = HubSurfaceElevated,
    onPrimaryContainer = HubWhite,
    secondary = HubLightGray,
    onSecondary = HubBlack,
    background = HubSurfaceDark,
    onBackground = HubWhite,
    surface = HubCard,
    onSurface = HubWhite,
    surfaceVariant = HubSurfaceElevated,
    onSurfaceVariant = HubSecondary,
    outline = HubBorder,
    outlineVariant = HubDarkGray,
    error = HubError,
    onError = HubWhite
)

@Composable
fun TheHubTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HubDarkColorScheme,
        typography = Typography,
        content = content
    )
}
