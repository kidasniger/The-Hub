package com.thehub.hb.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thehub.hb.data.repository.AdminAccountRow
import com.thehub.hb.data.repository.AdminPostRow
import com.thehub.hb.data.repository.AdminReportRow
import com.thehub.hb.data.repository.AdminRepository
import com.thehub.hb.data.repository.AdminStats
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(repository: AdminRepository) {
    var stats by remember { mutableStateOf<AdminStats?>(null) }

    LaunchedEffect(Unit) {
        stats = repository.getStats().getOrNull()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Tableau de bord", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Espace réservé à l'administration. Aucun feed, ami, messagerie ou profil utilisateur n'est affiché ici.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        val current = stats
        if (current == null) {
            item { CircularProgressIndicator() }
        } else {
            item { AdminStatCard("Utilisateurs", current.users.toString()) }
            item { AdminStatCard("Publications", current.posts.toString()) }
            item { AdminStatCard("Signalements", current.reports.toString()) }
            item { AdminStatCard("Administrateurs", current.admins.toString()) }
        }
    }
}

@Composable
private fun AdminStatCard(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
fun AdminUsersScreen(repository: AdminRepository) {
    var users by remember { mutableStateOf<List<com.thehub.hb.data.repository.AdminUserRow>>(emptyList()) }
    var filter by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            users = repository.getUsers().getOrDefault(emptyList())
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    val filtered = users.filter {
        val q = filter.trim().lowercase()
        q.isBlank() ||
            it.uid.lowercase().contains(q) ||
            it.email.lowercase().contains(q) ||
            it.displayName.lowercase().contains(q) ||
            it.username.lowercase().contains(q)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Utilisateurs", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = filter,
            onValueChange = { filter = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Rechercher") },
            singleLine = true
        )
        Spacer(Modifier.height(10.dp))

        if (loading) {
            CircularProgressIndicator()
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.uid }) { user ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(user.displayName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                listOf(
                                    user.email,
                                    user.username.takeIf { it.isNotBlank() }?.let { "@" + it }
                                ).filterNotNull().filter { it.isNotBlank() }.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "UID: " + user.uid,
                                style = MaterialTheme.typography.labelSmall
                            )
                            if (user.suspended || user.deleted) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    buildString {
                                        if (user.suspended) append("Suspendu")
                                        if (user.suspended && user.deleted) append(" • ")
                                        if (user.deleted) append("Désactivé")
                                    },
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            repository.setUserSuspended(user.uid, !user.suspended)
                                            reload()
                                        }
                                    }
                                ) {
                                    Text(if (user.suspended) "Réactiver" else "Suspendre")
                                }
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            repository.setUserDeleted(user.uid, !user.deleted)
                                            reload()
                                        }
                                    }
                                ) {
                                    Text(if (user.deleted) "Restaurer" else "Désactiver")
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
fun AdminPostsScreen(repository: AdminRepository) {
    var posts by remember { mutableStateOf<List<AdminPostRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            posts = repository.getPosts().getOrDefault(emptyList())
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Publications", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(10.dp))
        if (loading) {
            CircularProgressIndicator()
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(posts, key = { it.id }) { post ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                post.text.ifBlank { "Publication sans texte" },
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 8
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("Auteur: " + post.authorId)
                            Text(post.likes.toString() + " j'aime • " + post.comments + " commentaires")
                            Text("ID: " + post.id, style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        repository.deletePost(post.id)
                                        reload()
                                    }
                                }
                            ) {
                                Text("Supprimer")
                            }
                        }
                    }
                }
            }
        }
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

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Signalements", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(10.dp))
        if (loading) {
            CircularProgressIndicator()
        } else if (reports.isEmpty()) {
            Text("Aucun signalement.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(reports, key = { it.id }) { report ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(report.reason, style = MaterialTheme.typography.titleMedium)
                            Text("Statut: " + report.status)
                            Text("Reporter: " + report.reporterId)
                            Text("Cible: " + report.targetId)
                            Text("ID: " + report.id, style = MaterialTheme.typography.labelSmall)
                            if (report.status != "resolved") {
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            repository.resolveReport(report.id)
                                            reload()
                                        }
                                    }
                                ) {
                                    Text("Marquer traité")
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
fun AdminAdminsScreen(repository: AdminRepository) {
    var admins by remember { mutableStateOf<List<AdminAccountRow>>(emptyList()) }
    var targetUid by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            admins = repository.getAdmins().getOrDefault(emptyList())
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Administrateurs", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(10.dp))
        Button(onClick = { showAddDialog = true }) {
            Text("Ajouter un administrateur")
        }
        Spacer(Modifier.height(12.dp))

        if (loading) {
            CircularProgressIndicator()
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(admins, key = { it.uid }) { admin ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(admin.displayName, style = MaterialTheme.typography.titleMedium)
                            Text(admin.email)
                            Text("UID: " + admin.uid, style = MaterialTheme.typography.labelSmall)
                            if (admin.uid != repository.currentUserId) {
                                Spacer(Modifier.height(8.dp))
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
                    Text("Entre l'UID Firebase du compte à promouvoir.")
                    Spacer(Modifier.height(8.dp))
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
