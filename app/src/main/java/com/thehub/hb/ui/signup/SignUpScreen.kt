package com.thehub.hb.ui.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubButtonVariant
import com.thehub.hb.ui.components.HubTextField
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceDark
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite

@Composable
fun SignUpScreen(
    viewModel: SignUpViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToVerifyEmail: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToCompleteProfile: () -> Unit = {},
    onNavigateToFeed: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SignUpNavigationEvent.NavigateToVerifyEmail -> onNavigateToVerifyEmail()
                is SignUpNavigationEvent.NavigateToLogin -> onNavigateToLogin()
                is SignUpNavigationEvent.NavigateToTerms -> onNavigateToTerms()
                is SignUpNavigationEvent.NavigateToCompleteProfile -> onNavigateToCompleteProfile()
                is SignUpNavigationEvent.NavigateToFeed -> onNavigateToFeed()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HubSurfaceDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Back Button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(HubSurfaceElevated)
                .border(1.dp, HubBorder, CircleShape)
                .testTag("signup_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Retour",
                tint = HubWhite,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Créer ton compte",
            color = HubWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Email
            HubTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                placeholder = "Email",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Mail,
                        contentDescription = null,
                        tint = HubSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                errorMessage = uiState.emailError,
                testTag = "signup_email_input"
            )

            // Username
            HubTextField(
                value = uiState.username,
                onValueChange = viewModel::onUsernameChange,
                placeholder = "Nom d'utilisateur (ex: alex.rivera)",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.AlternateEmail,
                        contentDescription = null,
                        tint = HubSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                errorMessage = uiState.usernameError,
                testTag = "signup_username_input"
            )

            // Password
            HubTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                placeholder = "Mot de passe (8+ caractères)",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = HubSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = viewModel::togglePasswordVisibility) {
                        Icon(
                            imageVector = if (uiState.isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (uiState.isPasswordVisible) "Masquer" else "Afficher",
                            tint = HubSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                errorMessage = uiState.passwordError,
                testTag = "signup_password_input"
            )

            // Confirm Password
            HubTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                placeholder = "Confirmer le mot de passe",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = HubSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = viewModel::toggleConfirmPasswordVisibility) {
                        Icon(
                            imageVector = if (uiState.isConfirmPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (uiState.isConfirmPasswordVisible) "Masquer" else "Afficher",
                            tint = HubSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                visualTransformation = if (uiState.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                errorMessage = uiState.confirmPasswordError,
                testTag = "signup_confirm_password_input"
            )

            // Terms checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.termsAccepted,
                    onCheckedChange = viewModel::onTermsAcceptedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = HubWhite,
                        checkmarkColor = HubSurfaceDark,
                        uncheckedColor = HubSecondary
                    ),
                    modifier = Modifier.testTag("signup_terms_checkbox")
                )

                val termsText = buildAnnotatedString {
                    append("J'accepte les ")
                    withStyle(SpanStyle(color = HubWhite, textDecoration = TextDecoration.Underline)) {
                        append("CGU")
                    }
                    append(" et la ")
                    withStyle(SpanStyle(color = HubWhite, textDecoration = TextDecoration.Underline)) {
                        append("politique de confidentialité")
                    }
                }

                Text(
                    text = termsText,
                    color = HubSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier
                        .clickable { onNavigateToTerms() }
                        .padding(start = 4.dp)
                )
            }

            if (uiState.termsError != null) {
                Text(
                    text = uiState.termsError ?: "",
                    color = HubError,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            if (uiState.generalError != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(HubError.copy(alpha = 0.12f))
                        .border(1.dp, HubError.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = HubError,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.generalError ?: "",
                        color = HubError,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            HubButton(
                text = "Continuer",
                onClick = viewModel::signUp,
                isLoading = uiState.isLoading,
                modifier = Modifier.padding(top = 8.dp),
                testTag = "signup_submit_button"
            )

            // Divider "OU"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.weight(1f).height(1.dp).background(HubBorder)
                )
                Text(
                    text = "OU",
                    color = HubMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.weight(1f).height(1.dp).background(HubBorder)
                )
            }

            // Social Buttons
            HubButton(
                text = "Continuer avec Google",
                onClick = { viewModel.signInWithGoogle(context) },
                variant = HubButtonVariant.Secondary,
                isLoading = uiState.isGoogleLoading,
                testTag = "signup_google_button"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Tu as déjà un compte ? ", color = HubSecondary, fontSize = 13.sp)
                Text(
                    text = "Se connecter",
                    color = HubWhite,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable { onNavigateToLogin() }
                        .padding(4.dp)
                        .testTag("signup_go_to_login_button")
                )
            }
        }
    }
}
