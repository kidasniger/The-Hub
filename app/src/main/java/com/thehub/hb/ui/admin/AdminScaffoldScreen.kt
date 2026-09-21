package com.thehub.hb.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.thehub.hb.navigation.AdminScreen
import com.thehub.hb.ui.theme.HubOutline
import com.thehub.hb.ui.theme.HubSurface
import com.thehub.hb.ui.theme.HubViolet
import com.thehub.hb.ui.theme.HubWhite

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
        AdminScreen.Operations,
        AdminScreen.Reports,
        AdminScreen.Admins
    )
    val currentTab = tabs.firstOrNull { it.route == currentRoute } ?: AdminScreen.Dashboard

    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(34.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = HubViolet.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = HubViolet,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        Spacer(Modifier.size(10.dp))
                        Column {
                            Text(
                                "The Hub",
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Console admin • " + currentTab.label,
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 8.dp, end = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.AdminPanelSettings,
                                contentDescription = null,
                                tint = HubViolet,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                "ADMIN",
                                modifier = Modifier.padding(horizontal = 7.dp),
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                color = HubWhite
                            )
                            IconButton(onClick = onExit, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    Icons.Filled.Logout,
                                    contentDescription = "Quitter l’administration",
                                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                color = HubSurface,
                border = BorderStroke(1.dp, HubOutline),
                shadowElevation = 14.dp
            ) {
                NavigationBar(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    tonalElevation = 0.dp,
                    modifier = Modifier.clip(RoundedCornerShape(28.dp))
                ) {
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
                                        AdminScreen.Operations -> Icons.Filled.Settings
                                        AdminScreen.Reports -> Icons.Filled.Flag
                                        AdminScreen.Admins -> Icons.Filled.AdminPanelSettings
                                    },
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label, maxLines = 1) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
        ) {
            content()
        }
    }
}
