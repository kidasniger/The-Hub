package com.thehub.hb.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thehub.hb.data.repository.AdminAccountRow
import com.thehub.hb.data.repository.AdminPostRow
import com.thehub.hb.data.repository.AdminReportRow
import com.thehub.hb.data.repository.AdminRepository
import com.thehub.hb.data.repository.AdminStats
import com.thehub.hb.data.repository.AdminUserRow
import com.thehub.hb.ui.theme.HubBlue
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubOutline
import com.thehub.hb.ui.theme.HubSuccess
import com.thehub.hb.ui.theme.HubSurface
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubViolet
import com.thehub.hb.ui.components.UserAvatar
import kotlinx.coroutines.launch

@Composable
private fun AdminHeader(
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing?.invoke()
    }
}

@Composable
private fun AdminPill(text: String, tint: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = tint.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AdminLoading() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(30.dp),
            strokeWidth = 2.5.dp,
            color = HubViolet
        )
    }
}

@Composable
private fun AdminEmpty(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 45.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = HubViolet.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = HubViolet, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AdminMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = HubSurface),
        border = BorderStroke(1.dp, HubOutline)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(12.dp),
                color = tint.copy(alpha = 0.14f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
                }
            }
            Column {
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AdminDashboardScreen(repository: AdminRepository) {
    var stats by remember { mutableStateOf<AdminStats?>(null) }
    var logs by remember { mutableStateOf<List<com.thehub.hb.data.repository.AdminLogRow>>(emptyList()) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            refreshing = true
            stats = repository.getStats().getOrNull()
            logs = repository.getRecentLogs().getOrDefault(emptyList())
            refreshing = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AdminHeader("Vue d’ensemble", "Surveille l’activité et la modération de The Hub.") {
                Surface(
                    shape = CircleShape,
                    color = HubSurface,
                    border = BorderStroke(1.dp, HubOutline)
                ) {
                    IconButton(onClick = { reload() }) {
                        if (refreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = HubViolet
                            )
                        } else {
                            Icon(Icons.Filled.Refresh, "Actualiser")
                        }
                    }
                }
            }
        }
        val current = stats
        if (current == null) {
            item { AdminLoading() }
        } else {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminMetricCard("Utilisateurs", current.users.toString(), Icons.Filled.Group, HubBlue, Modifier.weight(1f).aspectRatio(1.08f))
                    AdminMetricCard("Publications", current.posts.toString(), Icons.Filled.Article, HubViolet, Modifier.weight(1f).aspectRatio(1.08f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminMetricCard("Signalements", current.reports.toString(), Icons.Filled.Flag, HubError, Modifier.weight(1f).aspectRatio(1.08f))
                    AdminMetricCard("Administrateurs", current.admins.toString(), Icons.Filled.VerifiedUser, HubSuccess, Modifier.weight(1f).aspectRatio(1.08f))
                }
            }
            item {
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    RoundedCornerShape(22.dp),
                    color = HubSurface,
                    border = BorderStroke(1.dp, HubOutline)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Security, null, tint = HubViolet, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Espace protégé", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Cette console est séparée du réseau social. Les actions sensibles restent contrôlées par les règles Firestore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (logs.isNotEmpty()) {
                item {
                    Surface(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        RoundedCornerShape(22.dp),
                        color = HubSurface,
                        border = BorderStroke(1.dp, HubOutline)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Security, null, tint = HubBlue, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Journal récent",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            logs.take(8).forEach { log ->
                                AdminInfoLine(
                                    label = log.action.replace('_', ' ').replaceFirstChar { it.uppercase() },
                                    value = buildString {
                                        append(log.targetId ?: "—")
                                        if (!log.adminId.isNullOrBlank()) {
                                            append(" • admin ")
                                            append(log.adminId.take(8))
                                        }
                                    }
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUsersScreen(repository: AdminRepository) {
    var users by remember { mutableStateOf<List<AdminUserRow>>(emptyList()) }
    var filter by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var pendingAction by remember { mutableStateOf<AdminUserRow?>(null) }
    var actionType by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            users = repository.getUsers().getOrDefault(emptyList())
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    val query = filter.trim().lowercase()
    val filtered = users.filter {
        query.isBlank() ||
            it.uid.lowercase().contains(query) ||
            it.email.lowercase().contains(query) ||
            it.displayName.lowercase().contains(query) ||
            it.username.lowercase().contains(query)
    }

    Column(Modifier.fillMaxSize()) {
        AdminHeader("Utilisateurs", users.size.toString() + " comptes • contrôle et modération") {
            Surface(shape = CircleShape, color = HubSurface, border = BorderStroke(1.dp, HubOutline)) {
                IconButton(onClick = { reload() }) {
                    Icon(Icons.Filled.Refresh, "Actualiser")
                }
            }
        }
        OutlinedTextField(
            value = filter,
            onValueChange = { filter = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            placeholder = { Text("Nom, e-mail, @username ou UID") },
            shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = HubSurface,
                unfocusedContainerColor = HubSurface,
                disabledContainerColor = HubSurface,
                focusedIndicatorColor = HubViolet,
                unfocusedIndicatorColor = HubOutline
            )
        )
        Spacer(Modifier.height(10.dp))
        when {
            loading -> AdminLoading()
            filtered.isEmpty() -> AdminEmpty(
                "Aucun utilisateur",
                if (query.isBlank()) "Aucun compte ne correspond au filtre actuel." else "Aucun résultat pour « " + filter + " ».",
                Icons.Filled.People
            )
            else -> LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.uid }) { user ->
                    AdminUserCard(
                        user = user,
                        onSuspend = {
                            pendingAction = user
                            actionType = if (user.suspended) "restore" else "suspend"
                        },
                        onDelete = {
                            pendingAction = user
                            actionType = if (user.deleted) "restore_deleted" else "delete"
                        }
                    )
                }
            }
        }
    }

    val selected = pendingAction
    val action = actionType
    if (selected != null && action != null) {
        val suspendAction = action == "suspend"
        val deleteAction = action == "delete"
        AlertDialog(
            onDismissRequest = {
                pendingAction = null
                actionType = null
            },
            title = {
                Text(
                    if (suspendAction) "Suspendre ce compte ?"
                    else if (deleteAction) "Désactiver ce compte ?"
                    else "Restaurer ce compte ?"
                )
            },
            text = {
                Text(
                    if (suspendAction) {
                        "Le compte de " + selected.displayName + " ne pourra plus accéder à The Hub."
                    } else if (deleteAction) {
                        "Le compte sera marqué comme désactivé. Cette action ne supprime pas les données de façon irréversible."
                    } else {
                        "Le compte retrouvera son état normal."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            when (action) {
                                "suspend" -> repository.setUserSuspended(selected.uid, true)
                                "restore" -> repository.setUserSuspended(selected.uid, false)
                                "delete" -> repository.setUserDeleted(selected.uid, true)
                                "restore_deleted" -> repository.setUserDeleted(selected.uid, false)
                            }
                            pendingAction = null
                            actionType = null
                            reload()
                        }
                    }
                ) {
                    Text("Confirmer")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingAction = null
                    actionType = null
                }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun AdminUserCard(
    user: AdminUserRow,
    onSuspend: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = HubSurface),
        border = BorderStroke(1.dp, HubOutline)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                    name = user.displayName,
                    photoUrl = user.photoUrl,
                    size = 46.dp
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        user.displayName.ifBlank { "Utilisateur" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        user.email.ifBlank { "E-mail non renseigné" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(Icons.Filled.MoreHoriz, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AdminPill(
                    if (user.suspended) "Suspendu" else "Actif",
                    if (user.suspended) HubError else HubSuccess,
                    if (user.suspended) Icons.Filled.Block else Icons.Filled.CheckCircle
                )
                if (user.deleted) AdminPill("Désactivé", HubError, Icons.Filled.Delete)
                if (user.username.isNotBlank()) AdminPill("@" + user.username, HubBlue, Icons.Filled.People)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "UID " + user.uid,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    user.postsCount.toString() + " publications",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    user.followersCount.toString() + " abonnés",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    user.followingCount.toString() + " suivis",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(11.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSuspend, Modifier.weight(1f)) {
                    Icon(
                        if (user.suspended) Icons.Filled.Restore else Icons.Filled.Block,
                        null,
                        Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (user.suspended) "Réactiver" else "Suspendre")
                }
                OutlinedButton(onClick = onDelete, Modifier.weight(1f)) {
                    Icon(
                        if (user.deleted) Icons.Filled.Restore else Icons.Filled.Delete,
                        null,
                        Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (user.deleted) "Restaurer" else "Désactiver")
                }
            }
        }
    }
}

@Composable
fun AdminPostsScreen(repository: AdminRepository) {
    var posts by remember { mutableStateOf<List<AdminPostRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var pendingDelete by remember { mutableStateOf<AdminPostRow?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            posts = repository.getPosts().getOrDefault(emptyList())
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize()) {
        AdminHeader("Publications", posts.size.toString() + " publications • modération du contenu") {
            Surface(shape = CircleShape, color = HubSurface, border = BorderStroke(1.dp, HubOutline)) {
                IconButton(onClick = { reload() }) {
                    Icon(Icons.Filled.Refresh, "Actualiser")
                }
            }
        }
        when {
            loading -> AdminLoading()
            posts.isEmpty() -> AdminEmpty(
                "Aucune publication",
                "Il n’y a aucune publication à modérer pour le moment.",
                Icons.Filled.Article
            )
            else -> LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(posts, key = { it.id }) { post ->
                    Card(
                        Modifier.fillMaxWidth(),
                        RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = HubSurface),
                        border = BorderStroke(1.dp, HubOutline)
                    ) {
                        Column(Modifier.padding(15.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    Modifier.size(36.dp),
                                    CircleShape,
                                    color = HubBlue.copy(alpha = 0.13f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Article, null, tint = HubBlue, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Publication", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Auteur " + post.authorId,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                AdminPill("Active", HubSuccess, Icons.Filled.CheckCircle)
                            }
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                Modifier.fillMaxWidth(),
                                RoundedCornerShape(16.dp),
                                color = HubSurfaceElevated
                            ) {
                                Text(
                                    post.text.ifBlank { "Publication sans texte" },
                                    Modifier.padding(13.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 7,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(post.likes.toString() + " j'aime", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(post.comments.toString() + " commentaires", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("ID " + post.id, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(onClick = { pendingDelete = post }) {
                                Icon(Icons.Filled.Delete, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Supprimer")
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { post ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Supprimer la publication ?") },
            text = { Text("Cette publication sera retirée du flux. Cette action n’est pas réversible depuis la console.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.deletePost(post.id)
                        pendingDelete = null
                        reload()
                    }
                }) {
                    Text("Supprimer", color = HubError)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun AdminReportsScreen(repository: AdminRepository) {
    var reports by remember { mutableStateOf<List<AdminReportRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            reports = repository.getReports().getOrDefault(emptyList())
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    val pending = reports.count { it.status != "resolved" }

    Column(Modifier.fillMaxSize()) {
        AdminHeader(
            "Signalements",
            pending.toString() + " en attente • sécurité et modération"
        ) {
            AdminPill(
                if (pending == 0) "À jour" else pending.toString() + " à traiter",
                if (pending == 0) HubSuccess else HubError,
                if (pending == 0) Icons.Filled.CheckCircle else Icons.Filled.Flag
            )
        }
        when {
            loading -> AdminLoading()
            reports.isEmpty() -> AdminEmpty("Tout est calme", "Aucun signalement à traiter actuellement.", Icons.Filled.Flag)
            else -> LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(reports, key = { it.id }) { report ->
                    val resolved = report.status == "resolved"
                    Card(
                        Modifier.fillMaxWidth(),
                        RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = HubSurface),
                        border = BorderStroke(1.dp, HubOutline)
                    ) {
                        Column(Modifier.padding(15.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    Modifier.size(38.dp),
                                    RoundedCornerShape(11.dp),
                                    color = (if (resolved) HubSuccess else HubError).copy(alpha = 0.12f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (resolved) Icons.Filled.CheckCircle else Icons.Filled.Flag,
                                            null,
                                            tint = if (resolved) HubSuccess else HubError,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(report.reason, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        if (resolved) "Traité" else "En attente",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                AdminPill(
                                    if (resolved) "Traité" else "À traiter",
                                    if (resolved) HubSuccess else HubError,
                                    if (resolved) Icons.Filled.CheckCircle else Icons.Filled.Flag
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Divider(color = HubOutline.copy(alpha = 0.7f))
                            Spacer(Modifier.height(10.dp))
                            AdminInfoLine("Reporter", report.reporterId)
                            AdminInfoLine("Cible", report.targetId)
                            AdminInfoLine("ID", report.id)
                            if (!resolved) {
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = {
                                    scope.launch {
                                        repository.resolveReport(report.id)
                                        reload()
                                    }
                                }) {
                                    Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Marquer comme traité")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminInfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            Modifier.width(76.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value.ifBlank { "—" },
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AdminAdminsScreen(repository: AdminRepository) {
    var admins by remember { mutableStateOf<List<AdminAccountRow>>(emptyList()) }
    var targetUid by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var isSuperAdmin by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            admins = repository.getAdmins().getOrDefault(emptyList())
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        isSuperAdmin = repository.isCurrentUserSuperAdmin()
        reload()
    }

    Column(Modifier.fillMaxSize()) {
        AdminHeader(
            "Administrateurs",
            admins.size.toString() + " accès • gestion des privilèges"
        ) {
            if (isSuperAdmin) {
                Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.AdminPanelSettings, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                    Text("Ajouter")
                }
            }
        }
        when {
            loading -> AdminLoading()
            admins.isEmpty() -> AdminEmpty(
                "Aucun administrateur",
                "Le compte bootstrap sera créé automatiquement à sa première connexion.",
                Icons.Filled.AdminPanelSettings
            )
            else -> LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(admins, key = { it.uid }) { admin ->
                    Card(
                        Modifier.fillMaxWidth(),
                        RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = HubSurface),
                        border = BorderStroke(1.dp, HubOutline)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(Modifier.size(42.dp), CircleShape, color = HubViolet.copy(alpha = 0.13f)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.AdminPanelSettings, null, tint = HubViolet, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        admin.displayName.ifBlank { "Administrateur" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        admin.email.ifBlank { "E-mail non renseigné" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    AdminPill(
                                        if (admin.active) "Actif" else "Inactif",
                                        if (admin.active) HubSuccess else HubError,
                                        if (admin.active) Icons.Filled.CheckCircle else Icons.Filled.Block
                                    )
                                    AdminPill(
                                        if (admin.role == "superadmin") "SUPER ADMIN" else "ADMIN",
                                        if (admin.role == "superadmin") HubViolet else HubBlue,
                                        Icons.Filled.VerifiedUser
                                    )
                                }
                            }
                            Spacer(Modifier.height(9.dp))
                            Text(
                                "UID " + admin.uid,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isSuperAdmin && admin.uid != repository.currentUserId) {
                                Spacer(Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            repository.removeAdmin(admin.uid)
                                            reload()
                                        }
                                    }
                                ) {
                                    Text("Retirer l'accès")
                                }
                            } else if (admin.uid == repository.currentUserId) {
                                Spacer(Modifier.height(10.dp))
                                AdminPill(
                                    if (admin.role == "superadmin") "Votre compte • protégé" else "Votre compte",
                                    HubBlue,
                                    Icons.Filled.VerifiedUser
                                )
                            } else if (!isSuperAdmin) {
                                Spacer(Modifier.height(10.dp))
                                AdminPill(
                                    "Géré par le superadmin",
                                    HubBlue,
                                    Icons.Filled.Security
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Ajouter un administrateur") },
            text = {
                Column {
                    Text(
                        "Promouvoir un compte déjà présent dans The Hub à partir de son UID Firebase.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = targetUid,
                        onValueChange = { targetUid = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("UID Firebase") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.addAdmin(targetUid.trim())
                            targetUid = ""
                            showAddDialog = false
                            reload()
                        }
                    },
                    enabled = targetUid.isNotBlank()
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
