package com.fern.vpn.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Every visual theme the app can paint itself with. */
object Palettes {
    const val AURORA = "aurora"
    const val COSMOS = "cosmos"
    const val EMERALD = "emerald"
    const val OCEAN = "ocean"
    const val SUNSET = "sunset"

    val keys = listOf(AURORA, COSMOS, EMERALD, OCEAN, SUNSET)
}

private fun onColor(color: Color): Color =
    if (color.luminance() > 0.45f) Color(0xFF10131A) else Color(0xFFF7F8FF)

private fun palette(
    dark: Boolean,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    background: Color,
    surface: Color
): ColorScheme {
    val onSurface = if (dark) Color(0xFFE6E8F2) else Color(0xFF1A1B21)
    val variant = if (dark) {
        lerp(surface, primary, 0.13f)
    } else {
        lerp(surface, primary, 0.10f)
    }
    val onVariant = if (dark) {
        lerp(onSurface, background, 0.32f)
    } else {
        lerp(onSurface, background, 0.38f)
    }
    val outline = lerp(onSurface, background, 0.55f)
    val outlineVariant = if (dark) {
        lerp(surface, onSurface, 0.16f)
    } else {
        lerp(surface, onSurface, 0.14f)
    }

    val primaryContainer = lerp(background, primary, if (dark) 0.30f else 0.22f)
    val secondaryContainer = lerp(background, secondary, if (dark) 0.30f else 0.22f)
    val tertiaryContainer = lerp(background, tertiary, if (dark) 0.30f else 0.22f)

    return if (dark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onColor(primary),
            primaryContainer = primaryContainer,
            onPrimaryContainer = lerp(primary, Color.White, 0.62f),
            secondary = secondary,
            onSecondary = onColor(secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = lerp(secondary, Color.White, 0.62f),
            tertiary = tertiary,
            onTertiary = onColor(tertiary),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = lerp(tertiary, Color.White, 0.62f),
            background = background,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = variant,
            onSurfaceVariant = onVariant,
            outline = outline,
            outlineVariant = outlineVariant,
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onColor(primary),
            primaryContainer = primaryContainer,
            onPrimaryContainer = lerp(primary, Color.Black, 0.55f),
            secondary = secondary,
            onSecondary = onColor(secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = lerp(secondary, Color.Black, 0.55f),
            tertiary = tertiary,
            onTertiary = onColor(tertiary),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = lerp(tertiary, Color.Black, 0.55f),
            background = background,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = variant,
            onSurfaceVariant = onVariant,
            outline = outline,
            outlineVariant = outlineVariant,
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002)
        )
    }
}

private fun schemeFor(paletteKey: String, dark: Boolean): ColorScheme = when (paletteKey) {
    Palettes.AURORA -> if (dark) {
        palette(
            dark = true,
            primary = Color(0xFFA8E6CF),
            secondary = Color(0xFFC9C4FF),
            tertiary = Color(0xFFFFD3B6),
            background = Color(0xFF070D11),
            surface = Color(0xFF0D171E)
        )
    } else {
        palette(
            dark = false,
            primary = Color(0xFF2F7D68),
            secondary = Color(0xFF615C9B),
            tertiary = Color(0xFFA15D3C),
            background = Color(0xFFFAFBF7),
            surface = Color(0xFFFDFDFB)
        )
    }

    Palettes.EMERALD -> if (dark) {
        palette(
            dark = true,
            primary = Color(0xFF7BE3AE),
            secondary = Color(0xFF9FD9C4),
            tertiary = Color(0xFFDCE98F),
            background = Color(0xFF03100B),
            surface = Color(0xFF081610)
        )
    } else {
        palette(
            dark = false,
            primary = Color(0xFF1F7A52),
            secondary = Color(0xFF3F6F5C),
            tertiary = Color(0xFF6B6A1C),
            background = Color(0xFFF5FBF6),
            surface = Color(0xFFF7FCF8)
        )
    }

    Palettes.OCEAN -> if (dark) {
        palette(
            dark = true,
            primary = Color(0xFF79C8FF),
            secondary = Color(0xFF8FB8FF),
            tertiary = Color(0xFFA9E7FF),
            background = Color(0xFF02080F),
            surface = Color(0xFF071220)
        )
    } else {
        palette(
            dark = false,
            primary = Color(0xFF1C5E8F),
            secondary = Color(0xFF3D5DA8),
            tertiary = Color(0xFF196F8C),
            background = Color(0xFFF5FAFF),
            surface = Color(0xFFF7FBFF)
        )
    }

    Palettes.SUNSET -> if (dark) {
        palette(
            dark = true,
            primary = Color(0xFFFFB178),
            secondary = Color(0xFFFF9AA8),
            tertiary = Color(0xFFE0A6FF),
            background = Color(0xFF120409),
            surface = Color(0xFF1C0A10)
        )
    } else {
        palette(
            dark = false,
            primary = Color(0xFF9A4A12),
            secondary = Color(0xFF9C3B52),
            tertiary = Color(0xFF7B3FA8),
            background = Color(0xFFFFF8F5),
            surface = Color(0xFFFFFAF8)
        )
    }

    else -> if (dark) {
        palette(
            dark = true,
            primary = Color(0xFFA9C7FF),
            secondary = Color(0xFFC9BFFF),
            tertiary = Color(0xFFFFC2DB),
            background = Color(0xFF05060E),
            surface = Color(0xFF0A0C16)
        )
    } else {
        palette(
            dark = false,
            primary = Color(0xFF3B5D8F),
            secondary = Color(0xFF5A5480),
            tertiary = Color(0xFF8C4A66),
            background = Color(0xFFFAF8FF),
            surface = Color(0xFFFFFBFF)
        )
    }
}

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
    paletteKey: String = Palettes.AURORA,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = schemeFor(paletteKey, darkTheme),
        typography = FernTypography,
        shapes = FernShapes,
        content = content
    )
}
