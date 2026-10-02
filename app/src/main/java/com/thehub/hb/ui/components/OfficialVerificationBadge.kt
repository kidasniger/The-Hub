package com.thehub.hb.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
 * The Hub official verification seal.
 * Matches the supplied red seal + white check visual.
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
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val outerRadius = this.size.minDimension / 2f
        val innerRadius = outerRadius * 0.88f
        val points = 14
        val path = Path()

        repeat(points * 2) { index ->
            val angle = (-90f + index * (360f / (points * 2))).toDouble()
            val radius = if (index % 2 == 0) outerRadius else innerRadius
            val x = center.x + (cos(Math.toRadians(angle)) * radius).toFloat()
            val y = center.y + (sin(Math.toRadians(angle)) * radius).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        drawPath(path = path, color = red)

        val checkStart = Offset(
            x = this.size.width * 0.27f,
            y = this.size.height * 0.52f
        )
        val checkMiddle = Offset(
            x = this.size.width * 0.44f,
            y = this.size.height * 0.68f
        )
        val checkEnd = Offset(
            x = this.size.width * 0.74f,
            y = this.size.height * 0.34f
        )
        val checkStroke = (this.size.minDimension * 0.14f).coerceAtLeast(1f)

        drawLine(
            color = white,
            start = checkStart,
            end = checkMiddle,
            strokeWidth = checkStroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = white,
            start = checkMiddle,
            end = checkEnd,
            strokeWidth = checkStroke,
            cap = StrokeCap.Round
        )

        // Tiny white inner edge to keep the seal crisp on dark backgrounds.
        drawArc(
            color = white.copy(alpha = 0.92f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius * 0.89f, center.y - outerRadius * 0.89f),
            size = androidx.compose.ui.geometry.Size(outerRadius * 1.78f, outerRadius * 1.78f),
            style = Stroke(width = this.size.minDimension * 0.018f)
        )
    }
}
