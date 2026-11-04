package com.thehub.hb.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Conteneur d'écran standard. Transparent volontairement :
 * le fond « Aurora Glass » (halos violet/bleu) est peint une seule fois
 * par HubThemeBackground dans MainActivity, derrière toute la navigation.
 */
@Composable
fun HubScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        content()
    }
}
