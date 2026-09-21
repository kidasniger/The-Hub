package com.thehub.hb.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun OfficialVerificationBadge(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    val red = Color(0xFFE31B3F)
    val white = Color.White

    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        drawCircle(
            color = red,
            radius = radius
        )
        drawCircle(
            color = white,
            radius = radius - 0.8.dp.toPx(),
            style = Stroke(width = 0.9.dp.toPx())
        )

        val start = Offset(
            x = this.size.width * 0.28f,
            y = this.size.height * 0.52f
        )
        val middle = Offset(
            x = this.size.width * 0.43f,
            y = this.size.height * 0.67f
        )
        val end = Offset(
            x = this.size.width * 0.73f,
            y = this.size.height * 0.34f
        )

        drawLine(
            color = white,
            start = start,
            end = middle,
            strokeWidth = size.value * 0.11f * density,
            cap = StrokeCap.Round
        )
        drawLine(
            color = white,
            start = middle,
            end = end,
            strokeWidth = size.value * 0.11f * density,
            cap = StrokeCap.Round
        )
    }
}
