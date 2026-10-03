package com.thehub.hb.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Hub official admin verification badge.
 * Drawn as a vector to avoid bitmap decoding at Feed startup.
 */
@Composable
fun OfficialVerificationBadge(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    val red = Color(0xFFE43B36)
    val white = Color.White

    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "Compte officiel certifié" }
    ) {
        val minSide = this.size.minDimension
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val outerRadius = minSide * 0.48f
        val innerRadius = outerRadius * 0.84f
        val points = 14
        val seal = Path()

        repeat(points * 2) { index ->
            val angle = Math.toRadians(
                (-90f + index * (360f / (points * 2))).toDouble()
            )
            val radius = if (index % 2 == 0) outerRadius else innerRadius
            val x = center.x + (cos(angle) * radius).toFloat()
            val y = center.y + (sin(angle) * radius).toFloat()
            if (index == 0) seal.moveTo(x, y) else seal.lineTo(x, y)
        }
        seal.close()

        drawPath(seal, red)

        val checkStroke = (minSide * 0.14f).coerceAtLeast(1.2f)
        val start = Offset(minSide * 0.27f, minSide * 0.51f)
        val middle = Offset(minSide * 0.44f, minSide * 0.68f)
        val end = Offset(minSide * 0.75f, minSide * 0.33f)

        drawLine(white, start, middle, checkStroke, StrokeCap.Round)
        drawLine(white, middle, end, checkStroke, StrokeCap.Round)

        drawCircle(
            color = white.copy(alpha = 0.16f),
            radius = outerRadius * 0.90f,
            center = center,
            style = Stroke(width = (minSide * 0.018f).coerceAtLeast(0.5f))
        )
    }
}

// Feed crash fix validation marker.
