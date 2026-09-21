package com.thehub.hb.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
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
import com.thehub.hb.data.repository.AdminAnalytics
import com.thehub.hb.data.repository.AdminCommentRow
import com.thehub.hb.data.repository.AdminRepository
import com.thehub.hb.data.repository.AdminSystemConfig
import com.thehub.hb.ui.theme.HubBlue
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubOutline
import com.thehub.hb.ui.theme.HubSuccess
import com.thehub.hb.ui.theme.HubSurface
import com.thehub.hb.ui.theme.HubViolet
import kotlinx.coroutines.launch

private enum class OpsSection(val label: String) {
    ANALYTICS("Analytics"),
    MODERATION("Modération"),
    SECURITY("Sécurité"),
    CONTENT("Contenu"),
    SUPPORT("Support"),
    SYSTEM("Système"),
    ADMINS("Admins")
}

private enum class ModerationMode(val label: String) {
    POSTS("Publications"),
    COMMENTS("Commentaires"),
    REPORTS("Signalements")
}

@Composable
fun AdminOperationsScreen(repository: AdminRepository) {
    var section by remember { mutableStateOf(OpsSection.ANALYTICS) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Centre d’administration", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Modération, analytics, sécurité, contenu, support et configuration.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OpsSection.values().forEach { item ->
                    OutlinedButton(
                        onClick = { section = item },
                        border = BorderStroke(1.dp, if (item == section) HubViolet else HubOutline)
                    ) {
                        Text(item.label)
                    }
                }
            }
        }

        when (section) {
            OpsSection.ANALYTICS -> AdminAnalyticsPanel(repository)
            OpsSection.MODERATION -> AdminModerationPanel(repository)
            OpsSection.SECURITY -> AdminSecurityPanel(repository)
            OpsSection.CONTENT -> AdminContentPanel(repository)
            OpsSection.SUPPORT -> AdminSupportPanel(repository)
            OpsSection.SYSTEM -> AdminSystemPanel(repository)
            OpsSection.ADMINS -> AdminAdminsScreen(repository)
        }
    }
}

@Composable
private fun Metric(title: String, value: String, detail: String, tint: Color) {
    Card(
        Modifier.width(165.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = HubSurface),
        border = BorderStroke(1.dp, HubOutline)
    ) {
        Column(Modifier.padding(14.dp)) {
            Surface(Modifier.size(34.dp), RoundedCornerShape(10.dp), color = tint.copy(alpha = 0.12f)) {
                Icon(Icons.Filled.Analytics, null, tint = tint, modifier = Modifier.padding(8.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = tint)
        }
    }
}

@Composable
private fun Panel(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = HubSurface),
        border = BorderStroke(1.dp, HubOutline)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun AdminAnalyticsPanel(repository: AdminRepository) {
    var data by remember { mutableStateOf<AdminAnalytics?>(null) }
    var sessions by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            data = repository.getAnalytics().getOrNull()
            sessions = repository.getActiveSessions().getOrDefault(0)
            loading = false
        }
    }
    LaunchedEffect(Unit) { reload() }

    if (loading && data == null) {
        CircularProgressIndicator(Modifier.padding(24.dp))
        return
    }

    val a = data
    if (a == null) {
        Text("Impossible de charger les analytics.", Modifier.padding(24.dp), color = HubError)
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("Utilisateurs", a.users.toString(), a.users7d.toString() + " nouveaux / 7 j", HubBlue)
                Metric("Actifs", a.activeUsers.toString(), sessions.toString() + " sessions récentes", HubSuccess)
                Metric("Publications", a.posts.toString(), a.posts7d.toString() + " nouvelles / 7 j", HubViolet)
                Metric("Commentaires", a.comments.toString(), "tous contenus", HubBlue)
            }
        }
        item {
            Panel("Modération", "État global de la sécurité de la communauté.") {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Metric("Signalements", a.reports.toString(), a.pendingReports.toString() + " en attente", HubError)
                    Metric("Suspendus", a.suspendedUsers.toString(), "comptes", HubError)
                    Metric("Désactivés", a.deletedUsers.toString(), "comptes", HubError)
                }
            }
        }
        item {
            Panel("Croissance", "Les statistiques sont calculées directement depuis Firestore, sans Google Analytics.") {
                Text(
                    a.users30d.toString() + " nouveaux utilisateurs et " + a.posts30d.toString() + " publications sur 30 jours.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { reload() }) { Text("Actualiser") }
            }
        }
    }
}

@Composable
private fun AdminModerationPanel(repository: AdminRepository) {
    var mode by remember { mutableStateOf(ModerationMode.POSTS) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModerationMode.values().forEach {
                OutlinedButton(
                    onClick = { mode = it },
                    border = BorderStroke(1.dp, if (mode == it) HubViolet else HubOutline)
                ) { Text(it.label) }
            }
        }
        when (mode) {
            ModerationMode.POSTS -> AdminPostsScreen(repository)
            ModerationMode.REPORTS -> AdminReportsScreen(repository)
            ModerationMode.COMMENTS -> AdminCommentsPanel(repository)
        }
    }
}

@Composable
private fun AdminCommentsPanel(repository: AdminRepository) {
    var comments by remember { mutableStateOf<List<AdminCommentRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<AdminCommentRow?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            comments = repository.getComments().getOrDefault(emptyList())
            loading = false
        }
    }
    LaunchedEffect(Unit) { reload() }

    if (loading) {
        CircularProgressIndicator(Modifier.padding(24.dp))
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(comments, key = { it.postId + "_" + it.id }) { comment ->
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = HubSurface),
                border = BorderStroke(1.dp, HubOutline)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Utilisateur " + comment.authorId, color = HubBlue, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(comment.text.ifBlank { "Commentaire sans texte" }, maxLines = 5, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Publication " + comment.postId + " • " + comment.likes + " j’aime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { selected = comment }) {
                        Icon(Icons.Filled.Delete, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Supprimer")
                    }
                }
            }
        }
    }

    selected?.let { comment ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("Supprimer ce commentaire ?") },
            text = { Text("Le commentaire sera retiré et le compteur de la publication sera ajusté.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.deleteComment(comment.postId, comment.id)
                        selected = null
                        reload()
                    }
                }) { Text("Supprimer", color = HubError) }
            },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun AdminSecurityPanel(repository: AdminRepository) {
    var logs by remember { mutableStateOf(emptyList<com.thehub.hb.data.repository.AdminLogRow>()) }
    var sessions by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            logs = repository.getRecentLogs(40).getOrDefault(emptyList())
            sessions = repository.getActiveSessions().getOrDefault(0)
            loading = false
        }
    }
    LaunchedEffect(Unit) { reload() }

    if (loading) {
        CircularProgressIndicator(Modifier.padding(24.dp))
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Panel("Sécurité", "Sessions et opérations sensibles.") {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Metric("Sessions", sessions.toString(), "30 dernières minutes", HubSuccess)
                    Metric("Actions", logs.size.toString(), "journal récent", HubBlue)
                }
            }
        }
        item {
            Panel("Journal d’audit", "Lecture seule depuis la console.") {
                logs.take(20).forEach { log ->
                    Column(Modifier.padding(vertical = 5.dp)) {
                        Text(log.action.replace('_', ' ').replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Cible " + (log.targetId ?: "—") + " • admin " + log.adminId.take(8),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedButton(onClick = { reload() }) { Text("Actualiser") }
            }
        }
    }
}

@Composable
private fun AdminContentPanel(repository: AdminRepository) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var announcements by remember { mutableStateOf(emptyList<com.thehub.hb.data.repository.AdminAnnouncementRow>()) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch { announcements = repository.getAnnouncements().getOrDefault(emptyList()) }
    }
    LaunchedEffect(Unit) { reload() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Panel("Annonces", "Publie une information visible par les utilisateurs.") {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Titre") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Message") },
                    minLines = 4
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            if (repository.publishAnnouncement(title, body).isSuccess) {
                                title = ""
                                body = ""
                                reload()
                            }
                        }
                    },
                    enabled = title.isNotBlank() && body.isNotBlank()
                ) { Text("Publier") }
            }
        }
        item {
            Panel("Historique", "Dernières annonces.") {
                if (announcements.isEmpty()) {
                    Text("Aucune annonce.")
                } else {
                    announcements.take(10).forEach {
                        Column(Modifier.padding(vertical = 6.dp)) {
                            Text(it.title, fontWeight = FontWeight.SemiBold)
                            Text(it.body, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSupportPanel(repository: AdminRepository) {
    var tickets by remember { mutableStateOf(emptyList<com.thehub.hb.data.repository.AdminSupportTicketRow>()) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            tickets = repository.getSupportTickets().getOrDefault(emptyList())
            loading = false
        }
    }
    LaunchedEffect(Unit) { reload() }

    if (loading) {
        CircularProgressIndicator(Modifier.padding(24.dp))
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(tickets, key = { it.id }) { ticket ->
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = HubSurface),
                border = BorderStroke(1.dp, HubOutline)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Forum, null, tint = HubBlue, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(ticket.subject, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(ticket.message, maxLines = 6, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(7.dp))
                    Text(
                        ticket.priority.uppercase() + " • " + ticket.status + " • " + ticket.userId,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (ticket.status != "resolved") {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = {
                            scope.launch {
                                repository.resolveSupportTicket(ticket.id)
                                reload()
                            }
                        }) { Text("Marquer résolu") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSystemPanel(repository: AdminRepository) {
    var config by remember { mutableStateOf(AdminSystemConfig()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            config = repository.getSystemConfig().getOrDefault(AdminSystemConfig())
            loading = false
        }
    }
    LaunchedEffect(Unit) { reload() }

    if (loading) {
        CircularProgressIndicator(Modifier.padding(24.dp))
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Panel("État du service", "Contrôle centralisé des fonctions sensibles.") {
                SwitchRow("Mode maintenance", config.maintenance) { config = config.copy(maintenance = it) }
                SwitchRow("Nouvelles inscriptions", config.newRegistrations) { config = config.copy(newRegistrations = it) }
                SwitchRow("Publications", config.postsEnabled) { config = config.copy(postsEnabled = it) }
                SwitchRow("Messagerie", config.messagingEnabled) { config = config.copy(messagingEnabled = it) }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = config.maintenanceMessage,
                    onValueChange = { config = config.copy(maintenanceMessage = it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Message de maintenance") },
                    minLines = 3
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        scope.launch {
                            saving = true
                            repository.setSystemConfig(config)
                            saving = false
                        }
                    },
                    enabled = !saving
                ) {
                    if (saving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Enregistrer")
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
