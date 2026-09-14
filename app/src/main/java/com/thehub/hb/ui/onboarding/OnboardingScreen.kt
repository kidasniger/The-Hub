package com.thehub.hb.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubBorderLight
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubDarkGray
import com.thehub.hb.ui.theme.HubLightGray
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceDark
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val visualType: Int
)

@Composable
fun OnboardingScreen(
    dataStoreManager: DataStoreManager,
    onFinish: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pages = listOf(
        OnboardingPage(
            title = "Partage sans bruit",
            description = "Pas d'algorithme, pas de vidéos infinies. Juste tes mots et tes images.",
            visualType = 0
        ),
        OnboardingPage(
            title = "Ton cercle, tes règles",
            description = "Choisis qui voit quoi. Ton feed, ton ordre, ton calme.",
            visualType = 1
        ),
        OnboardingPage(
            title = "Texte + Image, c'est tout",
            description = "On a supprimé le reste pour te rendre l'essentiel. Minimal par design.",
            visualType = 2
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    fun completeOnboarding() {
        scope.launch {
            dataStoreManager.setOnboardingCompleted(true)
            onFinish()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HubSurfaceDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        // Page carousel
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { pageIndex ->
            val page = pages[pageIndex]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Minimal visual container
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(HubSurfaceElevated, HubSurfaceDark)
                            )
                        )
                        .border(1.dp, HubBorder, RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when (page.visualType) {
                        0 -> {
                            // Quiet visual: overlapping spheres
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(HubBorder)
                                        .border(1.dp, HubBorderLight, CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .offset(x = (-16).dp)
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(HubSurfaceElevated)
                                        .border(1.dp, HubBorderLight, CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .offset(x = (-32).dp)
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(HubDarkGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(HubWhite.copy(alpha = 0.7f))
                                    )
                                }
                            }
                        }
                        1 -> {
                            // Circle visual: concentric rings
                            Box(
                                modifier = Modifier.size(112.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(1.dp, HubBorderLight, CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .border(1.dp, HubDarkGray, CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(HubLightGray)
                                )
                            }
                        }
                        2 -> {
                            // Minimal text + image card visual
                            Column(
                                modifier = Modifier.width(96.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(HubBorder)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(76.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(HubCard)
                                        .border(1.dp, HubBorder, RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Image,
                                        contentDescription = null,
                                        tint = HubDarkGray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = page.title,
                    color = HubWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp,
                    lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = page.description,
                    color = HubSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages.size) { index ->
                val isSelected = pagerState.currentPage == index
                val width by animateDpAsState(
                    targetValue = if (isSelected) 24.dp else 6.dp,
                    label = "indicator_width"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(width)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isSelected) HubWhite else HubDarkGray)
                )
            }
        }

        // Bottom actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isLastPage = pagerState.currentPage == pages.size - 1

            HubButton(
                text = if (isLastPage) "Commencer" else "Suivant",
                onClick = {
                    if (isLastPage) {
                        completeOnboarding()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )

            if (!isLastPage) {
                HubButton(
                    text = "Passer",
                    onClick = { completeOnboarding() },
                    variant = HubButtonVariant.Secondary,
                    modifier = Modifier.width(100.dp)
                )
            }
        }
    }
}
