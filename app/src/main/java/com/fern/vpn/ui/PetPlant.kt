package com.fern.vpn.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fern.vpn.R

data class PetStage(val thresholdSeconds: Long, val titleRes: Int)

/** The fern starts as a sprout — there is no seed stage. */
private val stages = listOf(
    PetStage(0L, R.string.pet_stage_sprout),
    PetStage(1_800L, R.string.pet_stage_young),
    PetStage(7_200L, R.string.pet_stage_grown),
    PetStage(86_400L, R.string.pet_stage_bloom)
)

fun petStageIndex(totalSeconds: Long): Int = stages.indexOfLast { totalSeconds >= it.thresholdSeconds }

val petStageCount: Int = stages.size

@Composable
fun PetPlant(totalSeconds: Long, modifier: Modifier = Modifier) {
    val index = petStageIndex(totalSeconds)
    val stage = stages[index]
    val next = stages.getOrNull(index + 1)
    val scheme = MaterialTheme.colorScheme

    val leaf = Color(0xFF8FD3AC)
    val leafDark = Color(0xFF5F9E7F)
    val flower = scheme.tertiary
    val soil = scheme.outlineVariant

    val sway = rememberInfiniteTransition(label = "petSway")
    val angle by sway.animateFloat(
        initialValue = -2.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "petAngle"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = scheme.surface.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer { rotationZ = angle }
            ) {
                drawPlant(index, leaf, leafDark, flower, soil)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(stage.titleRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (next != null) {
                        stringResource(
                            R.string.pet_next_stage,
                            formatDuration((next.thresholdSeconds - totalSeconds).coerceAtLeast(0L))
                        )
                    } else {
                        stringResource(R.string.pet_max_stage)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.85f)
                )
            }
        }
    }
}

private fun DrawScope.drawPlant(
    stageIndex: Int,
    leaf: Color,
    leafDark: Color,
    flower: Color,
    soil: Color
) {
    val width = size.width
    val height = size.height
    val baseX = width / 2f
    val baseY = height * 0.92f

    drawOval(
        color = soil.copy(alpha = 0.5f),
        topLeft = Offset(width * 0.22f, baseY - width * 0.02f),
        size = Size(width * 0.56f, width * 0.06f)
    )

    val stemHeight = when (stageIndex) {
        0 -> height * 0.34f
        1 -> height * 0.52f
        2 -> height * 0.66f
        else -> height * 0.74f
    }

    drawLine(
        color = leafDark,
        start = Offset(baseX, baseY),
        end = Offset(baseX, baseY - stemHeight),
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )

    val pairs = when (stageIndex) {
        0 -> 1
        1 -> 2
        2 -> 3
        else -> 3
    }

    for (leafIndex in 0 until pairs) {
        val y = baseY - stemHeight * (0.34f + 0.19f * leafIndex)
        val length = width * (0.20f + 0.03f * leafIndex)
        val sway = if (leafIndex % 2 == 0) -4f else 4f
        rotate(degrees = -22f + sway, pivot = Offset(baseX, y)) {
            drawOval(
                color = leaf,
                topLeft = Offset(baseX - length, y - length * 0.34f),
                size = Size(length, length * 0.68f)
            )
        }
        rotate(degrees = 22f + sway, pivot = Offset(baseX, y)) {
            drawOval(
                color = leafDark,
                topLeft = Offset(baseX, y - length * 0.34f),
                size = Size(length, length * 0.68f)
            )
        }
    }

    if (stageIndex >= 3) {
        drawCircle(
            color = flower.copy(alpha = 0.30f),
            radius = width * 0.13f,
            center = Offset(baseX, baseY - stemHeight)
        )
        drawCircle(
            color = flower,
            radius = width * 0.075f,
            center = Offset(baseX, baseY - stemHeight)
        )
    }
}
