package com.thehub.hb.ui.verifyemail

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubButtonVariant
import com.thehub.hb.ui.theme.HubBlack
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSuccess
import com.thehub.hb.ui.theme.HubSurfaceDark
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite

@Composable
fun VerifyEmailScreen(
    viewModel: VerifyEmailViewModel,
    onNavigateToCompleteProfile: () -> Unit,
    onNavigateToFeed: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is VerifyEmailNavigationEvent.NavigateToCompleteProfile -> onNavigateToCompleteProfile()
                is VerifyEmailNavigationEvent.NavigateToFeed -> onNavigateToFeed()
                is VerifyEmailNavigationEvent.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onSnackbarDismissed()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("verify_email_screen"),
        containerColor = HubSurfaceDark,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
                    .testTag("verify_email_snackbar")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .weight(1f, fill = false)
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Envelope icon with checkmark badge
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .testTag("verify_email_icon_container"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(HubSurfaceElevated)
                            .border(1.dp, HubBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Mail,
                            contentDescription = "Email",
                            tint = HubWhite,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Checkmark badge in bottom-right corner
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(HubSuccess)
                            .border(2.dp, HubSurfaceDark, CircleShape)
                            .testTag("verify_email_check_badge"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Email envoyé",
                            tint = HubBlack,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Email de vérification envoyé",
                    color = HubWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.testTag("verify_email_title")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Recipient confirmation card
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(HubCard)
                        .border(1.dp, HubBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("verify_email_confirmation_card")
                ) {
                    Text(
                        text = "Email de vérification envoyé à",
                        color = HubSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.email.ifBlank { "ton adresse email" },
                        color = HubWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("verify_email_recipient_text")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Clique sur le lien dans l'email pour activer ton compte.",
                        color = HubSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "N'oublie pas de vérifier ton dossier spam ou courriers indésirables si tu ne le trouves pas.",
                    color = HubSecondary.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // Error message banner with Retry button if failure occurs
                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(HubError.copy(alpha = 0.12f))
                            .border(1.dp, HubError.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                            .testTag("verify_email_error_card"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = HubError,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.errorMessage ?: "",
                                color = HubError,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }

                        if (uiState.canRetrySend) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HubButton(
                                text = "Réessayer",
                                onClick = viewModel::resendVerificationEmail,
                                isLoading = uiState.isResending,
                                variant = HubButtonVariant.Secondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                testTag = "verify_email_retry_button"
                            )
                        }
                    }
                }
            }

            // Bottom Action buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HubButton(
                    text = "J'ai vérifié",
                    onClick = viewModel::checkEmailVerification,
                    isLoading = uiState.isChecking,
                    variant = HubButtonVariant.Primary,
                    testTag = "verify_email_check_button"
                )

                val resendLabel = if (uiState.cooldownSeconds > 0) {
                    "Renvoyer l'email (${uiState.cooldownSeconds}s)"
                } else {
                    "Renvoyer l'email"
                }

                HubButton(
                    text = resendLabel,
                    onClick = viewModel::resendVerificationEmail,
                    enabled = uiState.cooldownSeconds == 0,
                    isLoading = uiState.isResending,
                    variant = HubButtonVariant.Secondary,
                    testTag = "verify_email_resend_button"
                )

                Text(
                    text = "Changer de compte / Déconnexion",
                    color = HubSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { viewModel.signOut() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("verify_email_signout_link")
                )
            }
        }
    }
}

