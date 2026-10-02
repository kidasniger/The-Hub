package com.thehub.hb.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.ui.components.AppLogo
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubButtonVariant
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceDark
import com.thehub.hb.ui.theme.HubThemeBackground
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubWhite

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToTerms: () -> Unit
) {
    HubThemeBackground(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(28.dp))

            AppLogo(size = 64.dp, animated = false)

            Spacer(modifier = Modifier.height(38.dp))

            Text(
                text = "Bienvenue sur\nThe Hub.",
                color = HubWhite,
                fontSize = 42.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 49.sp,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Le réseau où le silence est une feature.",
                color = HubSecondary,
                fontSize = 17.sp,
                lineHeight = 25.sp
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WelcomeFeatureRow(Icons.Outlined.Explore, "Découvrir", "Trouver des personnes et des publications")
            WelcomeFeatureRow(Icons.Outlined.EditNote, "Publier", "Partager une idée, une photo ou une vidéo")
            WelcomeFeatureRow(Icons.Outlined.ChatBubbleOutline, "Échanger", "Rester proche de ta communauté")
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            HubButton(
                text = "Se connecter",
                onClick = onNavigateToLogin,
                variant = HubButtonVariant.Primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            val termsText = buildAnnotatedString {
                append("En continuant, tu acceptes nos ")
                withStyle(SpanStyle(color = HubSecondary, textDecoration = TextDecoration.Underline)) {
                    append("CGU")
                }
                append(" et ")
                withStyle(SpanStyle(color = HubSecondary, textDecoration = TextDecoration.Underline)) {
                    append("Politique de confidentialité")
                }
                append(".")
            }

            Text(
                text = termsText,
                color = HubMuted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clickable { onNavigateToTerms() }
            )
        }
    }
    }
}

@Composable
private fun WelcomeFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HubSurfaceDark.copy(alpha = 0.65f))
            .border(1.dp, HubBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = HubSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = HubWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(description, color = HubMuted, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}

