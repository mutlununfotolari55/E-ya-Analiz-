package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val FuturisticColorScheme = darkColorScheme(
    primary = CyberGreen,
    secondary = CyberCyan,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = DarkBackground,
    onSecondary = DarkBackground,
    onBackground = TextLight,
    onSurface = TextLight,
    error = ErrorRed,
    primaryContainer = DarkSurface,
    onPrimaryContainer = CyberGreen,
    secondaryContainer = DarkSurface,
    onSecondaryContainer = CyberCyan
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme
    dynamicColor: Boolean = false, // Disable dynamic color to enforce futuristic vibe
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = FuturisticColorScheme,
        typography = Typography,
        content = content
    )
}
