package com.fern.vpn.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fern.vpn.R
import com.fern.vpn.data.SettingsRepository
import com.fern.vpn.vpn.VpnManager
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    status: VpnManager.Status,
    stats: VpnManager.Stats,
    samples: List<Float>,
    error: String?,
    configKind: String?,
    configName: String?,
    generating: Boolean,
    generatingSeconds: Int,
    endpoint: String,
    attempt: Int,
    attemptTotal: Int,
    onToggle: () -> Unit,
    onGenerate: () -> Unit,
    onCancelGenerate: () -> Unit,
    onImport: (String, String) -> Boolean,
    onOpenSettings: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val connected = status == VpnManager.Status.CONNECTED
    val connecting = status == VpnManager.Status.CONNECTING
    val context = LocalContext.current

    var showHeader by remember { mutableStateOf(false) }
    var showOrb by remember { mutableStateOf(false) }
    var showLower by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val loaded = runCatching {
            val name = context.displayName(uri) ?: "config.conf"
            val text = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: throw IllegalStateException("empty")
            name to text
        }.getOrNull()

        if (loaded == null) {
            Toast.makeText(context, R.string.import_failed, Toast.LENGTH_LONG).show()
        } else {
            val ok = onImport(loaded.first, loaded.second)
            Toast.makeText(
                context,
                if (ok) R.string.import_ok else R.string.import_failed,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        showHeader = true
        delay(120)
        showOrb = true
        delay(180)
        showLower = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CosmicSky()

        Scaffold(containerColor = Color.Transparent) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))

                AnimatedVisibility(
                    visible = showHeader,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -it / 6 }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = scheme.surface.copy(alpha = 0.60f),
                                border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                BrandMark(
                                    modifier = Modifier
                                        .padding(7.dp)
                                        .size(30.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = stringResource(R.string.app_tagline),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = scheme.surface.copy(alpha = 0.55f),
                                border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.6f))
                            ) {
                                IconButton(onClick = onOpenSettings) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = stringResource(R.string.settings),
                                        tint = scheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        ConfigCard(
                            kind = configKind,
                            name = configName,
                            generating = generating,
                            generatingSeconds = generatingSeconds,
                            onGenerate = onGenerate,
                            onCancelGenerate = onCancelGenerate,
                            onPickFile = { filePicker.launch(arrayOf("*/*")) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = showOrb,
                    enter = fadeIn(tween(600, delayMillis = 60)) +
                        slideInVertically(spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow)) { it / 5 }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(276.dp)) {
                            if (connected) {
                                OrbitRing(color = scheme.primary)
                            }
                            if (connected || connecting) {
                                PulseRings(color = if (connected) scheme.primary else scheme.tertiary)
                            }
                            PowerCore(
                                connected = connected,
                                connecting = connecting,
                                onToggle = onToggle
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(status = status, attempt = attempt, attemptTotal = attemptTotal)
                            if (connected && stats.latencyMs > 0) {
                                PingChip(latencyMs = stats.latencyMs)
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = formatDuration(stats.sessionSeconds),
                            style = MaterialTheme.typography.displayMedium,
                            color = scheme.onSurface
                        )

                        if (connected && endpoint.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.server) + " · " + endpoint,
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        if (!error.isNullOrBlank() && status == VpnManager.Status.ERROR) {
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = scheme.errorContainer.copy(alpha = 0.55f),
                                border = BorderStroke(1.dp, scheme.error.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onErrorContainer,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                AnimatedVisibility(
                    visible = showLower,
                    enter = fadeIn(tween(560, delayMillis = 120)) +
                        slideInVertically(tween(560, delayMillis = 120)) { it / 7 }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        GlassCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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

                            Spacer(Modifier.height(8.dp))

                            SpeedChart(
                                samples = samples,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(104.dp)
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 14.dp),
                                color = scheme.outlineVariant.copy(alpha = 0.45f)
                            )

                            StatRow(stringResource(R.string.session_time), formatDuration(stats.sessionSeconds))
                            StatRow(stringResource(R.string.connection), configurationLabel(configKind, configName))
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun ConfigCard(
    kind: String?,
    name: String?,
    generating: Boolean,
    generatingSeconds: Int,
    onGenerate: () -> Unit,
    onCancelGenerate: () -> Unit,
    onPickFile: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val configured = kind != null

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = scheme.surface.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(
                            color = when {
                                generating -> scheme.tertiary
                                configured -> scheme.primary
                                else -> scheme.outline
                            },
                            shape = CircleShape
                        )
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = configurationLabel(kind, name),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = when {
                            generating -> stringResource(R.string.seconds_elapsed, generatingSeconds)
                            configured -> stringResource(R.string.config_ready)
                            else -> stringResource(R.string.config_hint)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (generating) scheme.tertiary else scheme.onSurfaceVariant
                    )
                }
                if (generating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = scheme.tertiary
                    )
                }
            }

            if (generating) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.generating_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onGenerate,
                    enabled = !generating,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(50),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(
                            if (generating) R.string.generating else R.string.generate
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
                Button(
                    onClick = if (generating) onCancelGenerate else onPickFile,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(50),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.secondaryContainer,
                        contentColor = scheme.onSecondaryContainer
                    )
                ) {
                    Text(
                        text = stringResource(
                            if (generating) R.string.cancel else R.string.from_file
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun configurationLabel(kind: String?, name: String?): String = when (kind) {
    SettingsRepository.CONFIG_AUTO -> stringResource(R.string.config_auto)
    SettingsRepository.CONFIG_FILE -> name ?: stringResource(R.string.config_file)
    else -> stringResource(R.string.config_none)
}

private fun Context.displayName(uri: Uri): String? {
    return runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else {
                    null
                }
            }
    }.getOrNull()
}

@Composable
private fun PulseRings(color: Color) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    Canvas(modifier = Modifier.size(276.dp)) {
        val minRadius = size.minDimension * 0.36f
        val maxRadius = size.minDimension * 0.5f
        for (ring in 0..2) {
            val phase = (progress + ring / 3f) % 1f
            drawCircle(
                color = color.copy(alpha = (1f - phase) * 0.20f),
                radius = minRadius + (maxRadius - minRadius) * phase
            )
        }
    }
}

@Composable
private fun OrbitRing(color: Color) {
    val transition = rememberInfiniteTransition(label = "orbit")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitAngle"
    )

    Canvas(
        modifier = Modifier
            .size(272.dp)
            .graphicsLayer { rotationZ = angle }
    ) {
        val radius = size.minDimension * 0.475f
        drawCircle(
            color = color.copy(alpha = 0.28f),
            radius = radius,
            style = Stroke(width = 1.5f)
        )
        for (dot in 0..2) {
            val phi = Math.toRadians((dot * 120).toDouble())
            val x = center.x + (radius * Math.cos(phi)).toFloat()
            val y = center.y + (radius * Math.sin(phi)).toFloat()
            drawCircle(color = color.copy(alpha = 0.75f), radius = 3.5f, center = Offset(x, y))
        }
    }
}

@Composable
private fun PowerCore(connected: Boolean, connecting: Boolean, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val background by animateColorAsState(
        targetValue = if (connected) scheme.primary else scheme.surfaceVariant.copy(alpha = 0.85f),
        label = "powerBackground"
    )
    val foreground by animateColorAsState(
        targetValue = if (connected) scheme.onPrimary else scheme.onSurfaceVariant,
        label = "powerForeground"
    )
    val glow by animateColorAsState(
        targetValue = if (connected) scheme.primary else scheme.primary.copy(alpha = 0.25f),
        label = "powerGlow"
    )
    val scale by animateFloatAsState(
        targetValue = if (connecting) 0.96f else 1f,
        label = "powerScale"
    )

    Box(
        modifier = Modifier
            .size(200.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 30.dp,
                shape = CircleShape,
                ambientColor = glow.copy(alpha = 0.6f),
                spotColor = glow.copy(alpha = 0.75f)
            )
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        lerp(background, Color.White, 0.22f),
                        background,
                        lerp(background, Color.Black, 0.25f)
                    )
                )
            )
            .clickable(enabled = !connecting) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (connecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(38.dp),
                    color = foreground,
                    strokeWidth = 3.dp
                )
            } else {
                PowerGlyph(color = foreground, modifier = Modifier.size(52.dp))
            }
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

@Composable
private fun StatusPill(status: VpnManager.Status, attempt: Int, attemptTotal: Int) {
    val scheme = MaterialTheme.colorScheme
    val label = when (status) {
        VpnManager.Status.CONNECTED -> stringResource(R.string.status_connected)
        VpnManager.Status.CONNECTING -> {
            if (attempt > 0 && attemptTotal > 0) {
                stringResource(R.string.status_tuning, attempt, attemptTotal)
            } else {
                stringResource(R.string.status_connecting)
            }
        }

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
        color = scheme.surface.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.55f)),
        contentColor = scheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
private fun PingChip(latencyMs: Int) {
    val scheme = MaterialTheme.colorScheme
    val dot = when {
        latencyMs <= 80 -> scheme.primary
        latencyMs <= 180 -> scheme.tertiary
        else -> scheme.error
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = scheme.surface.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.55f)),
        contentColor = scheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(dot, CircleShape)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = stringResource(R.string.ping) + " " + latencyMs + " ms",
                style = MaterialTheme.typography.labelLarge
            )
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
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val fillTop = lineColor.copy(alpha = 0.26f)
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
            color = lineColor.copy(alpha = 0.18f),
            style = Stroke(width = 12f, cap = StrokeCap.Round)
        )
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
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
