package com.thehub.hb.ui.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thehub.hb.navigation.AdminScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScaffoldScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onExit: () -> Unit,
    content: @Composable () -> Unit
) {
    val tabs = listOf(
        AdminScreen.Dashboard,
        AdminScreen.Users,
        AdminScreen.Posts,
        AdminScreen.Reports,
        AdminScreen.Admins
    )
    val title = tabs.firstOrNull { it.route == currentRoute }?.label ?: "Administration"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("The Hub • $title") },
                actions = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Filled.Home, contentDescription = "Quitter l'administration")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = { onNavigate(tab.route) },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    AdminScreen.Dashboard -> Icons.Filled.Home
                                    AdminScreen.Users -> Icons.Filled.People
                                    AdminScreen.Posts -> Icons.Filled.Article
                                    AdminScreen.Reports -> Icons.Filled.Flag
                                    AdminScreen.Admins -> Icons.Filled.AdminPanelSettings
                                },
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            content()
        }
    }
}
