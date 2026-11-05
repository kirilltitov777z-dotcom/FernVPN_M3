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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
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
 * Soft aurora backdrop: a calm night gradient, three very large blurred-looking
 * lights that slowly drift, and a faint star dust layer. Everything is drawn
 * gently so the screen never feels harsh.
 */
@Composable
fun CosmicSky(modifier: Modifier = Modifier, starCount: Int = 90) {
    val scheme = MaterialTheme.colorScheme
    val stars = remember(starCount) {
        val random = Random(2718)
        List(starCount) {
            Star(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = 0.5f + random.nextFloat() * 1.1f,
                phase = random.nextFloat(),
                brightness = 0.10f + random.nextFloat() * 0.35f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "sky")
    val twinkle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "twinkle"
    )
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 120_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )
    val breathe by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val base = scheme.background
    val deep = lerp(base, Color.Black, 0.28f)
    val primary = scheme.primary
    val secondary = scheme.secondary
    val tertiary = scheme.tertiary

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    lerp(base, primary, 0.10f),
                    base,
                    deep
                )
            )
        )

        val swayX = (drift - 0.5f) * size.width * 0.18f
        val swayY = (breathe - 0.5f) * size.height * 0.04f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.26f), Color.Transparent),
                center = Offset(size.width * 0.22f + swayX, size.height * 0.18f + swayY),
                radius = size.width * 0.82f
            ),
            radius = size.width * 0.82f,
            center = Offset(size.width * 0.22f + swayX, size.height * 0.18f + swayY)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(size.width * 0.86f - swayX, size.height * 0.34f),
                radius = size.width * 0.70f
            ),
            radius = size.width * 0.70f,
            center = Offset(size.width * 0.86f - swayX, size.height * 0.34f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiary.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(size.width * 0.48f + swayX * 0.5f, size.height * 0.96f),
                radius = size.width * 0.95f
            ),
            radius = size.width * 0.95f,
            center = Offset(size.width * 0.48f + swayX * 0.5f, size.height * 0.96f)
        )

        // A very soft aurora ribbon sweeping across the upper third.
        rotate(degrees = -14f + drift * 6f, pivot = Offset(size.width * 0.5f, size.height * 0.3f)) {
            drawOval(
                brush = Brush.linearGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.10f),
                        secondary.copy(alpha = 0.14f),
                        Color.Transparent
                    )
                ),
                topLeft = Offset(-size.width * 0.15f, size.height * 0.18f),
                size = Size(size.width * 1.3f, size.height * 0.24f)
            )
        }

        for (star in stars) {
            val wave = 0.5f + 0.5f * sin(6.2831855f * (twinkle + star.phase))
            val alpha = star.brightness * (0.35f + 0.65f * wave)
            drawCircle(
                color = Color.White.copy(alpha = alpha.coerceIn(0f, 0.55f)),
                radius = star.radius,
                center = Offset(
                    (star.x + 0.01f * cos(6.2831855f * star.phase)) * size.width,
                    star.y * size.height
                )
            )
        }
    }
}
