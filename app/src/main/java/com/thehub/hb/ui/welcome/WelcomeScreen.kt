package com.thehub.hb.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
import com.thehub.hb.ui.theme.HubWhite

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToTerms: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HubSurfaceDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(24.dp))

            AppLogo(size = 48.dp, animated = false)

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Bienvenue sur\nThe Hub.",
                color = HubWhite,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 40.sp,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Le réseau où le silence est une feature.",
                color = HubSecondary,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
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
                fontSize = 11.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clickable { onNavigateToTerms() }
            )
        }
    }
}
