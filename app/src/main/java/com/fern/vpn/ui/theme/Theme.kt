package com.fern.vpn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.os.Build

private val LightColors = lightColorScheme(
    primary = Color(0xFF4C6650),
    onPrimary = Color(0xFFF8FBF5),
    primaryContainer = Color(0xFFCFE3CF),
    onPrimaryContainer = Color(0xFF0F2415),
    secondary = Color(0xFF56624F),
    onSecondary = Color(0xFFFAFCF6),
    secondaryContainer = Color(0xFFDAE6D2),
    onSecondaryContainer = Color(0xFF131F10),
    tertiary = Color(0xFF6E7A5A),
    onTertiary = Color(0xFFFFFDF2),
    tertiaryContainer = Color(0xFFE7E8C8),
    onTertiaryContainer = Color(0xFF1F2210),
    background = Color(0xFFF7F9F4),
    onBackground = Color(0xFF191D17),
    surface = Color(0xFFF7F9F4),
    onSurface = Color(0xFF191D17),
    surfaceVariant = Color(0xFFDFE4DA),
    onSurfaceVariant = Color(0xFF43483F),
    outline = Color(0xFF73796E),
    outlineVariant = Color(0xFFC3C9BC)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4CDB6),
    onPrimary = Color(0xFF1F3524),
    primaryContainer = Color(0xFF364C3A),
    onPrimaryContainer = Color(0xFFCFE3CF),
    secondary = Color(0xFFBECBB4),
    onSecondary = Color(0xFF283424),
    secondaryContainer = Color(0xFF3E4A39),
    onSecondaryContainer = Color(0xFFDAE6D2),
    tertiary = Color(0xFFCBCDA9),
    onTertiary = Color(0xFF343720),
    tertiaryContainer = Color(0xFF4B4E35),
    onTertiaryContainer = Color(0xFFE7E8C8),
    background = Color(0xFF10140F),
    onBackground = Color(0xFFE1E4DB),
    surface = Color(0xFF10140F),
    onSurface = Color(0xFFE1E4DB),
    surfaceVariant = Color(0xFF43483F),
    onSurfaceVariant = Color(0xFFC3C9BC),
    outline = Color(0xFF8D9387),
    outlineVariant = Color(0xFF43483F)
)

@Composable
fun FernTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
