package com.fern.vpn.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fern.vpn.R

data class PetStage(val thresholdSeconds: Long, val titleRes: Int)

private val stages = listOf(
    PetStage(0L, R.string.pet_stage_seed),
    PetStage(60L, R.string.pet_stage_sprout),
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

    val leaf = Color(0xFF7C9A7A)
    val leafDark = Color(0xFF5E7A60)
    val flower = Color(0xFFCBBD9E)
    val soil = MaterialTheme.colorScheme.outlineVariant
    val next = stages.getOrNull(index + 1)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(92.dp)) {
            drawPlant(index, leaf, leafDark, flower, soil)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(stage.titleRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (next != null) {
            val remaining = (next.thresholdSeconds - totalSeconds).coerceAtLeast(0L)
            Text(
                text = stringResource(R.string.pet_next_stage, formatDuration(remaining)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

    drawLine(
        color = soil,
        start = Offset(width * 0.2f, baseY),
        end = Offset(width * 0.8f, baseY),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )

    if (stageIndex == 0) {
        drawCircle(color = leafDark, radius = width * 0.045f, center = Offset(baseX, baseY - width * 0.05f))
        return
    }

    val stemHeight = when (stageIndex) {
        1 -> height * 0.28f
        2 -> height * 0.45f
        3 -> height * 0.6f
        else -> height * 0.66f
    }

    drawLine(
        color = leafDark,
        start = Offset(baseX, baseY),
        end = Offset(baseX, baseY - stemHeight),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )

    val pairs = when (stageIndex) {
        1 -> 1
        2 -> 2
        3 -> 3
        else -> 3
    }

    for (index in 0 until pairs) {
        val y = baseY - stemHeight * (0.32f + 0.19f * index)
        val length = width * (0.15f + 0.028f * index)
        rotate(degrees = -22f, pivot = Offset(baseX, y)) {
            drawOval(
                color = leaf,
                topLeft = Offset(baseX - length, y - length * 0.34f),
                size = Size(length, length * 0.68f)
            )
        }
        rotate(degrees = 22f, pivot = Offset(baseX, y)) {
            drawOval(
                color = leafDark,
                topLeft = Offset(baseX, y - length * 0.34f),
                size = Size(length, length * 0.68f)
            )
        }
    }

    if (stageIndex >= 4) {
        drawCircle(
            color = flower,
            radius = width * 0.075f,
            center = Offset(baseX, baseY - stemHeight)
        )
    }
}
