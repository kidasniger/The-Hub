package com.thehub.hb.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.thehub.hb.data.local.DataStoreManager
import com.thehub.hb.ui.components.AppLogo
import com.thehub.hb.ui.theme.HubBlue
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubBorderLight
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSuccess
import com.thehub.hb.ui.theme.HubSurfaceDark
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubViolet
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.HubThemeBackground
import com.thehub.hb.ui.theme.LocalHubColors
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
        OnboardingPage(
            "Bienvenue sur The Hub",
            "Un espace simple pour partager, découvrir et échanger avec ta communauté.",
            0
        ),
        OnboardingPage(
            "Découvre ce qui compte",
            "Un fil clair, des publications lisibles et les interactions essentielles à portée de main.",
            1
        ),
        OnboardingPage(
            "Partage en quelques secondes",
            "Une idée, une photo ou un moment : compose ta publication sans détour.",
            2
        ),
        OnboardingPage(
            "Ta communauté, ton rythme",
            "J’aime, commentaires, republications et conversations : tout reste à portée de main.",
            3
        )
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
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(HubWhite.copy(alpha = if (colors.isGlass) 0.10f else 0.06f))
                            .border(1.dp, HubBorderLight, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AppLogo(size = 26.dp, animated = false)
                    }
                    Text(
                        text = "THE HUB",
                        color = HubSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.2.sp
                    )
                }

                AnimatedVisibility(
                    visible = pagerState.currentPage < pages.lastIndex,
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(150))
                ) {
                    TextButton(
                        onClick = { completeOnboarding() },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Passer",
                            color = HubSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
                val alpha by animateFloatAsState(
                    targetValue = if (isCurrent) 1f else 0.55f,
                    animationSpec = tween(280),
                    label = "onboarding_alpha"
                )
                val scale by animateFloatAsState(
                    targetValue = if (isCurrent) 1f else 0.94f,
                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                    label = "onboarding_scale"
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(328.dp)
                            .graphicsLayer {
                                alpha = alpha
                                scaleX = scale
                                scaleY = scale
                            }
                    ) {
                        OnboardingIllustration(type = page.type, glass = colors.isGlass)
                    }

                    Spacer(Modifier.height(26.dp))

                    Text(
                        text = page.title,
                        color = HubWhite,
                        fontSize = 29.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 35.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = page.description,
                        color = HubSecondary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val selected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (selected) 28.dp else 7.dp,
                        animationSpec = tween(230),
                        label = "onboarding_indicator_width"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(7.dp)
                            .width(width)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) {
                                    Brush.horizontalGradient(listOf(HubViolet, HubBlue))
                                } else {
                                    null
                                } ?: Brush.linearGradient(
                                    listOf(HubMuted.copy(alpha = 0.45f), HubMuted.copy(alpha = 0.45f))
                                )
                            )
                    )
                }
            }

            val last = pagerState.currentPage == pages.lastIndex
            Button(
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
                    .height(56.dp)
                    .shadow(12.dp, RoundedCornerShape(28.dp), clip = false),
                shape = RoundedCornerShape(28.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Brush.horizontalGradient(listOf(HubViolet, HubBlue))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (last) "Commencer" else "Continuer",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingIllustration(
    type: Int,
    glass: Boolean
) {
    val frameShape = RoundedCornerShape(34.dp)
    val frameBackground = if (glass) HubWhite.copy(alpha = 0.06f) else HubSurfaceElevated
    val frameBorder = if (glass) HubWhite.copy(alpha = 0.20f) else HubBorder

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(frameShape)
            .background(frameBackground)
            .border(1.dp, frameBorder, frameShape)
            .padding(20.dp)
    ) {
        // Accent glows / depth.
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.TopStart)
                .offset(x = (-70).dp, y = (-70).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(HubViolet.copy(alpha = if (glass) 0.32f else 0.18f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 90.dp, y = 90.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(HubBlue.copy(alpha = if (glass) 0.30f else 0.14f), Color.Transparent)
                    )
                )
        )

        when (type) {
            0 -> WelcomeIllustration()
            1 -> FeedIllustration()
            2 -> ShareIllustration()
            else -> CommunityIllustration()
        }
    }
}

@Composable
private fun WelcomeIllustration() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(198.dp)
                .clip(CircleShape)
                .background(HubWhite.copy(alpha = 0.035f))
                .border(1.dp, HubBorderLight, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(HubViolet.copy(alpha = 0.20f), HubBlue.copy(alpha = 0.07f), Color.Transparent)))
                .border(1.dp, HubWhite.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(HubCard)
                    .border(1.dp, HubBorderLight, RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                AppLogo(size = 88.dp, animated = true)
            }
        }

        FloatingMiniChip(
            icon = Icons.Default.Favorite,
            text = "J'aime",
            modifier = Modifier.align(Alignment.TopEnd).offset(x = (-18).dp, y = 50.dp)
        )
        FloatingMiniChip(
            icon = Icons.Default.ChatBubble,
            text = "Échange",
            modifier = Modifier.align(Alignment.BottomStart).offset(x = 8.dp, y = (-30).dp)
        )
        FloatingMiniChip(
            icon = Icons.Default.Share,
            text = "Partage",
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-16).dp, y = (-70).dp)
        )
    }
}

@Composable
private fun FeedIllustration() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .width(238.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(HubCard)
                .border(1.dp, HubBorderLight, RoundedCornerShape(28.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarBubble(HubViolet)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("The Hub", color = HubWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("il y a 2 min", color = HubMuted, fontSize = 9.sp)
                }
                Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = HubMuted, modifier = Modifier.size(18.dp))
            }

            Text(
                "Une communauté qui tient l'essentiel dans une seule place.",
                color = HubWhite,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(HubViolet.copy(alpha = 0.65f), HubBlue.copy(alpha = 0.55f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(36.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiniMetric(Icons.Default.Favorite, "128", HubError)
                MiniMetric(Icons.Default.ChatBubble, "24", HubBlue)
                MiniMetric(Icons.Default.Share, "12", HubSuccess)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-14).dp, y = (-18).dp)
                .size(54.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(HubViolet, HubBlue)))
                .border(2.dp, Color.White.copy(alpha = 0.72f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun ShareIllustration() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .width(244.dp)
                .height(252.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(HubCard)
                .border(1.dp, HubBorderLight, RoundedCornerShape(30.dp))
                .padding(15.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarBubble(HubBlue)
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Créer une publication", color = HubWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Partager avec ta communauté", color = HubMuted, fontSize = 9.sp)
                    }
                }

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(78.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(HubSurfaceDark)
                        .border(1.dp, HubBorder, RoundedCornerShape(18.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        "Écris quelque chose de simple…",
                        color = HubMuted,
                        fontSize = 10.sp
                    )
                }

                Spacer(Modifier.height(11.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionChip(Icons.Default.Image, "Photo")
                    QuickActionChip(Icons.Default.PlayArrow, "Vidéo")
                    QuickActionChip(Icons.Default.Share, "Partager")
                }

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .width(116.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(HubViolet, HubBlue))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Publier", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 18.dp, y = 40.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(HubViolet.copy(alpha = 0.18f))
                .border(1.dp, HubViolet.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = HubWhite, modifier = Modifier.size(23.dp))
        }
    }
}

@Composable
private fun CommunityIllustration() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(116.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(HubViolet, HubBlue)))
                .border(3.dp, Color.White.copy(alpha = 0.78f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(50.dp))
        }

        CommunityNode(
            color = HubBlue,
            label = "Amis",
            modifier = Modifier.align(Alignment.TopStart).offset(x = 34.dp, y = 34.dp)
        )
        CommunityNode(
            color = HubSuccess,
            label = "Messages",
            modifier = Modifier.align(Alignment.TopEnd).offset(x = (-20).dp, y = 52.dp)
        )
        CommunityNode(
            color = HubError,
            label = "Réactions",
            modifier = Modifier.align(Alignment.BottomStart).offset(x = 44.dp, y = (-28).dp)
        )
        CommunityNode(
            color = HubViolet,
            label = "Partages",
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-24).dp, y = (-48).dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(240.dp)
                .border(1.dp, HubWhite.copy(alpha = 0.08f), CircleShape)
        )
    }
}

@Composable
private fun AvatarBubble(color: Color) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun FloatingMiniChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(HubCard)
            .border(1.dp, HubBorderLight, RoundedCornerShape(18.dp))
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = HubWhite, modifier = Modifier.size(14.dp))
        Text(text, color = HubSecondary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MiniMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Text(value, color = HubSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(HubSurfaceDark)
            .border(1.dp, HubBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 7.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = HubSecondary, modifier = Modifier.size(12.dp))
        Text(label, color = HubSecondary, fontSize = 8.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CommunityNode(
    color: Color,
    label: String,
    modifier: Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f))
                .border(1.dp, color.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ChatBubble, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(5.dp))
        Text(label, color = HubSecondary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}
