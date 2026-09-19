package com.thehub.hb.ui.main

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.model.User
import com.thehub.hb.data.repository.AuthRepository
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubButtonVariant
import com.thehub.hb.ui.components.UserAvatar
import com.thehub.hb.ui.createpost.CreatePostScreen
import com.thehub.hb.ui.createpost.CreatePostViewModel
import com.thehub.hb.ui.feed.FeedScreen
import com.thehub.hb.ui.feed.FeedViewModel
import com.thehub.hb.ui.feed.components.SharePostBottomSheet
import com.thehub.hb.ui.notifications.NotificationsScreen
import com.thehub.hb.ui.notifications.NotificationsViewModel
import com.thehub.hb.ui.profile.ProfileScreen
import com.thehub.hb.ui.profile.ProfileViewModel
import com.thehub.hb.ui.search.SearchScreen
import com.thehub.hb.ui.search.SearchViewModel
import com.thehub.hb.ui.theme.HubBlack
import com.thehub.hb.ui.theme.HubBlue
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubDarkGray
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubNavigationSurface
import com.thehub.hb.ui.theme.HubOutline
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceDark
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubViolet
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.hubPrimaryGradient

enum class MainTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    FEED("Feed", Icons.Filled.Home, Icons.Outlined.Home, "tab_feed"),
    SEARCH("Recherche", Icons.Filled.Search, Icons.Outlined.Search, "tab_search"),
    CREATE("Publier", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline, "tab_create"),
    NOTIFICATIONS("Notifs", Icons.Filled.Notifications, Icons.Outlined.Notifications, "tab_notifications"),
    PROFILE("Profil", Icons.Filled.Person, Icons.Outlined.Person, "tab_profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffoldScreen(
    feedViewModel: FeedViewModel,
    createPostViewModel: CreatePostViewModel,
    searchViewModel: SearchViewModel,
    notificationsViewModel: NotificationsViewModel,
    profileViewModel: ProfileViewModel,
    authRepository: AuthRepository,
    onPostClick: (String) -> Unit,
    onImageClick: (String) -> Unit,
    onOpenComments: (String) -> Unit,
    onOpenLikes: (String) -> Unit,
    onOpenMessenger: () -> Unit,
    onOpenFriends: () -> Unit = {},
    onNavigateToProfile: (String) -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFollowers: (String) -> Unit,
    onNavigateToFollowing: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToBookmarks: () -> Unit = {},
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(MainTab.FEED.ordinal) }
    val tabHistory = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf<Int>() }
    var postToShare by remember { mutableStateOf<Post?>(null) }
    var lastBackPressedTime by remember { mutableLongStateOf(0L) }
    val unreadNotificationsCount by notificationsViewModel.unreadCount.collectAsState()

    // Back button handling: pop tab history first, or exit if already on FEED
    BackHandler(enabled = true) {
        if (selectedTab != MainTab.FEED.ordinal) {
            if (tabHistory.isNotEmpty()) {
                val previousTab = tabHistory.removeAt(tabHistory.lastIndex)
                selectedTab = previousTab
            } else {
                selectedTab = MainTab.FEED.ordinal
            }
        } else {
            tabHistory.clear()
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressedTime < 2000L) {
                (context as? Activity)?.finish()
            } else {
                lastBackPressedTime = currentTime
                Toast.makeText(context, "Appuyez encore une fois pour quitter", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        containerColor = HubBlack,
        bottomBar = {
            HubBottomNavigationBar(
                selectedTab = MainTab.entries[selectedTab],
                unreadNotificationsCount = unreadNotificationsCount,
                onTabSelected = { tab ->
                    if (selectedTab != tab.ordinal) {
                        tabHistory.add(selectedTab)
                        selectedTab = tab.ordinal
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (MainTab.entries[selectedTab]) {
                MainTab.FEED -> {
                    FeedScreen(
                        viewModel = feedViewModel,
                        onPostClick = onPostClick,
                        onImageClick = onImageClick,
                        onOpenComments = onOpenComments,
                        onOpenLikes = onOpenLikes,
                        onCreatePost = {
                            createPostViewModel.reset()
                            tabHistory.add(selectedTab)
                            selectedTab = MainTab.CREATE.ordinal
                        },
                        onOpenMessenger = onOpenMessenger,
                        onOpenFriends = onOpenFriends,
                        onDiscoverUsers = {
                            tabHistory.add(selectedTab)
                            selectedTab = MainTab.SEARCH.ordinal
                        },
                        onAuthorClick = onNavigateToProfile,
                        onEditPost = { post ->
                            createPostViewModel.initForEdit(post.id, post.text)
                            tabHistory.add(selectedTab)
                            selectedTab = MainTab.CREATE.ordinal
                        }
                    )
                }

                MainTab.SEARCH -> {
                    SearchScreen(
                        viewModel = searchViewModel,
                        onPostClick = onPostClick,
                        onImageClick = onImageClick,
                        onOpenComments = onOpenComments,
                        onOpenLikes = onOpenLikes,
                        onOpenShare = { post ->
                            postToShare = post
                        },
                        onUserClick = { user ->
                            onNavigateToProfile(user.uid)
                        }
                    )
                }

                MainTab.CREATE -> {
                    CreatePostScreen(
                        viewModel = createPostViewModel,
                        onNavigateBack = {
                            createPostViewModel.reset()
                            if (tabHistory.isNotEmpty()) {
                                selectedTab = tabHistory.removeAt(tabHistory.lastIndex)
                            } else {
                                selectedTab = MainTab.FEED.ordinal
                            }
                        },
                        onPostCreated = {
                            createPostViewModel.reset()
                            selectedTab = MainTab.FEED.ordinal
                            tabHistory.clear()
                            feedViewModel.refresh()
                        }
                    )
                }

                MainTab.NOTIFICATIONS -> {
                    NotificationsScreen(
                        viewModel = notificationsViewModel,
                        onPostClick = onPostClick,
                        onUserClick = { actorId, _ ->
                            onNavigateToProfile(actorId)
                        },
                        onOpenMessenger = onOpenMessenger
                    )
                }

                MainTab.PROFILE -> {
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onNavigateBack = null,
                        onNavigateToEditProfile = onNavigateToEditProfile,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToBookmarks = onNavigateToBookmarks,
                        onNavigateToFollowers = onNavigateToFollowers,
                        onNavigateToFollowing = onNavigateToFollowing,
                        onNavigateToChat = onNavigateToChat,
                        onPostClick = onPostClick,
                        onOpenComments = onOpenComments,
                        onOpenLikes = onOpenLikes,
                        onImageClick = onImageClick,
                        isBottomTab = true
                    )
                }
            }

            postToShare?.let { post ->
                SharePostBottomSheet(
                    post = post,
                    onDismiss = { postToShare = null },
                    onRepost = { p ->
                        feedViewModel.repost(p)
                    }
                )
            }
        }
    }
}

private val TabTransitionEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

@Composable
fun HubBottomNavigationBar(
    selectedTab: MainTab,
    unreadNotificationsCount: Int = 0,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = com.thehub.hb.ui.theme.LocalHubStrings.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("hub_bottom_nav_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(44.dp),
                    ambientColor = Color.Black.copy(alpha = 0.55f),
                    spotColor = HubViolet.copy(alpha = 0.18f)
                )
                .clip(RoundedCornerShape(44.dp))
                .background(HubNavigationSurface)
                .border(
                    width = 1.dp,
                    color = HubOutline,
                    shape = RoundedCornerShape(44.dp)
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.entries.forEach { tab ->
                if (tab == MainTab.CREATE) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(
                                elevation = 18.dp,
                                shape = CircleShape,
                                ambientColor = HubViolet.copy(alpha = 0.45f),
                                spotColor = HubBlue.copy(alpha = 0.35f)
                            )
                            .clip(CircleShape)
                            .background(brush = hubPrimaryGradient())
                            .clickable { onTabSelected(MainTab.CREATE) }
                            .testTag(tab.testTag),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = strings.tabCreate,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    return@forEach
                }

                val isSelected = tab == selectedTab
                val isNotifTab = tab == MainTab.NOTIFICATIONS

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val targetIconScale = if (isPressed) 1.3f else if (isSelected) 1.15f else 1.0f
                val targetLabelScale = if (isPressed) 1.3f else 1.0f

                val iconScale by animateFloatAsState(
                    targetValue = targetIconScale,
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = TabTransitionEasing
                    ),
                    label = "tab_icon_scale_" + tab.name
                )

                val labelScale by animateFloatAsState(
                    targetValue = targetLabelScale,
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = TabTransitionEasing
                    ),
                    label = "tab_label_scale_" + tab.name
                )

                val tabTitle = when (tab) {
                    MainTab.FEED -> strings.tabFeed
                    MainTab.SEARCH -> strings.tabSearch
                    MainTab.NOTIFICATIONS -> strings.tabNotifications
                    MainTab.PROFILE -> strings.tabProfile
                    MainTab.CREATE -> strings.tabCreate
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onTabSelected(tab) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag(tab.testTag)
                ) {
                    Box(
                        modifier = Modifier.graphicsLayer(
                            scaleX = iconScale,
                            scaleY = iconScale
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isNotifTab && unreadNotificationsCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = HubWhite,
                                        contentColor = HubBlack,
                                        modifier = Modifier.testTag("notifications_unread_badge")
                                    ) {
                                        Text(
                                            text = if (unreadNotificationsCount > 99) "99+" else "" + unreadNotificationsCount,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tabTitle,
                                    tint = if (isSelected) HubWhite else HubMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tabTitle,
                                tint = if (isSelected) HubWhite else HubMuted,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = tabTitle,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) HubWhite else HubMuted,
                        modifier = Modifier.graphicsLayer(
                            scaleX = labelScale,
                            scaleY = labelScale
                        )
                    )
                }
            }
        }
    }
}
