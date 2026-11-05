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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.ChatBubbleOutline
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.local.DataStoreManager
import com.thehub.hb.ui.components.AppLogo
import com.thehub.hb.ui.components.GlassSurface
import com.thehub.hb.ui.components.GlassVariant
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.OfficialVerificationBadge
import com.thehub.hb.ui.theme.HubAccent
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubGlassStrong
import com.thehub.hb.ui.theme.HubInfo
import com.thehub.hb.ui.theme.HubStroke
import com.thehub.hb.ui.theme.HubSuccess
import com.thehub.hb.ui.theme.HubTextPrimary
import com.thehub.hb.ui.theme.HubTextSecondary
import com.thehub.hb.ui.theme.HubTextTertiary
import com.thehub.hb.ui.theme.HubThemeBackground
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.hubPrimaryGradient
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val titleTop: String,
    val titleGrad: String,
    val description: String
)

@Composable
fun OnboardingScreen(
    dataStoreManager: DataStoreManager,
    onFinish: () -> Unit
) {
    val strings = LocalHubStrings.current
    val scope = rememberCoroutineScope()
    // La maquette affiche 3 points : 3 pages.
    val pages = listOf(
        OnboardingPage(strings.ob1TitleTop, strings.ob1TitleGrad, strings.ob1Body),
        OnboardingPage(strings.ob2TitleTop, strings.ob2TitleGrad, strings.ob2Body),
        OnboardingPage(strings.ob3TitleTop, strings.ob3TitleGrad, strings.ob3Body)
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
        Column(modifier = Modifier.fillMaxSize()) {
            // En-tête : logo 30 px + « The Hub », action Passer (02-onboarding.html).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppLogo(size = 30.dp, animated = false)
                    Text(
                        text = buildAnnotatedString {
                            append("The ")
                            withStyle(style = SpanStyle(brush = hubPrimaryGradient())) {
                                append("Hub")
                            }
                        },
                        color = HubTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.2).sp
                    )
                }

                AnimatedVisibility(
                    visible = pagerState.currentPage < pages.lastIndex,
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(150))
                ) {
                    Text(
                        text = strings.onboardingSkip,
                        color = HubTextTertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { completeOnboarding() }
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Points de pagination : actif 30×5 dégradé, inactifs 7×5 verre bordé.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        val selected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (selected) 30.dp else 7.dp,
                            animationSpec = tween(230),
                            label = "onboarding_indicator_width"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .height(5.dp)
                                .width(width)
                                .then(onboardingDotBackground(selected))
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
                            .graphicsLayer {
                                this.alpha = visualAlpha
                                scaleX = scale
                                scaleY = scale
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Carte de démonstration (.glass, rayon 26, ombre --shadow).
                        GlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(26.dp),
                            variant = GlassVariant.Card
                        ) {
                            Column(modifier = Modifier.padding(15.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    DemoAvatar(
                                        initials = "FB",
                                        size = 38.dp,
                                        fontSize = 13.sp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = strings.demoName,
                                                color = HubTextPrimary,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            // .badge-verified { color: var(--info) }
                                            OfficialVerificationBadge(
                                                size = 12.dp,
                                                tint = HubInfo
                                            )
                                        }
                                        Text(
                                            text = "${strings.demoHandle} · ${strings.demoTime}",
                                            color = HubTextTertiary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(top = 1.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = strings.demoPostText,
                                    color = HubTextSecondary,
                                    fontSize = 11.5.sp,
                                    lineHeight = 17.5.sp,
                                    modifier = Modifier.padding(top = 10.dp)
                                )

                                Row(
                                    modifier = Modifier.padding(top = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // .pa hot / .pa cool / .pa share de la maquette.
                                    DemoAction(
                                        icon = Icons.Filled.Favorite,
                                        label = "128",
                                        color = HubError
                                    )
                                    DemoAction(
                                        icon = Icons.Outlined.ChatBubbleOutline,
                                        label = "24",
                                        color = HubInfo
                                    )
                                    DemoAction(
                                        icon = Icons.AutoMirrored.Outlined.Send,
                                        label = "12",
                                        color = HubSuccess
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Deux tuiles .glass-soft (rayon 18) de la maquette.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OnboardingTile(
                                icon = Icons.Outlined.Image,
                                title = strings.tileCreateTitle,
                                subtitle = strings.tileCreateSub,
                                modifier = Modifier.weight(1f)
                            )
                            OnboardingTile(
                                icon = Icons.Outlined.AlternateEmail,
                                title = strings.tileCommunityTitle,
                                subtitle = strings.tileCommunitySub,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        Text(
                            text = buildAnnotatedString {
                                append("${page.titleTop}\n")
                                withStyle(style = SpanStyle(brush = hubPrimaryGradient())) {
                                    append(page.titleGrad)
                                }
                            },
                            color = HubTextPrimary,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 29.5.sp,
                            letterSpacing = (-0.6).sp,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = page.description,
                            color = HubTextSecondary,
                            fontSize = 13.5.sp,
                            lineHeight = 21.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = 11.dp)
                                .fillMaxWidth()
                        )
                    }
                }

                val last = pagerState.currentPage == pages.lastIndex
                HubButton(
                    text = if (last) strings.onboardingStart else strings.onboardingContinue,
                    onClick = {
                        if (last) {
                            completeOnboarding()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    trailingIcon = if (!last) {
                        {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}

/**
 * Fond des points de pagination : dégradé de marque si actif,
 * verre --glass-strong + contour --stroke sinon (02-onboarding.html).
 */
@Composable
private fun onboardingDotBackground(selected: Boolean): Modifier {
    val shape = RoundedCornerShape(4.dp)
    return if (selected) {
        Modifier
            .clip(shape)
            .background(brush = hubPrimaryGradient(), shape = shape)
    } else {
        Modifier
            .clip(shape)
            .background(HubGlassStrong, shape)
            .border(1.dp, HubStroke, shape)
    }
}

/** Avatar de démonstration .av-g3 (#EC4899 → #F97316), initiale blanche 800. */
@Composable
private fun DemoAvatar(
    initials: String,
    size: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    listOf(Color(0xFFEC4899), Color(0xFFF97316))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/** Action de la carte démo (.pa hot / cool / share) : icône 13 + valeur 10.5/700. */
@Composable
private fun DemoAction(
    icon: ImageVector,
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            color = color,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Tuile .glass-soft : icône 16 accent, titre 11/800, sous-titre 9 --text-3. */
@Composable
private fun OnboardingTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        variant = GlassVariant.Soft
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HubAccent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                color = HubTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = subtitle,
                color = HubTextTertiary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
