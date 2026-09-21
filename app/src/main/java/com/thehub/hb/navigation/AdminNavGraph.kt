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
import com.thehub.hb.ui.admin.AdminReportsScreen
import com.thehub.hb.ui.admin.AdminScaffoldScreen
import com.thehub.hb.ui.admin.AdminUsersScreen

@Composable
fun AdminNavGraph(
    appContainer: AppContainer,
    onExit: () -> Unit,
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
            AdminScaffoldScreen(route, { navController.navigate(it) { launchSingleTop = true } }, onExit) {
                AdminDashboardScreen(repository)
            }
        }
        composable(AdminScreen.Users.route) {
            AdminScaffoldScreen(route, { navController.navigate(it) { launchSingleTop = true } }, onExit) {
                AdminUsersScreen(repository)
            }
        }
        composable(AdminScreen.Posts.route) {
            AdminScaffoldScreen(route, { navController.navigate(it) { launchSingleTop = true } }, onExit) {
                AdminPostsScreen(repository)
            }
        }
        composable(AdminScreen.Reports.route) {
            AdminScaffoldScreen(route, { navController.navigate(it) { launchSingleTop = true } }, onExit) {
                AdminReportsScreen(repository)
            }
        }
        composable(AdminScreen.Admins.route) {
            AdminScaffoldScreen(route, { navController.navigate(it) { launchSingleTop = true } }, onExit) {
                AdminAdminsScreen(repository)
            }
        }
    }
}
