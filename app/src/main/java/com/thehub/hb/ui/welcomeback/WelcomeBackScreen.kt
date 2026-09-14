package com.thehub.hb.ui.welcomeback

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.local.DataStoreManager
import com.thehub.hb.data.repository.AuthRepository
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubTextField
import com.thehub.hb.ui.components.UserAvatar
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceDark
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite
import kotlinx.coroutines.launch

@Composable
fun WelcomeBackScreen(
    authRepository: AuthRepository,
    dataStoreManager: DataStoreManager,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToFeed: () -> Unit,
    onNavigateToVerifyEmail: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val lastEmail by dataStoreManager.lastUserEmail.collectAsState(initial = "")
    val lastName by dataStoreManager.lastUserName.collectAsState(initial = "")
    val lastUsername by dataStoreManager.lastUsername.collectAsState(initial = "")
    val lastPhotoUrl by dataStoreManager.lastPhotoUrl.collectAsState(initial = null)

    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun handleSignIn() {
        val email = lastEmail ?: ""
        if (email.isBlank()) {
            onNavigateToLogin()
            return
        }
        if (password.isBlank()) {
            errorMessage = "Veuillez entrer votre mot de passe."
            return
        }

        isLoading = true
        errorMessage = null
        scope.launch {
            val result = authRepository.signIn(email, password)
            isLoading = false
            result.onSuccess { user ->
                if (user.isEmailVerified) {
                    onNavigateToFeed()
                } else {
                    onNavigateToVerifyEmail()
                }
            }.onFailure { error ->
                errorMessage = error.localizedMessage ?: "Mot de passe incorrect ou compte inaccessible."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HubSurfaceDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Top back button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(HubSurfaceElevated)
                .border(1.dp, HubBorder, CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Retour",
                tint = HubWhite,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UserAvatar(
                name = lastName ?: lastUsername ?: "Utilisateur",
                photoUrl = lastPhotoUrl,
                size = 80.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Bon retour, ${lastName ?: lastUsername ?: "ami"}",
                color = HubWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (!lastUsername.isNullOrBlank()) "@$lastUsername" else (lastEmail ?: ""),
                color = HubSecondary,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            HubTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                placeholder = "Mot de passe",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = HubSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (isPasswordVisible) "Masquer" else "Afficher",
                            tint = HubSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                errorMessage = errorMessage,
                testTag = "welcome_back_password_input"
            )

            HubButton(
                text = "Continuer",
                onClick = { handleSignIn() },
                isLoading = isLoading,
                enabled = password.isNotBlank(),
                testTag = "welcome_back_continue_button"
            )

            Text(
                text = "Ce n'est pas toi ? Changer de compte",
                color = HubSecondary,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onNavigateToLogin() }
                    .padding(8.dp)
            )
        }
    }
}
