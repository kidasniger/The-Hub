package com.thehub.hb.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.R
import com.thehub.hb.ui.components.AppLogo
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubButtonVariant
import com.thehub.hb.ui.components.HubErrorBanner
import com.thehub.hb.ui.theme.HubHighlight
import com.thehub.hb.ui.theme.HubStrokeSoft
import com.thehub.hb.ui.theme.HubTextPrimary
import com.thehub.hb.ui.theme.HubTextSecondary
import com.thehub.hb.ui.theme.HubTextTertiary
import com.thehub.hb.ui.theme.HubThemeBackground
import com.thehub.hb.ui.theme.LocalHubColors
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.hubPrimaryGradient

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToFeed: () -> Unit,
    onNavigateToCompleteProfile: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val strings = LocalHubStrings.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                LoginNavigationEvent.NavigateToFeed -> onNavigateToFeed()
                LoginNavigationEvent.NavigateToCompleteProfile -> onNavigateToCompleteProfile()
            }
        }
    }

    HubThemeBackground(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            // Appbar : iconbtn verre 40/14 (04-login.html).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 12.dp)
            ) {
                HubGlassIconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("login_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = strings.back,
                        tint = HubTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Column {
                    // Marque 40 px (logo .mark statique).
                    AppLogo(size = 40.dp, animated = false)

                    Text(
                        text = buildAnnotatedString {
                            append("${strings.loginTitleTop}\n")
                            // La maquette applique .grad-text sur 👋 ; le pinceau
                            // n'affecte pas l'emoji, le rendu reste identique.
                            withStyle(style = SpanStyle(brush = hubPrimaryGradient())) {
                                append(strings.loginTitleGrad)
                            }
                        },
                        color = HubTextPrimary,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 31.9.sp,
                        letterSpacing = (-0.6).sp,
                        modifier = Modifier.padding(top = 18.dp)
                    )

                    Text(
                        text = strings.loginBody,
                        color = HubTextSecondary,
                        fontSize = 13.5.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier
                            .padding(top = 9.dp)
                            .testTag("login_description")
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    // Bouton verre Google de la maquette (.btn-glass + icône G).
                    HubButton(
                        text = strings.continueWithGoogle,
                        onClick = { viewModel.signInWithGoogle(context) },
                        variant = HubButtonVariant.Secondary,
                        enabled = !uiState.isGoogleLoading,
                        isLoading = uiState.isGoogleLoading,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_google_g),
                                contentDescription = "Logo Google",
                                modifier = Modifier.size(17.dp)
                            )
                        },
                        modifier = Modifier.testTag("login_google_button")
                    )

                    // État « erreur » de la maquette : bannière .banner.danger.
                    if (!uiState.generalError.isNullOrBlank()) {
                        HubErrorBanner(
                            message = uiState.generalError.orEmpty(),
                            modifier = Modifier.testTag("login_error_message")
                        )
                    }

                    // Séparateur « ou » de la maquette (.divider + .small).
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(HubStrokeSoft)
                        )
                        Text(
                            text = strings.loginOr,
                            color = HubTextTertiary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(HubStrokeSoft)
                        )
                    }

                    // Inscription : même flux Google (le compte est créé à la
                    // première connexion), comme le pied de la maquette.
                    val footerText = buildAnnotatedString {
                        append(strings.loginNoAccount)
                        withStyle(
                            SpanStyle(
                                brush = hubPrimaryGradient(),
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(strings.loginSignUp)
                        }
                    }

                    Text(
                        text = footerText,
                        color = HubTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clickable {
                                if (!uiState.isGoogleLoading) {
                                    viewModel.signInWithGoogle(context)
                                }
                            }
                            .testTag("login_sign_up_footer")
                    )
                }
            }
        }
    }
}

/**
 * Iconbtn de la maquette (.iconbtn) : 40 dp, rayon --r-sm 14, verre --glass,
 * contour --stroke, ombre --shadow-sm et reflet interne haut 1 px.
 */
@Composable
private fun HubGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val highlight = HubHighlight
    val palette = LocalHubColors.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(40.dp)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                clip = false,
                ambientColor = palette.shadowColor,
                spotColor = palette.shadowColor
            )
            .clip(shape)
            .background(palette.glass)
            .drawBehind {
                // inset 0 1px 0 var(--hl)
                drawRect(
                    color = highlight,
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, 1.dp.toPx())
                )
            }
            .border(1.dp, palette.stroke, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
