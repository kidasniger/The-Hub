package com.thehub.hb.ui.terms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceDark
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite

@Composable
fun TermsScreen(
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HubSurfaceDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(HubSurfaceDark)
                .border(width = 1.dp, color = HubBorder)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(HubSurfaceElevated)
                    .border(1.dp, HubBorder, CircleShape)
                    .testTag("terms_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Retour",
                    tint = HubWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = "Conditions & confidentialité",
                color = HubWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TermSection(
                title = "1. Introduction",
                content = "The Hub est un réseau social minimaliste axé sur le partage texte + image uniquement. Aucun tracking publicitaire, aucun flux infini."
            )

            TermSection(
                title = "2. Données personnelles",
                content = "Nous collectons uniquement ce que tu publies. Pas de revente de données à des tiers. Stockage chiffré. Suppression de compte et de données sur demande."
            )

            TermSection(
                title = "3. Contenu et respect",
                content = "Texte et image uniquement. Pas de vidéo, pas d'audio. Nous encourageons le silence et la lenteur. Tout contenu haineux ou illégal est immédiatement modéré."
            )

            TermSection(
                title = "4. Ton cercle, tes règles",
                content = "Tu contrôles qui te suit et qui voit tes posts. Tu peux mettre en sourdine, bloquer et signaler à tout moment."
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Dernière mise à jour : 2026 • The Hub",
                color = HubMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun TermSection(title: String, content: String) {
    Column {
        Text(
            text = title,
            color = HubWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = content,
            color = HubSecondary,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}
