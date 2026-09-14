package com.thehub.hb.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Welcome : Screen("welcome")
    data object WelcomeBack : Screen("welcome_back")
    data object Login : Screen("login")
    data object SignUp : Screen("signup")
    data object VerifyEmail : Screen("verify_email")
    data object ResetPassword : Screen("reset_password")
    data object ConfirmPassword : Screen("confirm_password?oobCode={oobCode}") {
        fun createRoute(oobCode: String = ""): String = "confirm_password?oobCode=$oobCode"
    }
    data object CompleteProfile : Screen("complete_profile")
    data object Feed : Screen("feed")
    data object CreatePost : Screen("create_post")
    data object PostDetail : Screen("post_detail/{postId}") {
        fun createRoute(postId: String): String = "post_detail/$postId"
    }
    data object Comments : Screen("comments/{postId}") {
        fun createRoute(postId: String): String = "comments/$postId"
    }
    data object LikesList : Screen("likes_list/{postId}") {
        fun createRoute(postId: String): String = "likes_list/$postId"
    }
    data object ImageViewer : Screen("image_viewer?imageUrl={imageUrl}") {
        fun createRoute(imageUrl: String): String {
            val encoded = java.net.URLEncoder.encode(imageUrl, "UTF-8")
            return "image_viewer?imageUrl=$encoded"
        }
    }
    data object Messenger : Screen("messenger")
    data object NewMessage : Screen("new_message")
    data object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: String): String = "chat/$conversationId"
    }
    data object ChatInfo : Screen("chat_info/{conversationId}") {
        fun createRoute(conversationId: String): String = "chat_info/$conversationId"
    }
    data object Terms : Screen("terms")
    data object Profile : Screen("profile/{userId}") {
        fun createRoute(userId: String): String = "profile/$userId"
    }
    data object EditProfile : Screen("edit_profile")
    data object FollowersList : Screen("followers_list/{userId}") {
        fun createRoute(userId: String): String = "followers_list/$userId"
    }
    data object FollowingList : Screen("following_list/{userId}") {
        fun createRoute(userId: String): String = "following_list/$userId"
    }
    data object Settings : Screen("settings")
    data object BlockedUsers : Screen("blocked_users")
}
