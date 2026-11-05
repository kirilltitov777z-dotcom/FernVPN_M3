package com.fern.vpn.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Fallback palette used before Android 12 and when dynamic colour is unavailable. */
private val CosmicColors = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF10233F),
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFC9BFFF),
    onSecondary = Color(0xFF24164D),
    secondaryContainer = Color(0xFF332A63),
    onSecondaryContainer = Color(0xFFE7DEFF),
    tertiary = Color(0xFFFFC2DB),
    onTertiary = Color(0xFF3D1130),
    tertiaryContainer = Color(0xFF55243F),
    onTertiaryContainer = Color(0xFFFFD8E7),
    background = Color(0xFF05060E),
    onBackground = Color(0xFFE4E7F5),
    surface = Color(0xFF0A0C16),
    onSurface = Color(0xFFE4E7F5),
    surfaceVariant = Color(0xFF181B2B),
    onSurfaceVariant = Color(0xFFBFC4DA),
    outline = Color(0xFF6C7188),
    outlineVariant = Color(0xFF262A3D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val DayColors = lightColorScheme(
    primary = Color(0xFF3B5D8F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD5E3FF),
    onPrimaryContainer = Color(0xFF001B3B),
    secondary = Color(0xFF5A5480),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2DEFF),
    onSecondaryContainer = Color(0xFF17123B),
    tertiary = Color(0xFF8C4A66),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E7),
    onTertiaryContainer = Color(0xFF3A0922),
    background = Color(0xFFFAF8FF),
    onBackground = Color(0xFF1A1B21),
    surface = Color(0xFFFAF8FF),
    onSurface = Color(0xFF1A1B21),
    surfaceVariant = Color(0xFFE1E1EC),
    onSurfaceVariant = Color(0xFF45464F),
    outline = Color(0xFF767680),
    outlineVariant = Color(0xFFC6C6D0)
)

private val FernShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

private val BaseTypography = Typography()

private val FernTypography = Typography(
    displayMedium = BaseTypography.displayMedium.copy(
        fontWeight = FontWeight.Light,
        letterSpacing = (-1.2).sp
    ),
    displaySmall = BaseTypography.displaySmall.copy(
        fontWeight = FontWeight.Light,
        letterSpacing = (-0.8).sp
    ),
    headlineMedium = BaseTypography.headlineMedium.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.4).sp
    ),
    headlineSmall = BaseTypography.headlineSmall.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.Medium),
    labelLarge = BaseTypography.labelLarge.copy(letterSpacing = 0.3.sp)
)

@Composable
fun FernTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> CosmicColors
        else -> DayColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FernTypography,
        shapes = FernShapes,
        content = content
    )
}
