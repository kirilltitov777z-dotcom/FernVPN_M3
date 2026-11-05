package com.fern.vpn.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fern.vpn.R
import com.fern.vpn.vpn.VpnManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    status: VpnManager.Status,
    stats: VpnManager.Stats,
    samples: List<Float>,
    error: String?,
    petEnabled: Boolean,
    totalSeconds: Long,
    onToggle: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val connected = status == VpnManager.Status.CONNECTED
    val connecting = status == VpnManager.Status.CONNECTING
    val active = connected || connecting

    var showPet by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }
    var showLower by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showPet = true
        delay(110)
        showButton = true
        delay(150)
        showLower = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        scheme.surface,
                        scheme.surfaceVariant.copy(alpha = 0.35f),
                        scheme.surface
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    actions = {
                        Surface(
                            shape = CircleShape,
                            color = scheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            IconButton(onClick = onOpenSettings) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.settings),
                                    tint = scheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(4.dp))

                AnimatedVisibility(
                    visible = showPet,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 8 }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (petEnabled) {
                            PetPlant(totalSeconds = totalSeconds, modifier = Modifier.fillMaxWidth())
                        } else {
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                AnimatedVisibility(
                    visible = showButton,
                    enter = fadeIn(tween(520, delayMillis = 60)) +
                        slideInVertically(spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)) { it / 5 }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(258.dp)) {
                            if (active) {
                                PulseRings(color = scheme.primary)
                            }
                            PowerButton(connected = connected, connecting = connecting, onToggle = onToggle)
                        }

                        Spacer(Modifier.height(4.dp))

                        StatusPill(status = status)

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = formatDuration(stats.sessionSeconds),
                            style = MaterialTheme.typography.displaySmall,
                            color = scheme.onSurface,
                            fontWeight = FontWeight.Light
                        )

                        if (!error.isNullOrBlank() && status == VpnManager.Status.ERROR) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                AnimatedVisibility(
                    visible = showLower,
                    enter = fadeIn(tween(520, delayMillis = 120)) +
                        slideInVertically(tween(520, delayMillis = 120)) { it / 6 }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.speed_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurfaceVariant
                            )
                            Row {
                                Text(
                                    text = "\u2193 " + formatSpeed(stats.downSpeed),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = scheme.primary
                                )
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    text = "\u2191 " + formatSpeed(stats.upSpeed),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = scheme.secondary
                                )
                            }
                        }

                        SpeedChart(
                            samples = samples,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(118.dp)
                        )

                        Spacer(Modifier.height(18.dp))

                        StatsCard(stats = stats)
                    }
                }

                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun PulseRings(color: Color) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    Canvas(modifier = Modifier.size(258.dp)) {
        val minRadius = size.minDimension * 0.37f
        val maxRadius = size.minDimension * 0.5f
        for (ring in 0..2) {
            val phase = (progress + ring / 3f) % 1f
            drawCircle(
                color = color.copy(alpha = (1f - phase) * 0.16f),
                radius = minRadius + (maxRadius - minRadius) * phase
            )
        }
    }
}

@Composable
private fun PowerButton(connected: Boolean, connecting: Boolean, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val background by animateColorAsState(
        targetValue = if (connected) scheme.primary else scheme.surfaceVariant,
        label = "powerBackground"
    )
    val foreground by animateColorAsState(
        targetValue = if (connected) scheme.onPrimary else scheme.onSurfaceVariant,
        label = "powerForeground"
    )
    val scale by animateFloatAsState(
        targetValue = if (connecting) 0.97f else 1f,
        label = "powerScale"
    )

    Surface(
        shape = CircleShape,
        color = background,
        contentColor = foreground,
        modifier = Modifier
            .size(190.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 22.dp,
                shape = CircleShape,
                ambientColor = scheme.primary.copy(alpha = 0.30f),
                spotColor = scheme.primary.copy(alpha = 0.40f)
            )
            .clickable(enabled = !connecting, onClick = onToggle)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (connecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = foreground,
                    strokeWidth = 3.dp
                )
            } else {
                PowerGlyph(color = foreground, modifier = Modifier.size(50.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(if (connected) R.string.disconnect else R.string.connect),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun StatusPill(status: VpnManager.Status) {
    val scheme = MaterialTheme.colorScheme
    val label = when (status) {
        VpnManager.Status.CONNECTED -> stringResource(R.string.status_connected)
        VpnManager.Status.CONNECTING -> stringResource(R.string.status_connecting)
        VpnManager.Status.ERROR -> stringResource(R.string.status_error)
        VpnManager.Status.DISCONNECTED -> stringResource(R.string.status_disconnected)
    }
    val dot = when (status) {
        VpnManager.Status.CONNECTED -> scheme.primary
        VpnManager.Status.CONNECTING -> scheme.tertiary
        VpnManager.Status.ERROR -> scheme.error
        VpnManager.Status.DISCONNECTED -> scheme.outline
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = scheme.surfaceVariant.copy(alpha = 0.55f),
        contentColor = scheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(dot, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PowerGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.085f
        val radius = size.minDimension * 0.34f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawArc(
            color = color,
            startAngle = -48f,
            sweepAngle = 276f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawLine(
            color = color,
            start = Offset(center.x, size.height * 0.12f),
            end = Offset(center.x, size.height * 0.46f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun SpeedChart(samples: List<Float>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val fillTop = lineColor.copy(alpha = 0.22f)
    val fillBottom = lineColor.copy(alpha = 0.01f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val top = height * 0.10f
        val bottom = height * 0.92f

        for (line in 0..3) {
            val y = top + (bottom - top) * line / 3f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        if (samples.size < 2) return@Canvas

        val maxValue = (samples.maxOrNull() ?: 0f).coerceAtLeast(2048f)
        val stepX = width / (samples.size - 1)
        val linePath = Path()
        val fillPath = Path()

        samples.forEachIndexed { index, value ->
            val x = stepX * index
            val y = bottom - (value / maxValue) * (bottom - top)
            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, bottom)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(width, bottom)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillTop, fillBottom),
                startY = top,
                endY = bottom
            )
        )
        drawPath(
            path = linePath,
            color = lineColor.copy(alpha = 0.16f),
            style = Stroke(width = 10f, cap = StrokeCap.Round)
        )
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun StatsCard(stats: VpnManager.Stats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Metric(
                    modifier = Modifier.weight(1f),
                    arrow = "\u2193",
                    label = stringResource(R.string.received),
                    value = formatBytes(stats.rx)
                )
                Metric(
                    modifier = Modifier.weight(1f),
                    arrow = "\u2191",
                    label = stringResource(R.string.sent),
                    value = formatBytes(stats.tx)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )

            StatRow(stringResource(R.string.session_time), formatDuration(stats.sessionSeconds))
            StatRow(stringResource(R.string.server), stringResource(R.string.warp_server))
        }
    }
}

@Composable
private fun Metric(modifier: Modifier, arrow: String, label: String, value: String) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = arrow,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
