package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CherryPrimaryDark,
    onPrimary = CherryOnPrimaryDark,
    primaryContainer = CherryPrimaryContainerDark,
    onPrimaryContainer = CherryOnPrimaryContainerDark,
    secondary = CherrySecondaryDark,
    onSecondary = CherryOnSecondaryDark,
    secondaryContainer = CherrySecondaryContainerDark,
    onSecondaryContainer = CherryOnSecondaryContainerDark,
    tertiary = CherryTertiaryDark,
    onTertiary = CherryOnTertiaryDark,
    tertiaryContainer = CherryTertiaryContainerDark,
    onTertiaryContainer = CherryOnTertiaryContainerDark,
    background = CherryBackgroundDark,
    onBackground = CherryOnBackgroundDark,
    surface = CherrySurfaceDark,
    onSurface = CherryOnSurfaceDark,
    surfaceVariant = CherrySurfaceVariantDark,
    onSurfaceVariant = CherryOnSurfaceVariantDark,
    outline = CherryOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = CherryPrimary,
    onPrimary = CherryOnPrimary,
    primaryContainer = CherryPrimaryContainer,
    onPrimaryContainer = CherryOnPrimaryContainer,
    secondary = CherrySecondary,
    onSecondary = CherryOnSecondary,
    secondaryContainer = CherrySecondaryContainer,
    onSecondaryContainer = CherryOnSecondaryContainer,
    tertiary = CherryTertiary,
    onTertiary = CherryOnTertiary,
    tertiaryContainer = CherryTertiaryContainer,
    onTertiaryContainer = CherryOnTertiaryContainer,
    background = CherryBackgroundLight,
    onBackground = CherryOnBackgroundLight,
    surface = CherrySurfaceLight,
    onSurface = CherryOnSurfaceLight,
    surfaceVariant = CherrySurfaceVariantLight,
    onSurfaceVariant = CherryOnSurfaceVariantLight,
    outline = CherryOutlineLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our signature Cherry Palette by default
    content: @Composable () -> Unit,
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
        typography = Typography,
        content = content
    )
}
