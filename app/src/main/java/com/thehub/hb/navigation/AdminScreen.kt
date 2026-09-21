package com.thehub.hb.navigation

sealed class AdminScreen(val route: String, val label: String) {
    data object Dashboard : AdminScreen("admin_dashboard", "Accueil")
    data object Users : AdminScreen("admin_users", "Utilisateurs")
    data object Posts : AdminScreen("admin_posts", "Publications")
    data object Reports : AdminScreen("admin_reports", "Signalements")
    data object Admins : AdminScreen("admin_admins", "Administrateurs")
}
