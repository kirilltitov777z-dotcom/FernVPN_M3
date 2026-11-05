package com.fern.vpn.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fern.vpn.R

private data class OnboardingPage(
    val titleRes: Int,
    val textRes: Int
)

private val onboardingPages = listOf(
    OnboardingPage(R.string.onb_title_welcome, R.string.onb_text_welcome),
    OnboardingPage(R.string.onb_title_connect, R.string.onb_text_connect),
    OnboardingPage(R.string.onb_title_secure, R.string.onb_text_secure)
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val lastPage = page == onboardingPages.lastIndex
    val scheme = MaterialTheme.colorScheme

    Box(modifier = Modifier.fillMaxSize()) {
        CosmicSky(starCount = 170)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 26.dp, vertical = 12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!lastPage) {
                    TextButton(onClick = onFinish) {
                        Text(
                            text = stringResource(R.string.onboarding_skip),
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            PlanetBadge(page = page, modifier = Modifier.align(Alignment.CenterHorizontally))

            Spacer(Modifier.height(36.dp))

            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    (fadeIn(tween(320)) + slideInHorizontally(tween(320)) { it / 6 })
                        .togetherWith(fadeOut(tween(160)) + slideOutHorizontally(tween(240)) { -it / 10 })
                },
                label = "onboardingPage"
            ) { current ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(onboardingPages[current].titleRes),
                        style = MaterialTheme.typography.headlineMedium,
                        color = scheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(onboardingPages[current].textRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                onboardingPages.indices.forEach { index ->
                    val selected = index == page
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = if (selected) 24.dp else 8.dp, height = 8.dp)
                            .background(
                                color = if (selected) scheme.primary else scheme.outlineVariant,
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { if (lastPage) onFinish() else page += 1 },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(
                        if (lastPage) R.string.onboarding_start else R.string.onboarding_next
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

/**
 * A small "planet with an orbit" illustration. The accent colour follows the
 * current page so the onboarding still feels alive.
 */
@Composable
private fun PlanetBadge(page: Int, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val accent by animateColorAsState(
        targetValue = when (page) {
            0 -> scheme.primary
            1 -> scheme.secondary
            else -> scheme.tertiary
        },
        label = "onboardingAccent"
    )

    val transition = rememberInfiniteTransition(label = "planet")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "planetRotation"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "planetBreathe"
    )

    Canvas(modifier = modifier.size(210.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val planetRadius = size.minDimension * 0.24f * breathe

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.35f), Color.Transparent),
                center = center,
                radius = size.minDimension * 0.52f
            ),
            radius = size.minDimension * 0.52f,
            center = center
        )

        rotate(degrees = rotation, pivot = center) {
            drawOval(
                color = accent.copy(alpha = 0.55f),
                topLeft = Offset(center.x - size.minDimension * 0.42f, center.y - size.minDimension * 0.16f),
                size = Size(size.minDimension * 0.84f, size.minDimension * 0.32f),
                style = Stroke(width = 2f)
            )
            val phi = Math.toRadians(rotation.toDouble())
            val x = center.x + (size.minDimension * 0.42f * Math.cos(phi)).toFloat()
            val y = center.y + (size.minDimension * 0.16f * Math.sin(phi)).toFloat()
            drawCircle(color = accent, radius = 5f, center = Offset(x, y))
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.copy(alpha = 0.95f),
                    accent.copy(alpha = 0.45f)
                ),
                center = Offset(center.x - planetRadius * 0.35f, center.y - planetRadius * 0.35f),
                radius = planetRadius * 2.2f
            ),
            radius = planetRadius,
            center = center
        )
        drawCircle(color = accent.copy(alpha = 0.25f), radius = planetRadius * 1.02f, center = center, style = Stroke(width = 1.5f))
        drawLine(
            color = Color.White.copy(alpha = 0.55f),
            start = Offset(center.x - planetRadius * 0.4f, center.y - planetRadius * 0.45f),
            end = Offset(center.x + planetRadius * 0.05f, center.y - planetRadius * 0.62f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
    }
}
