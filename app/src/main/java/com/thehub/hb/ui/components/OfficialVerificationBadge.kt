package com.thehub.hb.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Badge de vérification officielle « Aurora Glass » : cercle plein --success
 * avec coche blanche, comme le pictogramme « Vérifié » du design-system
 * (.pill.ok / .badge-verified). Dessiné en vectoriel pour éviter tout décodage
 * bitmap au démarrage du Feed.
 */
@Composable
fun OfficialVerificationBadge(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    val successColor = com.thehub.hb.ui.theme.HubSuccess
    val white = Color.White

    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "Compte officiel certifié" }
    ) {
        val minSide = this.size.minDimension
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        // Disque de vérification (vert succès de la maquette).
        drawCircle(
            color = successColor,
            radius = minSide / 2f,
            center = center
        )

        // Coche blanche du design-system (circle + check).
        val checkStroke = (minSide * 0.14f).coerceAtLeast(1.2f)
        val start = Offset(minSide * 0.27f, minSide * 0.51f)
        val middle = Offset(minSide * 0.44f, minSide * 0.68f)
        val end = Offset(minSide * 0.75f, minSide * 0.33f)

        drawLine(white, start, middle, checkStroke, StrokeCap.Round)
        drawLine(white, middle, end, checkStroke, StrokeCap.Round)
    }
}

// Feed crash fix validation marker.
