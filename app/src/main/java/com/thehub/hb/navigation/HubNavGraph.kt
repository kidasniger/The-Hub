package com.thehub.hb.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.thehub.hb.di.AppContainer
import com.thehub.hb.ui.bookmarks.BookmarksScreen
import com.thehub.hb.ui.bookmarks.BookmarksViewModel
import com.thehub.hb.ui.comments.CommentsScreen
import com.thehub.hb.ui.comments.CommentsViewModel
import com.thehub.hb.ui.completeprofile.CompleteProfileScreen
import com.thehub.hb.ui.completeprofile.CompleteProfileViewModel
import com.thehub.hb.ui.confirmpassword.ConfirmPasswordScreen
import com.thehub.hb.ui.confirmpassword.ConfirmPasswordViewModel
import com.thehub.hb.ui.createpost.CreatePostScreen
import com.thehub.hb.ui.createpost.CreatePostViewModel
import com.thehub.hb.ui.feed.FeedViewModel
import com.thehub.hb.ui.imageviewer.ImageViewerScreen
import com.thehub.hb.ui.likeslist.LikesListScreen
import com.thehub.hb.ui.likeslist.LikesListViewModel
import com.thehub.hb.ui.login.LoginScreen
import com.thehub.hb.ui.login.LoginViewModel
import com.thehub.hb.ui.main.MainScaffoldScreen
import com.thehub.hb.ui.messenger.MessengerScreen
import com.thehub.hb.ui.messenger.MessengerViewModel
import com.thehub.hb.ui.messenger.chat.ChatScreen
import com.thehub.hb.ui.messenger.chat.ChatViewModel
import com.thehub.hb.ui.messenger.chatinfo.ChatInfoScreen
import com.thehub.hb.ui.messenger.chatinfo.ChatInfoViewModel
import com.thehub.hb.ui.messenger.newmessage.NewMessageScreen
import com.thehub.hb.ui.messenger.newmessage.NewMessageViewModel
import com.thehub.hb.ui.notifications.NotificationsViewModel
import com.thehub.hb.ui.onboarding.OnboardingScreen
import com.thehub.hb.ui.postdetail.PostDetailScreen
import com.thehub.hb.ui.postdetail.PostDetailViewModel
import com.thehub.hb.ui.profile.EditProfileScreen
import com.thehub.hb.ui.profile.EditProfileViewModel
import com.thehub.hb.ui.profile.FollowListScreen
import com.thehub.hb.ui.profile.FollowListType
import com.thehub.hb.ui.profile.FollowListViewModel
import com.thehub.hb.ui.profile.ProfileScreen
import com.thehub.hb.ui.profile.ProfileViewModel
import com.thehub.hb.ui.resetpassword.ResetPasswordScreen
import com.thehub.hb.ui.resetpassword.ResetPasswordViewModel
import com.thehub.hb.ui.search.SearchViewModel
import com.thehub.hb.ui.settings.BlockedUsersScreen
import com.thehub.hb.ui.settings.BlockedUsersViewModel
import com.thehub.hb.ui.settings.SettingsScreen
import com.thehub.hb.ui.settings.SettingsViewModel
import com.thehub.hb.ui.signup.SignUpScreen
import com.thehub.hb.ui.signup.SignUpViewModel
import com.thehub.hb.ui.splash.SplashScreen
import com.thehub.hb.ui.terms.TermsScreen
import com.thehub.hb.ui.verifyemail.VerifyEmailScreen
import com.thehub.hb.ui.verifyemail.VerifyEmailViewModel
import com.thehub.hb.ui.welcome.WelcomeScreen
import com.thehub.hb.ui.welcomeback.WelcomeBackScreen

@Composable
fun HubNavGraph(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController()
) {
    val authRepository = appContainer.authRepository
    val dataStoreManager = appContainer.dataStoreManager
    val lastUserEmail by dataStoreManager.lastUserEmail.collectAsState(initial = "")

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Splash
        composable(Screen.Splash.route) {
            SplashScreen(
                authRepository = authRepository,
                dataStoreManager = dataStoreManager,
                onNavigateToFeed = {
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToWelcome = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToWelcomeBack = {
                    navController.navigate(Screen.WelcomeBack.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Onboarding
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                dataStoreManager = dataStoreManager,
                onFinish = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // Welcome
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                onNavigateToTerms = { navController.navigate(Screen.Terms.route) }
            )
        }

        // Welcome Back
        composable(Screen.WelcomeBack.route) {
            WelcomeBackScreen(
                authRepository = authRepository,
                dataStoreManager = dataStoreManager,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                onNavigateToFeed = {
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(Screen.WelcomeBack.route) { inclusive = true }
                    }
                },
                onNavigateToVerifyEmail = {
                    navController.navigate(Screen.VerifyEmail.route)
                }
            )
        }

        // Login
        composable(Screen.Login.route) {
            val loginViewModel: LoginViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return LoginViewModel(authRepository, lastUserEmail ?: "") as T
                    }
                }
            )

            LoginScreen(
                viewModel = loginViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFeed = {
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToVerifyEmail = {
                    navController.navigate(Screen.VerifyEmail.route)
                },
                onNavigateToResetPassword = {
                    navController.navigate(Screen.ResetPassword.route)
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                }
            )
        }

        // Sign Up
        composable(Screen.SignUp.route) {
            val signUpViewModel: SignUpViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return SignUpViewModel(authRepository) as T
                    }
                }
            )

            SignUpScreen(
                viewModel = signUpViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVerifyEmail = {
                    navController.navigate(Screen.VerifyEmail.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onNavigateToTerms = {
                    navController.navigate(Screen.Terms.route)
                }
            )
        }

        // Verify Email
        composable(Screen.VerifyEmail.route) {
            val verifyEmailViewModel: VerifyEmailViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return VerifyEmailViewModel(authRepository) as T
                    }
                }
            )

            VerifyEmailScreen(
                viewModel = verifyEmailViewModel,
                onNavigateToCompleteProfile = {
                    navController.navigate(Screen.CompleteProfile.route) {
                        popUpTo(Screen.VerifyEmail.route) { inclusive = true }
                    }
                },
                onNavigateToFeed = {
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(Screen.VerifyEmail.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.VerifyEmail.route) { inclusive = true }
                    }
                }
            )
        }

        // Reset Password
        composable(Screen.ResetPassword.route) {
            val resetPasswordViewModel: ResetPasswordViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ResetPasswordViewModel(authRepository) as T
                    }
                }
            )

            ResetPasswordScreen(
                viewModel = resetPasswordViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToConfirmPassword = { oobCode ->
                    navController.navigate(Screen.ConfirmPassword.createRoute(oobCode))
                }
            )
        }

        // Confirm Password
        composable(
            route = Screen.ConfirmPassword.route,
            arguments = listOf(
                navArgument("oobCode") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val code = backStackEntry.arguments?.getString("oobCode") ?: ""
            val confirmPasswordViewModel: ConfirmPasswordViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ConfirmPasswordViewModel(authRepository, code) as T
                    }
                }
            )

            ConfirmPasswordScreen(
                viewModel = confirmPasswordViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.ResetPassword.route) { inclusive = true }
                    }
                }
            )
        }

        // Complete Profile
        composable(Screen.CompleteProfile.route) {
            val completeProfileViewModel: CompleteProfileViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return CompleteProfileViewModel(authRepository) as T
                    }
                }
            )

            CompleteProfileScreen(
                viewModel = completeProfileViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFeed = {
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(Screen.CompleteProfile.route) { inclusive = true }
                    }
                }
            )
        }

        // Main Hub (Feed with persistent bottom navigation)
        composable(Screen.Feed.route) {
            val feedViewModel: FeedViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return FeedViewModel(appContainer.postRepository, appContainer.messageRepository) as T
                    }
                }
            )

            val createPostViewModel: CreatePostViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return CreatePostViewModel(appContainer.postRepository) as T
                    }
                }
            )

            val searchViewModel: SearchViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return SearchViewModel(
                            searchRepository = appContainer.searchRepository,
                            postRepository = appContainer.postRepository,
                            notificationRepository = appContainer.notificationRepository
                        ) as T
                    }
                }
            )

            val notificationsViewModel: NotificationsViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return NotificationsViewModel(
                            notificationRepository = appContainer.notificationRepository
                        ) as T
                    }
                }
            )

            val profileViewModel: ProfileViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ProfileViewModel(
                            targetUserId = null,
                            userRepository = appContainer.userRepository,
                            messageRepository = appContainer.messageRepository,
                            postRepository = appContainer.postRepository
                        ) as T
                    }
                }
            )

            MainScaffoldScreen(
                feedViewModel = feedViewModel,
                createPostViewModel = createPostViewModel,
                searchViewModel = searchViewModel,
                notificationsViewModel = notificationsViewModel,
                profileViewModel = profileViewModel,
                authRepository = authRepository,
                onPostClick = { postId ->
                    navController.navigate(Screen.PostDetail.createRoute(postId))
                },
                onImageClick = { imageUrl ->
                    navController.navigate(Screen.ImageViewer.createRoute(imageUrl))
                },
                onOpenComments = { postId ->
                    navController.navigate(Screen.Comments.createRoute(postId))
                },
                onOpenLikes = { postId ->
                    navController.navigate(Screen.LikesList.createRoute(postId))
                },
                onOpenMessenger = {
                    navController.navigate(Screen.Messenger.route)
                },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.Profile.createRoute(userId))
                },
                onNavigateToEditProfile = {
                    navController.navigate(Screen.EditProfile.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToFollowers = { userId ->
                    navController.navigate(Screen.FollowersList.createRoute(userId))
                },
                onNavigateToFollowing = { userId ->
                    navController.navigate(Screen.FollowingList.createRoute(userId))
                },
                onNavigateToChat = {
                    navController.navigate(Screen.Messenger.route)
                },
                onNavigateToBookmarks = {
                    navController.navigate(Screen.Bookmarks.route)
                },
                onSignOut = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Create Post (standalone destination if opened from notification or deep link)
        composable(Screen.CreatePost.route) {
            val createPostViewModel: CreatePostViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return CreatePostViewModel(appContainer.postRepository) as T
                    }
                }
            )

            CreatePostScreen(
                viewModel = createPostViewModel,
                onNavigateBack = { navController.popBackStack() },
                onPostCreated = {
                    navController.popBackStack()
                }
            )
        }

        // Post Detail
        composable(
            route = Screen.PostDetail.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            val postDetailViewModel: PostDetailViewModel = viewModel(
                key = "post_detail_$postId",
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return PostDetailViewModel(postId, appContainer.postRepository) as T
                    }
                }
            )

            PostDetailScreen(
                viewModel = postDetailViewModel,
                onNavigateBack = { navController.popBackStack() },
                onImageClick = { imageUrl ->
                    navController.navigate(Screen.ImageViewer.createRoute(imageUrl))
                },
                onOpenComments = { pid ->
                    navController.navigate(Screen.Comments.createRoute(pid))
                },
                onOpenLikes = { pid ->
                    navController.navigate(Screen.LikesList.createRoute(pid))
                },
                onAuthorClick = { authorId ->
                    navController.navigate(Screen.Profile.createRoute(authorId))
                }
            )
        }

        // Comments
        composable(
            route = Screen.Comments.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            val commentsViewModel: CommentsViewModel = viewModel(
                key = "comments_$postId",
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return CommentsViewModel(postId, appContainer.postRepository, appContainer.authRepository) as T
                    }
                }
            )

            CommentsScreen(
                viewModel = commentsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onUserClick = { authorId ->
                    navController.navigate(Screen.Profile.createRoute(authorId))
                }
            )
        }

        // Likes List
        composable(
            route = Screen.LikesList.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            val likesListViewModel: LikesListViewModel = viewModel(
                key = "likes_$postId",
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return LikesListViewModel(postId, appContainer.postRepository) as T
                    }
                }
            )

            LikesListScreen(
                viewModel = likesListViewModel,
                onNavigateBack = { navController.popBackStack() },
                onUserClick = { uid ->
                    navController.navigate(Screen.Profile.createRoute(uid))
                }
            )
        }

        // Image Viewer
        composable(
            route = Screen.ImageViewer.route,
            arguments = listOf(navArgument("imageUrl") { type = NavType.StringType })
        ) { backStackEntry ->
            val rawImageUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""
            val decodedUrl = try {
                java.net.URLDecoder.decode(rawImageUrl, "UTF-8")
            } catch (_: Exception) {
                rawImageUrl
            }

            ImageViewerScreen(
                imageUrl = decodedUrl,
                onClose = { navController.popBackStack() }
            )
        }

        // Terms
        composable(Screen.Terms.route) {
            TermsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Messenger
        composable(Screen.Messenger.route) {
            val messengerViewModel: MessengerViewModel = viewModel(
                factory = MessengerViewModel.Factory(appContainer.messageRepository)
            )

            MessengerScreen(
                viewModel = messengerViewModel,
                onBack = { navController.popBackStack() },
                onConversationClick = { convId ->
                    navController.navigate(Screen.Chat.createRoute(convId))
                },
                onNewMessageClick = {
                    navController.navigate(Screen.NewMessage.route)
                }
            )
        }

        // New Message
        composable(Screen.NewMessage.route) {
            val newMessageViewModel: NewMessageViewModel = viewModel(
                factory = NewMessageViewModel.Factory(appContainer.messageRepository)
            )

            NewMessageScreen(
                viewModel = newMessageViewModel,
                onBack = { navController.popBackStack() },
                onConversationCreated = { convId ->
                    navController.navigate(Screen.Chat.createRoute(convId)) {
                        popUpTo(Screen.Messenger.route)
                    }
                }
            )
        }

        // Chat
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            val chatViewModel: ChatViewModel = viewModel(
                key = "chat_$conversationId",
                factory = ChatViewModel.Factory(conversationId, appContainer.messageRepository)
            )

            ChatScreen(
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() },
                onChatInfoClick = { convId ->
                    navController.navigate(Screen.ChatInfo.createRoute(convId))
                },
                onImageClick = { imageUrl ->
                    navController.navigate(Screen.ImageViewer.createRoute(imageUrl))
                }
            )
        }

        // Chat Info
        composable(
            route = Screen.ChatInfo.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            val chatInfoViewModel: ChatInfoViewModel = viewModel(
                key = "chat_info_$conversationId",
                factory = ChatInfoViewModel.Factory(conversationId, appContainer.messageRepository)
            )

            ChatInfoScreen(
                viewModel = chatInfoViewModel,
                onBack = { navController.popBackStack() },
                onConversationDeleted = {
                    navController.popBackStack(Screen.Messenger.route, inclusive = false)
                }
            )
        }

        // Profile (user profile or own profile viewed from another screen)
        composable(
            route = Screen.Profile.route,
            arguments = listOf(
                navArgument("userId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            val profileViewModel: ProfileViewModel = viewModel(
                key = "profile_${userId ?: "current"}",
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ProfileViewModel(
                            targetUserId = userId,
                            userRepository = appContainer.userRepository,
                            messageRepository = appContainer.messageRepository,
                            postRepository = appContainer.postRepository
                        ) as T
                    }
                }
            )

            ProfileScreen(
                viewModel = profileViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditProfile = {
                    navController.navigate(Screen.EditProfile.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToFollowers = { uid ->
                    navController.navigate(Screen.FollowersList.createRoute(uid))
                },
                onNavigateToFollowing = { uid ->
                    navController.navigate(Screen.FollowingList.createRoute(uid))
                },
                onNavigateToChat = { uid ->
                    navController.navigate(Screen.Messenger.route)
                },
                onNavigateToBookmarks = {
                    navController.navigate(Screen.Bookmarks.route)
                },
                onPostClick = { postId ->
                    navController.navigate(Screen.PostDetail.createRoute(postId))
                },
                onOpenComments = { postId ->
                    navController.navigate(Screen.Comments.createRoute(postId))
                },
                onOpenLikes = { postId ->
                    navController.navigate(Screen.LikesList.createRoute(postId))
                },
                onImageClick = { imageUrl ->
                    navController.navigate(Screen.ImageViewer.createRoute(imageUrl))
                },
                isBottomTab = false
            )
        }

        // Edit Profile
        composable(Screen.EditProfile.route) {
            val editProfileViewModel: EditProfileViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return EditProfileViewModel(
                            userRepository = appContainer.userRepository
                        ) as T
                    }
                }
            )

            EditProfileScreen(
                viewModel = editProfileViewModel,
                onNavigateBack = { navController.popBackStack() },
                onProfileUpdated = { navController.popBackStack() }
            )
        }

        // Followers List
        composable(
            route = Screen.FollowersList.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val targetUserId = backStackEntry.arguments?.getString("userId") ?: ""
            val followListViewModel: FollowListViewModel = viewModel(
                key = "followers_$targetUserId",
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return FollowListViewModel(
                            targetUserId = targetUserId,
                            type = FollowListType.FOLLOWERS,
                            userRepository = appContainer.userRepository
                        ) as T
                    }
                }
            )

            FollowListScreen(
                viewModel = followListViewModel,
                onNavigateBack = { navController.popBackStack() },
                onUserClick = { uid ->
                    navController.navigate(Screen.Profile.createRoute(uid))
                }
            )
        }

        // Following List
        composable(
            route = Screen.FollowingList.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val targetUserId = backStackEntry.arguments?.getString("userId") ?: ""
            val followListViewModel: FollowListViewModel = viewModel(
                key = "following_$targetUserId",
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return FollowListViewModel(
                            targetUserId = targetUserId,
                            type = FollowListType.FOLLOWING,
                            userRepository = appContainer.userRepository
                        ) as T
                    }
                }
            )

            FollowListScreen(
                viewModel = followListViewModel,
                onNavigateBack = { navController.popBackStack() },
                onUserClick = { uid ->
                    navController.navigate(Screen.Profile.createRoute(uid))
                }
            )
        }

        // Settings
        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return SettingsViewModel(
                            authRepository = appContainer.authRepository,
                            userRepository = appContainer.userRepository,
                            dataStoreManager = appContainer.dataStoreManager
                        ) as T
                    }
                }
            )

            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToResetPassword = {
                    navController.navigate(Screen.ResetPassword.route)
                },
                onNavigateToBlockedUsers = {
                    navController.navigate(Screen.BlockedUsers.route)
                },
                onNavigateToTerms = {
                    navController.navigate(Screen.Terms.route)
                },
                onSignedOut = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onAccountDeleted = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Blocked Users
        composable(Screen.BlockedUsers.route) {
            val blockedUsersViewModel: BlockedUsersViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return BlockedUsersViewModel(
                            userRepository = appContainer.userRepository
                        ) as T
                    }
                }
            )

            BlockedUsersScreen(
                viewModel = blockedUsersViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Bookmarks
        composable(Screen.Bookmarks.route) {
            val bookmarksViewModel: BookmarksViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return BookmarksViewModel(
                            postRepository = appContainer.postRepository
                        ) as T
                    }
                }
            )

            BookmarksScreen(
                viewModel = bookmarksViewModel,
                onNavigateBack = { navController.popBackStack() },
                onPostClick = { postId ->
                    navController.navigate(Screen.PostDetail.createRoute(postId))
                },
                onImageClick = { imageUrl ->
                    navController.navigate(Screen.ImageViewer.createRoute(imageUrl))
                },
                onOpenComments = { postId ->
                    navController.navigate(Screen.Comments.createRoute(postId))
                },
                onOpenLikes = { postId ->
                    navController.navigate(Screen.LikesList.createRoute(postId))
                },
                onAuthorClick = { userId ->
                    navController.navigate(Screen.Profile.createRoute(userId))
                }
            )
        }
    }
}
