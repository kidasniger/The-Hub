package com.thehub.hb.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.local.DataStoreManager
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubButtonVariant
import com.thehub.hb.ui.theme.HubBlack
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubBorderLight
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceDark
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubColors
import com.thehub.hb.ui.theme.HubThemeBackground
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val description: String,
    val type: Int
)

@Composable
fun OnboardingScreen(
    dataStoreManager: DataStoreManager,
    onFinish: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val colors = LocalHubColors.current
    val pages = listOf(
        OnboardingPage("Bienvenue sur The Hub", "Un espace simple pour partager, découvrir et échanger avec ta communauté.", 0),
        OnboardingPage("Découvre ce qui compte", "Photos, textes et discussions dans une interface pensée pour aller à l’essentiel.", 1),
        OnboardingPage("Partage en quelques secondes", "Publie une idée, une photo ou un moment avec les personnes qui comptent.", 2),
        OnboardingPage("Ta communauté, ton rythme", "J’aime, commentaires, republications et conversations : tout reste à portée de main.", 3)
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })

    fun completeOnboarding() {
        scope.launch {
            dataStoreManager.setOnboardingCompleted(true)
            onFinish()
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
                .padding(horizontal = 22.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(HubWhite)
                    )
                    Text(
                        text = "THE HUB",
                        color = HubSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                AnimatedVisibility(
                    visible = pagerState.currentPage < pages.lastIndex,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    HubButton(
                        text = "Passer",
                        onClick = { completeOnboarding() },
                        variant = HubButtonVariant.Secondary,
                        modifier = Modifier.width(82.dp)
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { index ->
                val page = pages[index]
                val isCurrent = pagerState.currentPage == index
                val visualAlpha by animateFloatAsState(
                    targetValue = if (isCurrent) 1f else 0.78f,
                    animationSpec = androidx.compose.animation.core.tween(350),
                    label = "onboarding_visual_alpha"
                )
                val visualScale by animateFloatAsState(
                    targetValue = if (isCurrent) 1f else 0.93f,
                    animationSpec = androidx.compose.animation.core.tween(350, easing = FastOutSlowInEasing),
                    label = "onboarding_visual_scale"
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .offset(y = (-12).dp)
                            .alpha(visualAlpha)
                    ) {
                        OnboardingVisual(type = page.type, scale = visualScale, glass = colors.isGlass)
                    }

                    Spacer(Modifier.height(26.dp))

                    Text(
                        text = page.title,
                        color = HubWhite,
                        fontSize = 29.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 35.sp,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = page.description,
                        color = HubSecondary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { index ->
                    val selected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (selected) 24.dp else 6.dp,
                        animationSpec = androidx.compose.animation.core.tween(250),
                        label = "onboarding_indicator"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(width)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (selected) HubWhite else HubMuted.copy(alpha = 0.5f))
                    )
                }
            }

            val last = pagerState.currentPage == pages.lastIndex
            HubButton(
                text = if (last) "Commencer 🚀" else "Continuer",
                onClick = {
                    if (last) {
                        completeOnboarding()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            )
        }
    }
}

@Composable
private fun OnboardingVisual(type: Int, scale: Float, glass: Boolean) {
    val surface = if (glass) HubCard else HubSurfaceElevated
    Box(
        modifier = Modifier
            .fillMaxSize()
            .scaleSafe(scale)
            .clip(RoundedCornerShape(38.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(surface, HubSurfaceDark)
                )
            )
            .border(1.dp, HubBorderLight, RoundedCornerShape(38.dp)),
        contentAlignment = Alignment.Center
    ) {
        when (type) {
            0 -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(HubWhite.copy(alpha = 0.08f))
                            .border(1.dp, HubBorderLight, CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(HubWhite.copy(alpha = 0.12f))
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Ton espace.", color = HubWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("Ton rythme.", color = HubSecondary, fontSize = 13.sp)
                }
            }
            1 -> {
                Column(
                    modifier = Modifier.width(155.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(2) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(HubCard)
                                .border(1.dp, HubBorder, RoundedCornerShape(18.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(HubWhite.copy(alpha = 0.13f))
                            )
                            Column(Modifier.weight(1f)) {
                                Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(HubWhite.copy(alpha = 0.18f)))
                                Spacer(Modifier.height(6.dp))
                                Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(HubMuted.copy(alpha = 0.35f)))
                            }
                        }
                    }
                    Icon(Icons.Default.Image, contentDescription = null, tint = HubWhite, modifier = Modifier.size(30.dp).align(Alignment.CenterHorizontally))
                }
            }
            2 -> {
                Box(
                    modifier = Modifier
                        .width(156.dp)
                        .height(178.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(HubCard)
                        .border(1.dp, HubBorder, RoundedCornerShape(24.dp))
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = HubWhite,
                        modifier = Modifier
                            .size(62.dp)
                            .align(Alignment.Center)
                    )
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = HubSecondary,
                        modifier = Modifier
                            .size(26.dp)
                            .align(Alignment.BottomEnd)
                            .padding(end = 22.dp, bottom = 20.dp)
                    )
                }
            }
            else -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        Icons.Default.Favorite to "J'aime",
                        Icons.Default.People to "Amis",
                        Icons.Default.Share to "Partager"
                    ).forEach { (icon, label) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(HubWhite.copy(alpha = 0.08f))
                                    .border(1.dp, HubBorderLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = HubWhite, modifier = Modifier.size(22.dp))
                            }
                            Text(label, color = HubSecondary, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.scaleSafe(value: Float): Modifier = this.scale(value)
