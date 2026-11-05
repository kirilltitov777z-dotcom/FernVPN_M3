package com.fern.vpn.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fern.vpn.R
import com.fern.vpn.vpn.VpnManager

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
    val connected = status == VpnManager.Status.CONNECTED
    val connecting = status == VpnManager.Status.CONNECTING

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
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
            Spacer(Modifier.height(8.dp))

            if (petEnabled) {
                PetPlant(totalSeconds = totalSeconds, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }

            PowerButton(
                connected = connected,
                connecting = connecting,
                onToggle = onToggle
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = when (status) {
                    VpnManager.Status.CONNECTED -> stringResource(R.string.status_connected)
                    VpnManager.Status.CONNECTING -> stringResource(R.string.status_connecting)
                    VpnManager.Status.ERROR -> stringResource(R.string.status_error)
                    VpnManager.Status.DISCONNECTED -> stringResource(R.string.status_disconnected)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!error.isNullOrBlank() && status == VpnManager.Status.ERROR) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "\u2193 " + formatSpeed(stats.downSpeed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "\u2191 " + formatSpeed(stats.upSpeed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.height(8.dp))

            SpeedChart(
                samples = samples,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp)
            )

            Spacer(Modifier.height(18.dp))

            StatsCard(stats = stats)

            Spacer(Modifier.height(24.dp))
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

    Box(modifier = Modifier.size(216.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = CircleShape,
            color = background,
            tonalElevation = if (connected) 6.dp else 1.dp,
            modifier = Modifier
                .size(196.dp)
                .clickable(enabled = !connecting, onClick = onToggle)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PowerGlyph(color = foreground, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(if (connected) R.string.disconnect else R.string.connect),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = foreground
                )
            }
        }
    }
}

@Composable
private fun PowerGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.09f
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
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val fillTop = lineColor.copy(alpha = 0.20f)
    val fillBottom = lineColor.copy(alpha = 0.02f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val top = height * 0.12f
        val bottom = height * 0.9f

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
            color = lineColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun StatsCard(stats: VpnManager.Stats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.statistics),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            StatRow(stringResource(R.string.received), formatBytes(stats.rx))
            StatRow(stringResource(R.string.sent), formatBytes(stats.tx))
            StatRow(stringResource(R.string.session_time), formatDuration(stats.sessionSeconds))
            StatRow(stringResource(R.string.server), stringResource(R.string.warp_server))
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
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
