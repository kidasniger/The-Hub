package com.thehub.hb.ui.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.thehub.hb.BuildConfig
import com.thehub.hb.data.local.DataStoreManager
import com.thehub.hb.data.repository.AuthRepository
import com.thehub.hb.ui.components.AppLogo
import com.thehub.hb.ui.theme.AuroraGlowViolet
import com.thehub.hb.ui.theme.HubGlassSoft
import com.thehub.hb.ui.theme.HubStrokeSoft
import com.thehub.hb.ui.theme.HubGlassStrong
import com.thehub.hb.ui.theme.HubTextPrimary
import com.thehub.hb.ui.theme.HubTextTertiary
import com.thehub.hb.ui.theme.HubFabGlow
import com.thehub.hb.ui.theme.HubThemeBackground
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.hubPrimaryGradient
import com.thehub.hb.utils.SessionValidationPolicy
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

    val strings = LocalHubStrings.current
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(brandAlpha)
        ) {
            // Bloc central : logo flottant, marque, barre de chargement (01-splash.html).
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(26.dp)
                ) {
                    // .float : logo 112 px, rayon 34, anneau inset -18 px rayon 44.
                    Box(
                        modifier = Modifier.size(148.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(148.dp)
                                .clip(RoundedCornerShape(44.dp))
                                .background(HubGlassSoft)
                                .border(
                                    width = 1.5.dp,
                                    color = HubStrokeSoft,
                                    shape = RoundedCornerShape(44.dp)
                                )
                        )
                        Box(
                            modifier = Modifier.shadow(
                                elevation = 30.dp,
                                shape = RoundedCornerShape(112.dp / 3),
                                clip = false,
                                ambientColor = Color.Transparent,
                                // 0 30px 70px rgba(124,92,255,.55)
                                spotColor = HubFabGlow.copy(alpha = 0.55f)
                            )
                        ) {
                            AppLogo(size = 112.dp, animated = true)
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = strings.brandWordmark,
                            color = HubTextPrimary,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 6.5.sp,
                            // margin-left 6 px : compense le letter-spacing final.
                            modifier = Modifier.padding(start = 6.dp)
                        )

                        Spacer(Modifier.height(9.dp))

                        Text(
                            text = strings.splashTagline.uppercase(),
                            color = HubTextTertiary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 2.2.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // .progress : 150 px, hauteur 7, fond --glass-strong,
                    // contour --stroke-soft, remplissage dégradé + lueur.
                    val progressTransition = rememberInfiniteTransition(label = "splash_progress")
                    val progressFraction by progressTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 0.64f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1600, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "splash_progress_fraction"
                    )

                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(7.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(HubGlassStrong)
                            .border(1.dp, HubStrokeSoft, RoundedCornerShape(6.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .shadow(
                                    elevation = 6.dp,
                                    shape = RoundedCornerShape(6.dp),
                                    clip = false,
                                    ambientColor = Color.Transparent,
                                    // box-shadow 0 0 12px rgba(139,92,246,.7)
                                    spotColor = AuroraGlowViolet.copy(alpha = 0.7f)
                                )
                                .background(brush = hubPrimaryGradient())
                        )
                    }
                }
            }

            // Pied : version courante, comme « v1.0.173 · build 173 · Aurora Glass ».
            Text(
                text = strings.splashVersionLine.format(
                    BuildConfig.VERSION_NAME,
                    "${BuildConfig.VERSION_CODE}"
                ),
                color = HubTextTertiary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 34.dp)
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
