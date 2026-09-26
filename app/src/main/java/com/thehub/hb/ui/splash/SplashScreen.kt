package com.thehub.hb.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.thehub.hb.data.local.DataStoreManager
import com.thehub.hb.data.repository.AuthRepository
import com.thehub.hb.ui.components.AppLogo
import com.thehub.hb.utils.SessionValidationPolicy
import com.thehub.hb.ui.theme.AppThemeMode
import com.thehub.hb.ui.theme.HubBackground
import com.thehub.hb.ui.theme.HubDarkGray
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

private const val MIN_SPLASH_DURATION_MS = 700L

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

    val colors = LocalHubColors.current
    var showBrand by remember { mutableStateOf(false) }
    val brandAlpha by animateFloatAsState(
        targetValue = if (showBrand) 1f else 0f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "splash_brand_alpha"
    )

    LaunchedEffect(Unit) {
        delay(180L)
        showBrand = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HubBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (colors.isGlass) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0x5567E8F9),
                                Color(0x223B82F6),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0x446A5CFA), Color.Transparent))
                    )
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppLogo(size = 180.dp, animated = true)

            Spacer(modifier = Modifier.height(28.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(brandAlpha)
            ) {
                Text(
                    text = "THE HUB",
                    color = HubWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 6.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(HubWhite.copy(alpha = 0.55f))
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when {
                        colors.isGlass -> "CONNECTÉ. CRÉATIF. ENSEMBLE."
                        colors.isLight -> "PARTAGE. DÉCOUVRE. ÉCHANGE."
                        else -> "PARTAGE. DÉCOUVRE. ÉCHANGE."
                    },
                    color = HubSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.8.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 34.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 26.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(HubWhite.copy(alpha = 0.85f))
            )
            repeat(2) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(HubWhite.copy(alpha = 0.18f))
                )
            }
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
