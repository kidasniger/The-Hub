package com.thehub.hb.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thehub.hb.R

/**
 * The Hub official verification badge.
 * Uses the exact badge artwork supplied for this app.
 */
@Composable
fun OfficialVerificationBadge(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Image(
        painter = painterResource(R.drawable.official_verification_badge),
        contentDescription = "Compte officiel certifié",
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "Compte officiel certifié" }
    )
}
