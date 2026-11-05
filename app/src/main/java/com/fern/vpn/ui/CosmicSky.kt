package com.fern.vpn.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.sin
import kotlin.random.Random

private data class Star(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
    val brightness: Float
)

/**
 * Deep space backdrop: vertical night gradient, two soft nebulae tinted with the
 * Material You palette and a slowly twinkling star field.
 */
@Composable
fun CosmicSky(modifier: Modifier = Modifier, starCount: Int = 120) {
    val scheme = MaterialTheme.colorScheme
    val stars = remember(starCount) {
        val random = Random(2718)
        List(starCount) {
            Star(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = 0.6f + random.nextFloat() * 1.6f,
                phase = random.nextFloat(),
                brightness = 0.30f + random.nextFloat() * 0.70f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "sky")
    val twinkle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "twinkle"
    )
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 90_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    val base = scheme.background
    val deep = lerp(base, Color.Black, 0.45f)
    val primary = scheme.primary
    val secondary = scheme.secondary
    val tertiary = scheme.tertiary

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(deep, lerp(base, primary, 0.07f), deep)
            )
        )

        val sway = (drift - 0.5f) * size.width * 0.10f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.34f), Color.Transparent),
                center = Offset(size.width * 0.22f + sway, size.height * 0.16f),
                radius = size.width * 0.72f
            ),
            radius = size.width * 0.72f,
            center = Offset(size.width * 0.22f + sway, size.height * 0.16f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = 0.26f), Color.Transparent),
                center = Offset(size.width * 0.88f - sway, size.height * 0.40f),
                radius = size.width * 0.60f
            ),
            radius = size.width * 0.60f,
            center = Offset(size.width * 0.88f - sway, size.height * 0.40f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiary.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(size.width * 0.50f, size.height * 1.02f),
                radius = size.width * 0.90f
            ),
            radius = size.width * 0.90f,
            center = Offset(size.width * 0.50f, size.height * 1.02f)
        )

        for (star in stars) {
            val wave = 0.5f + 0.5f * sin(6.2831855f * (twinkle + star.phase))
            val alpha = star.brightness * (0.25f + 0.75f * wave)
            drawCircle(
                color = Color.White.copy(alpha = alpha.coerceIn(0f, 0.95f)),
                radius = star.radius,
                center = Offset(star.x * size.width, star.y * size.height)
            )
        }
    }
}
