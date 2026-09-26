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
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubBorderLight
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubViolet
import com.thehub.hb.ui.theme.HubBlue
import com.thehub.hb.ui.theme.HubThemeBackground
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


    HubThemeBackground(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val isGlass = colors.isGlass
        val panelShape = RoundedCornerShape(40.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
        ) {
            // Même langage visuel que l'onboarding : halos de marque + surface principale.
            Box(
                modifier = Modifier
                    .size(330.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                HubViolet.copy(alpha = if (isGlass) 0.28f else 0.16f),
                                HubBlue.copy(alpha = if (isGlass) 0.18f else 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .clip(panelShape)
                    .background(
                        if (isGlass) HubWhite.copy(alpha = 0.055f) else HubCard
                    )
                    .border(
                        1.dp,
                        if (isGlass) HubWhite.copy(alpha = 0.18f) else HubBorderLight,
                        panelShape
                    )
                    .padding(horizontal = 28.dp, vertical = 34.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(126.dp)
                            .clip(RoundedCornerShape(34.dp))
                            .background(
                                if (isGlass) HubWhite.copy(alpha = 0.08f)
                                else HubSurfaceElevated
                            )
                            .border(
                                1.dp,
                                if (isGlass) HubWhite.copy(alpha = 0.20f) else HubBorderLight,
                                RoundedCornerShape(34.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AppLogo(size = 92.dp, animated = true)
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = "THE HUB",
                        color = HubWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 5.5.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Brush.horizontalGradient(listOf(HubViolet, HubBlue)))
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = when {
                            isGlass -> "CONNECTÉ. CRÉATIF. ENSEMBLE."
                            colors.isLight -> "PARTAGE. DÉCOUVRE. ÉCHANGE."
                            else -> "PARTAGE. DÉCOUVRE. ÉCHANGE."
                        },
                        color = HubSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.6.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .height(7.dp)
                        .width(28.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(HubViolet, HubBlue)))
                )
                Spacer(Modifier.width(5.dp))
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(HubMuted.copy(alpha = 0.45f))
                    )
                    if (it == 0) Spacer(Modifier.width(5.dp))
                }
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
