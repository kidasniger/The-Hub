package com.thehub.hb.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.thehub.hb.data.local.DataStoreManager
import com.thehub.hb.data.repository.AuthRepository
import com.thehub.hb.ui.components.AppLogo
import com.thehub.hb.utils.SessionValidationPolicy
import com.thehub.hb.ui.theme.HubDarkGray
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubBackground
import com.thehub.hb.ui.theme.HubWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

private const val MIN_SPLASH_DURATION_MS = 450L

@Composable
fun SplashScreen(
    authRepository: AuthRepository,
    dataStoreManager: DataStoreManager,
    onNavigateToFeed: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToWelcome: () -> Unit,
    isOnline: Boolean = true
) {
    LaunchedEffect(isOnline) {
        if (!isOnline) return@LaunchedEffect
        val startTime = System.currentTimeMillis()
        val currentUser = authRepository.currentFirebaseUser
        var isSessionValid = false

        if (currentUser != null) {
            try {
                // Validate the persisted Firebase session before entering the app.
                currentUser.reload().await()
                if (currentUser.isEmailVerified) {
                    val userDoc = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(currentUser.uid)
                        .get()
                        .await()

                    if (userDoc.exists()
                        && userDoc.getBoolean("isDeleted") != true
                        && userDoc.getBoolean("isSuspended") != true) {
                        isSessionValid = true
                    } else {
                        authRepository.signOut()
                        dataStoreManager.clearAll()
                    }
                }
            } catch (e: Exception) {
                val isTransient = isTransientNetworkFailure(e)
                if (SessionValidationPolicy.shouldClearSession(isOnline, isTransient)) {
                    authRepository.signOut()
                    dataStoreManager.clearAll()
                }
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed < MIN_SPLASH_DURATION_MS) {
            delay(MIN_SPLASH_DURATION_MS - elapsed)
        }

        if (!isOnline) return@LaunchedEffect

        if (isSessionValid) {
            onNavigateToFeed()
        } else {
            val isOnboardingCompleted = dataStoreManager.isOnboardingCompleted.first()
            if (!isOnboardingCompleted) {
                onNavigateToOnboarding()
            } else {
                onNavigateToWelcome()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HubBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppLogo(
                size = 180.dp,
                animated = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "THE HUB",
                color = HubWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 6.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(1.dp)
                    .background(HubDarkGray)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "TEXTE + IMAGE. C'EST TOUT.",
                color = HubSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 24.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(HubWhite.copy(alpha = 0.8f))
            )
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(HubWhite.copy(alpha = 0.2f))
            )
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(HubWhite.copy(alpha = 0.2f))
            )
        }
    }
}

private fun isTransientNetworkFailure(error: Exception): Boolean {
    if (error is FirebaseFirestoreException) {
        return error.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
            error.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
    }

    var current: Throwable? = error
    repeat(3) {
        if (current is java.io.IOException) return true
        if (current?.message?.contains("network", ignoreCase = true) == true) return true
        if (current?.message?.contains("offline", ignoreCase = true) == true) return true
        current = current?.cause
    }
    return false
}

