package com.thehub.hb.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.thehub.hb.di.AppContainer
import com.thehub.hb.ui.admin.AdminAdminsScreen
import com.thehub.hb.ui.admin.AdminDashboardScreen
import com.thehub.hb.ui.admin.AdminPostsScreen
import com.thehub.hb.ui.admin.AdminOperationsScreen
import com.thehub.hb.ui.admin.AdminReportsScreen
import com.thehub.hb.ui.admin.AdminScaffoldScreen
import com.thehub.hb.ui.admin.AdminUsersScreen

@Composable
fun AdminNavGraph(
    appContainer: AppContainer,
    onExit: () -> Unit,
    onOpenFeed: () -> Unit = {},
    navController: NavHostController = rememberNavController()
) {
    val repository = appContainer.adminRepository
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: AdminScreen.Dashboard.route

    var accessState by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        accessState = repository.isCurrentUserAdmin()
        if (accessState == false) onExit()
    }

    if (accessState != true) return

    NavHost(
        navController = navController,
        startDestination = AdminScreen.Dashboard.route
    ) {
        composable(AdminScreen.Dashboard.route) {
            AdminScaffoldScreen(
                currentRoute = route,
                onNavigate = { navController.navigate(it) { launchSingleTop = true } },
                onExit = onExit,
                onOpenFeed = onOpenFeed
            ) {
                AdminDashboardScreen(repository)
            }
        }
        composable(AdminScreen.Users.route) {
            AdminScaffoldScreen(
                currentRoute = route,
                onNavigate = { navController.navigate(it) { launchSingleTop = true } },
                onExit = onExit,
                onOpenFeed = onOpenFeed
            ) {
                AdminUsersScreen(repository)
            }
        }
        composable(AdminScreen.Posts.route) {
            AdminScaffoldScreen(
                currentRoute = route,
                onNavigate = { navController.navigate(it) { launchSingleTop = true } },
                onExit = onExit,
                onOpenFeed = onOpenFeed
            ) {
                AdminPostsScreen(repository)
            }
        }
        composable(AdminScreen.Operations.route) {
            AdminScaffoldScreen(
                currentRoute = route,
                onNavigate = { navController.navigate(it) { launchSingleTop = true } },
                onExit = onExit,
                onOpenFeed = onOpenFeed
            ) {
                AdminOperationsScreen(repository)
            }
        }
        composable(AdminScreen.Reports.route) {
            AdminScaffoldScreen(
                currentRoute = route,
                onNavigate = { navController.navigate(it) { launchSingleTop = true } },
                onExit = onExit,
                onOpenFeed = onOpenFeed
            ) {
                AdminReportsScreen(repository)
            }
        }
        composable(AdminScreen.Admins.route) {
            AdminScaffoldScreen(
                currentRoute = route,
                onNavigate = { navController.navigate(it) { launchSingleTop = true } },
                onExit = onExit,
                onOpenFeed = onOpenFeed
            ) {
                AdminAdminsScreen(repository)
            }
        }
    }
}
