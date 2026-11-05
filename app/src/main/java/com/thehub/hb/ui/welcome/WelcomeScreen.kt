package com.thehub.hb.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.ui.components.AppLogo
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubButtonVariant
import com.thehub.hb.ui.theme.HubAccent
import com.thehub.hb.ui.theme.HubBlue
import com.thehub.hb.ui.theme.HubFabGlow
import com.thehub.hb.ui.theme.HubSuccess
import com.thehub.hb.ui.theme.HubTextPrimary
import com.thehub.hb.ui.theme.HubTextSecondary
import com.thehub.hb.ui.theme.HubTextTertiary
import com.thehub.hb.ui.theme.HubViolet
import com.thehub.hb.ui.theme.HubWarning
import com.thehub.hb.ui.theme.HubThemeBackground
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.hubPrimaryGradient

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToTerms: () -> Unit
) {
    val strings = LocalHubStrings.current

    HubThemeBackground(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, top = 34.dp, bottom = 30.dp)
        ) {
            // Bloc central : logo flottant, titre, pills (03-welcome.html).
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.shadow(
                        elevation = 26.dp,
                        shape = RoundedCornerShape(88.dp / 3),
                        clip = false,
                        ambientColor = Color.Transparent,
                        // 0 26px 60px rgba(124,92,255,.55)
                        spotColor = HubFabGlow.copy(alpha = 0.55f)
                    )
                ) {
                    AppLogo(size = 88.dp, animated = true)
                }

                Spacer(Modifier.height(30.dp))

                Text(
                    text = buildAnnotatedString {
                        append("${strings.welcomeTitleTop}\n")
                        withStyle(style = SpanStyle(brush = hubPrimaryGradient())) {
                            append(strings.welcomeTitleGrad)
                        }
                    },
                    color = HubTextPrimary,
                    fontSize = 31.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 36.5.sp,
                    letterSpacing = (-0.6).sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = strings.welcomeBody,
                    color = HubTextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )

                Spacer(Modifier.height(30.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WelcomePill(
                        icon = Icons.Outlined.Shield,
                        label = strings.pillEncrypted,
                        container = Brush.linearGradient(
                            listOf(
                                HubViolet.copy(alpha = 0.22f),
                                HubBlue.copy(alpha = 0.22f)
                            )
                        ),
                        content = HubAccent,
                        outline = HubAccent.copy(alpha = 0.35f)
                    )
                    WelcomePill(
                        icon = Icons.Outlined.Group,
                        label = strings.pillCommunity,
                        container = Brush.linearGradient(
                            listOf(
                                HubSuccess.copy(alpha = 0.15f),
                                HubSuccess.copy(alpha = 0.15f)
                            )
                        ),
                        content = HubSuccess,
                        outline = HubSuccess.copy(alpha = 0.35f)
                    )
                    WelcomePill(
                        icon = Icons.Outlined.LocalFireDepartment,
                        label = strings.pillQuiet,
                        container = Brush.linearGradient(
                            listOf(
                                HubWarning.copy(alpha = 0.15f),
                                HubWarning.copy(alpha = 0.15f)
                            )
                        ),
                        content = HubWarning,
                        outline = HubWarning.copy(alpha = 0.38f)
                    )
                }
            }

            // Actions : connexion / création de compte / mentions légales.
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                HubButton(
                    text = strings.welcomeLogin,
                    onClick = onNavigateToLogin,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    },
                    modifier = Modifier.testTag("welcome_login_button")
                )

                HubButton(
                    text = strings.welcomeCreateAccount,
                    onClick = onNavigateToLogin,
                    variant = HubButtonVariant.Secondary,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = HubTextPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    },
                    modifier = Modifier.testTag("welcome_create_account_button")
                )

                // Mentions légales : gras --text-2, écran entier cliquable (CGU).
                val legalText = buildAnnotatedString {
                    append(strings.legalPrefix)
                    withStyle(
                        SpanStyle(
                            color = HubTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(strings.legalTerms)
                    }
                    append("\n${strings.legalLine2Prefix}")
                    withStyle(
                        SpanStyle(
                            color = HubTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(strings.legalPrivacy)
                    }
                }

                Text(
                    text = legalText,
                    color = HubTextTertiary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 16.8.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clickable { onNavigateToTerms() }
                )
            }
        }
    }
}

/**
 * Pilule d'argument de la maquette (.pill acc / ok / warn) :
 * 10.5/800/0.3, padding 5×11, rayon 999, icône 12, contour sémantique.
 */
@Composable
private fun WelcomePill(
    icon: ImageVector,
    label: String,
    container: Brush,
    content: Color,
    outline: Color
) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(brush = container, shape = shape)
            .border(1.dp, outline, shape)
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = label,
            color = content,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.3.sp
        )
    }
}
