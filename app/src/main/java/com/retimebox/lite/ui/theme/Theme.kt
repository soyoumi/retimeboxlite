package com.retimebox.lite.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = Color(0xFF546E7A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFD8DC),
    onSecondaryContainer = Color(0xFF263238),
    tertiary = Color(0xFF0288D1),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB3E5FC),
    onTertiaryContainer = Color(0xFF01579B),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFF5F7FA),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE1E6ED),
    onSurfaceVariant = Color(0xFF41474D),
    outline = Color(0xFF71777F),
    outlineVariant = Color(0xFFC1C7CF)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF0D47A1),
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFBBDEFB),
    secondary = Color(0xFFB0BEC5),
    onSecondary = Color(0xFF263238),
    secondaryContainer = Color(0xFF37474F),
    onSecondaryContainer = Color(0xFFCFD8DC),
    tertiary = Color(0xFF4FC3F7),
    onTertiary = Color(0xFF01579B),
    tertiaryContainer = Color(0xFF0288D1),
    onTertiaryContainer = Color(0xFFB3E5FC),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF121418),
    onBackground = Color(0xFFE1E3E6),
    surface = Color(0xFF121418),
    onSurface = Color(0xFFE1E3E6),
    surfaceVariant = Color(0xFF41474D),
    onSurfaceVariant = Color(0xFFC1C7CF),
    outline = Color(0xFF8B9199),
    outlineVariant = Color(0xFF41474D)
)

@Composable
fun RETimeBoxLiteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}