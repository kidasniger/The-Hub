package com.thehub.hb.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.R
import com.thehub.hb.ui.components.AppLogo
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubBorderLight
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubBlue
import com.thehub.hb.ui.theme.HubViolet
import com.thehub.hb.ui.theme.HubThemeBackground
import com.thehub.hb.ui.theme.LocalHubColors
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToFeed: () -> Unit,
    onNavigateToCompleteProfile: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                LoginNavigationEvent.NavigateToFeed -> onNavigateToFeed()
                LoginNavigationEvent.NavigateToCompleteProfile -> onNavigateToCompleteProfile()
            }
        }
    }


    val colors = com.thehub.hb.ui.theme.LocalHubColors.current
    HubThemeBackground(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val panelShape = RoundedCornerShape(34.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Retour conservé, mais intégré au même langage de surface que l'onboarding.
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (colors.isGlass) HubWhite.copy(alpha = 0.07f)
                        else HubSurfaceElevated
                    )
                    .border(
                        1.dp,
                        if (colors.isGlass) HubWhite.copy(alpha = 0.18f) else HubBorder,
                        CircleShape
                    )
                    .testTag("login_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Retour",
                    tint = HubWhite,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .clip(panelShape)
                    .background(
                        if (colors.isGlass) HubWhite.copy(alpha = 0.055f)
                        else HubCard
                    )
                    .border(
                        1.dp,
                        if (colors.isGlass) HubWhite.copy(alpha = 0.18f) else HubBorderLight,
                        panelShape
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                if (colors.isGlass) HubWhite.copy(alpha = 0.08f)
                                else HubSurfaceElevated
                            )
                            .border(
                                1.dp,
                                if (colors.isGlass) HubWhite.copy(alpha = 0.20f) else HubBorderLight,
                                RoundedCornerShape(26.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AppLogo(size = 64.dp, animated = true)
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "THE HUB",
                        color = HubWhite,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 5.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Bienvenue dans ton espace",
                        color = HubSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Connecte-toi pour partager, découvrir et échanger avec ta communauté.",
                        color = HubSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .testTag("login_description")
                    )

                    Spacer(Modifier.height(26.dp))

                    Button(
                        onClick = { viewModel.signInWithGoogle(context) },
                        enabled = !uiState.isGoogleLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("login_google_button"),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = Color.White.copy(alpha = 0.75f)
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(HubViolet, HubBlue)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isGoogleLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = Color.White
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_google_g),
                                    contentDescription = "Logo Google",
                                    modifier = Modifier.size(22.dp)
                                )

                                Spacer(modifier = Modifier.size(10.dp))

                                Text(
                                    text = "Continuer avec Google",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (!uiState.generalError.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(HubError.copy(alpha = 0.10f))
                                .border(
                                    1.dp,
                                    HubError.copy(alpha = 0.24f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 11.dp)
                        ) {
                            Text(
                                text = uiState.generalError.orEmpty(),
                                color = HubError,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_error_message")
                            )
                        }
                    }
                }
            }
        }
    }
}
