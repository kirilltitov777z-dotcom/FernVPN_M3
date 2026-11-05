package com.fern.vpn.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/** Translucent "soft glass" card that floats above the aurora. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        scheme.surface.copy(alpha = 0.82f),
                        scheme.surface.copy(alpha = 0.56f)
                    )
                )
            )
            .border(
                border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.42f)),
                shape = shape
            )
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

/**
 * The Aster mark: a six-petal flower / soft star, slowly turning.
 */
@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val secondary = scheme.secondary
    val tertiary = scheme.tertiary

    val transition = rememberInfiniteTransition(label = "brand")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 48_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "brandSpin"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.30f), Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        rotate(degrees = spin, pivot = center) {
            for (index in 0 until 6) {
                rotate(degrees = index * 60f, pivot = center) {
                    drawOval(
                        brush = Brush.verticalGradient(
                            colors = listOf(primary, secondary),
                            startY = center.y - radius,
                            endY = center.y + radius
                        ),
                        topLeft = Offset(center.x - radius * 0.15f, center.y - radius * 0.86f),
                        size = Size(radius * 0.30f, radius * 0.60f)
                    )
                }
            }
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.92f), tertiary),
                center = center,
                radius = radius * 0.22f
            ),
            radius = radius * 0.19f,
            center = center
        )
    }
}
