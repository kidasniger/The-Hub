package com.thehub.hb.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.thehub.hb.data.repository.AdminAnalyticsV2
import com.thehub.hb.data.repository.AdminAntiSpamConfigV2
import com.thehub.hb.data.repository.AdminControlRepository
import com.thehub.hb.data.repository.AdminEmergencyV2
import com.thehub.hb.data.repository.AdminFeatureFlagV2
import com.thehub.hb.data.repository.AdminUserDetailV2
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubOutline
import com.thehub.hb.ui.theme.HubSurface
import com.thehub.hb.ui.theme.HubViolet
import java.util.Date
import kotlinx.coroutines.launch

private enum class AdminV2Section(
    val title: String,
    val description: String,
    val icon: ImageVector
) {
    DASHBOARD("Tableau de bord", "Vue générale de l’activité, des utilisateurs et des alertes.", Icons.Filled.Home),
    USER("Utilisateurs", "Rechercher une personne et ouvrir sa fiche complète.", Icons.Filled.People),
    SANCTIONS("Sanctions", "Avertissements, restrictions et suspensions.", Icons.Filled.Shield),
    QUEUE("File de modération", "Traiter les signalements avec priorité et état.", Icons.Filled.Flag),
    POSTS("Publications", "Masquer, supprimer, épingler ou verrouiller.", Icons.Filled.Article),
    ROLES("Rôles et permissions", "Gérer les rôles et les accès des administrateurs.", Icons.Filled.Security),
    REAUTH("Réauthentification", "Valider à nouveau l’identité avant une action sensible.", Icons.Filled.Security),
    SESSIONS("Sessions administrateur", "Voir les sessions et révoquer les accès.", Icons.Filled.Security),
    AUDIT("Journal des actions", "Historique des actions administratives, filtrable.", Icons.Filled.History),
    NOTIFICATIONS("Notifications", "Envoyer des messages à des groupes d’utilisateurs.", Icons.Filled.Notifications),
    ANNOUNCEMENTS("Annonces", "Créer, programmer et faire expirer des annonces.", Icons.Filled.Notifications),
    VERSIONS("Mises à jour", "Définir la version minimale et les mises à jour forcées.", Icons.Filled.Settings),
    FLAGS("Fonctionnalités", "Activer ou désactiver des fonctions progressivement.", Icons.Filled.Settings),
    EMERGENCY("Mode urgence", "Couper rapidement des fonctions en cas d’incident.", Icons.Filled.Shield),
    INVESTIGATION("Mode enquête", "Afficher la chronologie d’un compte.", Icons.Filled.Search),
    ANALYTICS("Statistiques d’utilisation", "DAU, WAU, MAU et rétention.", Icons.Filled.History),
    ANTISPAM("Protection anti-spam", "Régler les seuils de publication, messages et signalements.", Icons.Filled.Security),
    SUPPORT("Support", "Attribuer les tickets et répondre aux utilisateurs.", Icons.Filled.People)
}

private enum class AdminV2Category(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val sections: List<AdminV2Section>
) {
    OVERVIEW(
        "Vue générale",
        "Pilotage quotidien et statistiques.",
        Icons.Filled.Home,
        listOf(AdminV2Section.DASHBOARD, AdminV2Section.ANALYTICS)
    ),
    PEOPLE(
        "Utilisateurs et sécurité",
        "Comptes, sanctions, sessions et permissions.",
        Icons.Filled.People,
        listOf(AdminV2Section.USER, AdminV2Section.SANCTIONS, AdminV2Section.ROLES, AdminV2Section.REAUTH, AdminV2Section.SESSIONS, AdminV2Section.ANTISPAM)
    ),
    MODERATION(
        "Modération",
        "Signalements et contrôle des publications.",
        Icons.Filled.Flag,
        listOf(AdminV2Section.QUEUE, AdminV2Section.POSTS, AdminV2Section.INVESTIGATION, AdminV2Section.AUDIT)
    ),
    COMMUNICATION(
        "Communication",
        "Notifications et annonces envoyées aux membres.",
        Icons.Filled.Notifications,
        listOf(AdminV2Section.NOTIFICATIONS, AdminV2Section.ANNOUNCEMENTS, AdminV2Section.SUPPORT)
    ),
    SYSTEM(
        "Système",
        "Versions, fonctionnalités et mode urgence.",
        Icons.Filled.Settings,
        listOf(AdminV2Section.VERSIONS, AdminV2Section.FLAGS, AdminV2Section.EMERGENCY)
    )
}

@Composable
fun AdminControlCenterScreen(repository: AdminControlRepository) {
    var selected by remember { mutableStateOf<AdminV2Section?>(null) }

    if (selected == null) {
        AdminCenterHome(onOpenSection = { selected = it })
    } else {
        AdminSectionScreen(
            section = selected!!,
            repository = repository,
            onBack = { selected = null }
        )
    }
}

@Composable
private fun AdminCenterHome(onOpenSection: (AdminV2Section) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Centre d’administration",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Choisis une catégorie. Chaque outil explique clairement à quoi il sert.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(AdminV2Category.values().toList(), key = { it.name }) { category ->
            CategoryCard(category = category, onOpenSection = onOpenSection)
        }

        item {
            Text(
                "18 outils disponibles",
                style = MaterialTheme.typography.labelLarge,
                color = HubViolet,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun CategoryCard(
    category: AdminV2Category,
    onOpenSection: (AdminV2Section) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HubSurface),
        border = BorderStroke(1.dp, HubOutline)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(category.icon, contentDescription = null, tint = HubViolet, modifier = Modifier.size(28.dp))
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(category.title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            for (section in category.sections) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSection(section) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(section.icon, contentDescription = null, tint = HubViolet, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(section.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            section.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSectionScreen(
    section: AdminV2Section,
    repository: AdminControlRepository,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
            }
            Column(Modifier.weight(1f)) {
                Text(section.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    section.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when (section) {
            AdminV2Section.DASHBOARD -> DashboardV2(repository)
            AdminV2Section.USER -> UserPanel(repository)
            AdminV2Section.SANCTIONS -> SanctionPanel(repository)
            AdminV2Section.QUEUE -> QueuePanel(repository)
            AdminV2Section.POSTS -> PostModerationPanel(repository)
            AdminV2Section.ROLES -> RolesPanel(repository)
            AdminV2Section.REAUTH -> ReauthPanel(repository)
            AdminV2Section.SESSIONS -> SessionsPanel(repository)
            AdminV2Section.AUDIT -> AuditPanel(repository)
            AdminV2Section.NOTIFICATIONS -> NotificationsPanel(repository)
            AdminV2Section.ANNOUNCEMENTS -> AnnouncementsPanel(repository)
            AdminV2Section.VERSIONS -> VersionsPanel(repository)
            AdminV2Section.FLAGS -> FlagsPanel(repository)
            AdminV2Section.EMERGENCY -> EmergencyPanel(repository)
            AdminV2Section.INVESTIGATION -> InvestigationPanel(repository)
            AdminV2Section.ANALYTICS -> AnalyticsPanel(repository)
            AdminV2Section.ANTISPAM -> AntiSpamPanel(repository)
            AdminV2Section.SUPPORT -> SupportPanel(repository)
        }
    }
}

@Composable
private fun Panel(title: String, subtitle: String = "", content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HubSurface),
        border = BorderStroke(1.dp, HubOutline)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DashboardV2(repo: AdminControlRepository) {
    var data by remember { mutableStateOf<com.thehub.hb.data.repository.AdminDashboardV2?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun load() = scope.launch { repo.dashboard().onSuccess { data = it; error = null }.onFailure { error = it.message } }
    LaunchedEffect(Unit) { load() }
    if (data == null && error == null) CircularProgressIndicator(Modifier.padding(24.dp))
    else LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { error?.let { Text(it, color = HubError) } }
        data?.let { d ->
            item { Panel("Vue d’ensemble", "Temps réel alimenté par Firestore.") {
                Text("Utilisateurs : " + d.users)
                Text("Actifs aujourd’hui : " + d.activeToday + " • 7 j : " + d.active7d + " • 30 j : " + d.active30d)
                Text("Nouveaux aujourd’hui : " + d.newUsersToday + " • Suspendus : " + d.suspendedUsers)
                Text("Publications aujourd’hui : " + d.postsToday + " • Commentaires : " + d.commentsToday)
                Text("Signalements en attente : " + d.pendingReports + " • Actions admin aujourd’hui : " + d.adminActionsToday)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { load() }) { Text("Actualiser") }
            } }
        }
    }
}

@Composable
private fun UserPanel(repo: AdminControlRepository) {
    var query by remember { mutableStateOf("") }
    var user by remember { mutableStateOf<AdminUserDetailV2?>(null) }
    var note by remember { mutableStateOf("") }
    var postRestricted by remember { mutableStateOf(false) }
    var commentRestricted by remember { mutableStateOf(false) }
    var messageRestricted by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load() = scope.launch {
        busy = true
        repo.findUser(query).onSuccess {
            user = it
            note = it?.internalNote.orEmpty()
        }.onFailure { status = it.message }
        busy = false
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Panel("Recherche utilisateur", "UID, e-mail ou username.") {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Recherche") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { load() }, enabled = query.isNotBlank() && !busy) { Text("Charger la fiche") }
        } }
        item { user?.let { u ->
            Panel("Fiche complète", "Profil, activité, sécurité et historique.") {
                Text(u.displayName + "  @" + u.username)
                Text("UID : " + u.uid)
                Text("E-mail : " + u.email)
                Text("Statut : " + u.status + " • rôle : " + u.role)
                Text("Certifié : " + u.verified + " (" + u.verificationType.orEmpty() + ")")
                Text("Posts " + u.posts + " • commentaires " + u.comments + " • abonnés " + u.followers + " • abonnements " + u.following)
                Text("Signalements " + u.reports + " • sanctions " + u.sanctions)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Note interne") }, minLines = 3)
                SwitchRow("Restriction publications", postRestricted) { postRestricted = it }
                SwitchRow("Restriction commentaires", commentRestricted) { commentRestricted = it }
                SwitchRow("Restriction messages", messageRestricted) { messageRestricted = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { scope.launch { repo.setUserControls(u.uid, note, postRestricted, commentRestricted, messageRestricted).onSuccess { status = "Contrôles enregistrés." }.onFailure { status = it.message } } }) { Text("Enregistrer") }
                    OutlinedButton(onClick = { scope.launch { repo.suspendUser(u.uid, true).onSuccess { status = "Compte suspendu." }.onFailure { status = it.message }; load() } }) { Text("Suspendre") }
                    OutlinedButton(onClick = { scope.launch { repo.suspendUser(u.uid, false).onSuccess { status = "Compte restauré." }.onFailure { status = it.message }; load() } }) { Text("Restaurer") }
                }
            }
        } }
        item { status?.let { Text(it) } }
    }
}

@Composable
private fun SwitchRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun SanctionPanel(repo: AdminControlRepository) {
    var userId by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("warning") }
    var reason by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("24") }
    var proof by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf(emptyList<com.thehub.hb.data.repository.AdminSanctionV2>()) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun load() = scope.launch { repo.sanctions().onSuccess { rows = it }.onFailure { message = it.message } }
    LaunchedEffect(Unit) { load() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Panel("Sanctions avancées", "Warning, restriction, suspension et suspension permanente avec durée, motif et preuve.") {
            OutlinedTextField(userId, { userId = it }, Modifier.fillMaxWidth(), label = { Text("UID") })
            OutlinedTextField(type, { type = it }, Modifier.fillMaxWidth(), label = { Text("Type: warning / post_restriction / comment_restriction / messaging_restriction / suspend / permanent_suspension") })
            OutlinedTextField(reason, { reason = it }, Modifier.fillMaxWidth(), label = { Text("Motif") })
            OutlinedTextField(hours, { hours = it }, Modifier.fillMaxWidth(), label = { Text("Durée en heures, vide = permanente") })
            OutlinedTextField(proof, { proof = it }, Modifier.fillMaxWidth(), label = { Text("Preuve / contexte") })
            Button(onClick = { scope.launch {
                repo.applySanction(userId, type, reason, hours.toLongOrNull(), proof).onSuccess { message = "Sanction appliquée." }.onFailure { message = it.message }; load()
            } }, enabled = userId.isNotBlank() && reason.isNotBlank()) { Text("Appliquer") }
        } }
        item { message?.let { Text(it) } }
        items(rows.take(80), key = { it.id }) { s ->
            Panel(s.type + " • " + s.userId) {
                Text(s.reason)
                if (s.proof.isNotBlank()) Text("Preuve : " + s.proof)
                Text("Créée par " + s.createdBy + " • expire : " + (s.expiresAt ?: "jamais"))
            }
        }
    }
}

@Composable
private fun QueuePanel(repo: AdminControlRepository) {
    var rows by remember { mutableStateOf(emptyList<com.thehub.hb.data.repository.AdminQueueItemV2>()) }
    val scope = rememberCoroutineScope()
    fun load() = scope.launch { repo.moderationQueue().onSuccess { rows = it } }
    LaunchedEffect(Unit) { load() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { OutlinedButton(onClick = { load() }) { Text("Actualiser la file") } }
        items(rows, key = { it.id }) { item ->
            Panel(item.reason, "Cible " + item.targetId + " • reporter " + item.reporterId) {
                Text("État : " + item.status + " • priorité : " + item.priority)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("new", "in_progress", "action_taken", "rejected", "archived").forEach { state ->
                        OutlinedButton(onClick = { scope.launch { repo.updateQueue(item.id, state, if (state == "new") item.priority else "normal"); load() } }) { Text(state) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { scope.launch { repo.updateQueue(item.id, item.status, "high"); load() } }) { Text("High") }
                    OutlinedButton(onClick = { scope.launch { repo.updateQueue(item.id, item.status, "urgent"); load() } }) { Text("Urgent") }
                }
            }
        }
    }
}

@Composable
private fun PostModerationPanel(repo: AdminControlRepository) {
    var postId by remember { mutableStateOf("") }
    var hidden by remember { mutableStateOf(false) }
    var deleted by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    var pinned by remember { mutableStateOf(false) }
    var official by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Panel("Modération publications avancée", "Masquer, supprimer, verrouiller les commentaires, épingler ou marquer officiel.") {
            OutlinedTextField(postId, { postId = it }, Modifier.fillMaxWidth(), label = { Text("ID publication") })
            SwitchRow("Masquée", hidden) { hidden = it }
            SwitchRow("Supprimée par admin", deleted) { deleted = it }
            SwitchRow("Commentaires verrouillés", locked) { locked = it }
            SwitchRow("Épinglée", pinned) { pinned = it }
            SwitchRow("Publication officielle", official) { official = it }
            Button(onClick = { scope.launch { repo.moderatePost(postId, hidden, deleted, locked, pinned, official).onSuccess { message = "Modération enregistrée." }.onFailure { message = it.message } } }, enabled = postId.isNotBlank()) { Text("Appliquer") }
            message?.let { Text(it) }
        } }
    }
}

@Composable
private fun RolesPanel(repo: AdminControlRepository) {
    var rows by remember { mutableStateOf(emptyList<com.thehub.hb.data.repository.AdminRolePermissionV2>()) }
    var uid by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("admin") }
    var permissions by remember { mutableStateOf("dashboard,users,moderation,content,support,analytics") }
    val scope = rememberCoroutineScope()
    fun load() = scope.launch { repo.roles().onSuccess { rows = it } }
    LaunchedEffect(Unit) { load() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Panel("Rôles et permissions granulaires", "Rôles : superadmin, admin, moderator, support, analyst, community_manager.") {
            OutlinedTextField(uid, { uid = it }, Modifier.fillMaxWidth(), label = { Text("UID admin") })
            OutlinedTextField(role, { role = it }, Modifier.fillMaxWidth(), label = { Text("Rôle") })
            OutlinedTextField(permissions, { permissions = it }, Modifier.fillMaxWidth(), label = { Text("Permissions séparées par des virgules") })
            Button(onClick = { scope.launch { repo.updateRole(uid, role, permissions.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()).onSuccess { load() } } }, enabled = uid.isNotBlank()) { Text("Enregistrer") }
        } }
        items(rows, key = { it.uid }) { r -> Panel(r.displayName, r.email) { Text(r.uid + " • " + r.role + " • " + r.permissions.joinToString()) } }
    }
}

@Composable
private fun ReauthPanel(repo: AdminControlRepository) {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item { Panel("Réauthentification administrateur", "Google est demandé à nouveau avant une opération critique.") {
            Button(onClick = { scope.launch { repo.reauthenticateAdmin(context).onSuccess { message = "Session réauthentifiée." }.onFailure { message = it.message } } }) { Text("Réauthentifier maintenant") }
            message?.let { Text(it) }
            Spacer(Modifier.height(8.dp))
            Text("Les actions critiques doivent être précédées par cette validation dans la console.")
        } }
    }
}

@Composable
private fun SessionsPanel(repo: AdminControlRepository) {
    var rows by remember { mutableStateOf(emptyList<com.thehub.hb.data.repository.AdminSessionV2>()) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun load() = scope.launch { repo.sessions().onSuccess { rows = it }.onFailure { message = it.message } }
    LaunchedEffect(Unit) { load() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { OutlinedButton(onClick = { load() }) { Text("Actualiser les sessions") } }
        items(rows, key = { it.uid }) { s ->
            Panel(s.uid) {
                Text(if (s.active) "Session active récente" else "Hors ligne")
                Text("Dernière activité : " + (s.lastSeen ?: "—") + " • révocation : " + (s.revokedAt ?: "aucune"))
                OutlinedButton(onClick = { scope.launch { repo.revokeSessions(s.uid).onSuccess { message = "Sessions marquées révoquées." }.onFailure { message = it.message }; load() } }) { Text("Révoquer toutes les sessions") }
            }
        }
        item { message?.let { Text(it) } }
    }
}

@Composable
private fun AuditPanel(repo: AdminControlRepository) {
    var action by remember { mutableStateOf("") }
    var admin by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf(emptyList<com.thehub.hb.data.repository.AdminAuditV2>()) }
    val scope = rememberCoroutineScope()
    fun load() = scope.launch { repo.audits(action, admin, target).onSuccess { rows = it } }
    LaunchedEffect(Unit) { load() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Panel("Audit Center", "Filtrable par action, admin et cible. Les entrées ne sont pas modifiables.") {
            OutlinedTextField(action, { action = it }, Modifier.fillMaxWidth(), label = { Text("Action") })
            OutlinedTextField(admin, { admin = it }, Modifier.fillMaxWidth(), label = { Text("Admin UID") })
            OutlinedTextField(target, { target = it }, Modifier.fillMaxWidth(), label = { Text("Cible UID / ID") })
            Button(onClick = { load() }) { Text("Filtrer") }
        } }
        items(rows.take(200), key = { it.id }) { r -> Text(r.action + " • " + r.type + " • " + r.result + " • " + (r.targetId ?: "—"), modifier = Modifier.padding(vertical = 4.dp)) }
    }
}

@Composable
private fun NotificationsPanel(repo: AdminControlRepository) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var segment by remember { mutableStateOf("all") }
    var result by remember { mutableStateOf<String?>(null) }
    var rows by remember { mutableStateOf(emptyList<com.thehub.hb.data.repository.AdminBroadcastV2>()) }
    val scope = rememberCoroutineScope()
    fun load() = scope.launch { repo.broadcasts().onSuccess { rows = it } }
    LaunchedEffect(Unit) { load() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Panel("Notifications segmentées", "Segments : all, active_7d, admins. Les campagnes gardent leurs statistiques.") {
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Titre") })
            OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth(), label = { Text("Message") }, minLines = 4)
            OutlinedTextField(segment, { segment = it }, Modifier.fillMaxWidth(), label = { Text("Segment") })
            Button(onClick = { scope.launch { repo.sendNotification(title, body, segment).onSuccess { result = it.toString() + " destinataires" }.onFailure { result = it.message }; load() } }, enabled = title.isNotBlank() && body.isNotBlank()) { Text("Envoyer") }
            result?.let { Text(it) }
        } }
        items(rows, key = { it.id }) { r -> Text(r.title + " • " + r.recipients + " destinataires • " + r.opened + " ouverts", modifier = Modifier.padding(vertical = 4.dp)) }
    }
}

@Composable
private fun AnnouncementsPanel(repo: AdminControlRepository) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var segment by remember { mutableStateOf("all") }
    var priority by remember { mutableStateOf("normal") }
    var publishAt by remember { mutableStateOf("") }
    var expiresAt by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf(emptyList<com.thehub.hb.data.repository.AdminAnnouncementV2>()) }
    val scope = rememberCoroutineScope()
    fun load() = scope.launch { repo.announcements().onSuccess { rows = it } }
    LaunchedEffect(Unit) { load() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Panel("Annonces programmables", "Date = epoch millis. Laisse vide pour publier immédiatement.") {
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Titre") })
            OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth(), label = { Text("Message") }, minLines = 3)
            OutlinedTextField(segment, { segment = it }, Modifier.fillMaxWidth(), label = { Text("Segment") })
            OutlinedTextField(priority, { priority = it }, Modifier.fillMaxWidth(), label = { Text("Priorité") })
            OutlinedTextField(publishAt, { publishAt = it }, Modifier.fillMaxWidth(), label = { Text("publishAt epoch ms") })
            OutlinedTextField(expiresAt, { expiresAt = it }, Modifier.fillMaxWidth(), label = { Text("expiresAt epoch ms") })
            Button(onClick = { scope.launch { repo.scheduleAnnouncement(title, body, segment, priority, publishAt.toLongOrNull()?.let { Timestamp(Date(it)) }, expiresAt.toLongOrNull()?.let { Timestamp(Date(it)) }); load() } }) { Text("Programmer") }
        } }
        items(rows, key = { it.id }) { r -> Text(r.title + " • " + r.status + " • " + r.priority, modifier = Modifier.padding(vertical = 4.dp)) }
    }
}

@Composable
private fun VersionsPanel(repo: AdminControlRepository) {
    var version by remember { mutableStateOf<com.thehub.hb.data.repository.AdminVersionV2?>(null) }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var minCode by remember { mutableStateOf("") }
    var apk by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var force by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { repo.version().onSuccess {
        version = it; name = it.versionName; code = it.versionCode.toString(); minCode = it.minimumSupportedCode.toString(); apk = it.apkUrl; notes = it.releaseNotes; force = it.forceUpdate
    } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Panel("Gestion des versions", "Politique minimum, recommandée ou forcée.") {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Version") })
            OutlinedTextField(code, { code = it }, Modifier.fillMaxWidth(), label = { Text("VersionCode") })
            OutlinedTextField(minCode, { minCode = it }, Modifier.fillMaxWidth(), label = { Text("Minimum supporté") })
            OutlinedTextField(apk, { apk = it }, Modifier.fillMaxWidth(), label = { Text("URL APK") })
            OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), label = { Text("Release notes") }, minLines = 4)
            SwitchRow("Forcer la mise à jour", force) { force = it }
            Button(onClick = { scope.launch { repo.setVersion(com.thehub.hb.data.repository.AdminVersionV2(name, code.toLongOrNull() ?: 0, minCode.toLongOrNull() ?: 0, apk, notes, force, version?.updatedAt)) } }) { Text("Enregistrer") }
        } }
    }
}

@Composable
private fun FlagsPanel(repo: AdminControlRepository) {
    var key by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var enabled by remember { mutableStateOf(false) }
    var rows by remember { mutableStateOf(emptyList<AdminFeatureFlagV2>()) }
    val scope = rememberCoroutineScope()
    fun load() = scope.launch { repo.featureFlags().onSuccess { rows = it } }
    LaunchedEffect(Unit) { load() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Panel("Feature Flags", "Déploiement progressif sans publier une nouvelle version.") {
            OutlinedTextField(key, { key = it }, Modifier.fillMaxWidth(), label = { Text("Clé") })
            OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Description") })
            SwitchRow("Activée", enabled) { enabled = it }
            Button(onClick = { scope.launch { repo.setFeatureFlag(key, enabled, description); load() } }, enabled = key.isNotBlank()) { Text("Enregistrer") }
        } }
        items(rows, key = { it.key }) { r -> Text(r.key + " • " + if (r.enabled) "ON" else "OFF" + " • " + r.description, modifier = Modifier.padding(vertical = 4.dp)) }
    }
}

@Composable
private fun EmergencyPanel(repo: AdminControlRepository) {
    var value by remember { mutableStateOf(AdminEmergencyV2()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { repo.emergency().onSuccess { value = it } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Panel("Emergency Center", "Chaque bascule est journalisée et réservée au superadmin.") {
            SwitchRow("Maintenance", value.maintenance) { value = value.copy(maintenance = it) }
            SwitchRow("Nouvelles inscriptions", value.registrations) { value = value.copy(registrations = it) }
            SwitchRow("Publications", value.posts) { value = value.copy(posts = it) }
            SwitchRow("Commentaires", value.comments) { value = value.copy(comments = it) }
            SwitchRow("Messagerie", value.messaging) { value = value.copy(messaging = it) }
            SwitchRow("Notifications", value.notifications) { value = value.copy(notifications = it) }
            SwitchRow("Uploads", value.uploads) { value = value.copy(uploads = it) }
            OutlinedTextField(value.message, { value = value.copy(message = it) }, Modifier.fillMaxWidth(), label = { Text("Message d’urgence") })
            Button(onClick = { scope.launch { repo.setEmergency(value) } }) { Text("Appliquer") }
        } }
    }
}

@Composable
private fun InvestigationPanel(repo: AdminControlRepository) {
    var userId by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf(emptyList<com.thehub.hb.data.repository.AdminInvestigationEventV2>()) }
    val scope = rememberCoroutineScope()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Panel("Mode enquête", "Chronologie consolidée des audits, sanctions et signalements.") {
            OutlinedTextField(userId, { userId = it }, Modifier.fillMaxWidth(), label = { Text("UID utilisateur") })
            Button(onClick = { scope.launch { repo.investigation(userId).onSuccess { rows = it } } }) { Text("Charger la timeline") }
        } }
        items(rows) { e -> Text(e.action + " • " + e.source + " • " + (e.createdAt ?: "—"), modifier = Modifier.padding(vertical = 4.dp)) }
    }
}

@Composable
private fun AnalyticsPanel(repo: AdminControlRepository) {
    var data by remember { mutableStateOf<AdminAnalyticsV2?>(null) }
    val scope = rememberCoroutineScope()
    fun load() = scope.launch { repo.analyticsV2().onSuccess { data = it } }
    LaunchedEffect(Unit) { load() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Panel("Analytics produit", "DAU / WAU / MAU et rétention 7 jours.") {
            data?.let {
                Text("DAU : " + it.dau)
                Text("WAU : " + it.wau)
                Text("MAU : " + it.mau)
                Text("Rétention 7 j — cohorte récente active aujourd’hui : " + it.retention7d)
                Text("Publications : " + it.posts + " • commentaires : " + it.comments)
                Text("Signalements : " + it.reports + " • suspensions : " + it.suspensions)
            } ?: CircularProgressIndicator()
            OutlinedButton(onClick = { load() }) { Text("Actualiser") }
        } }
    }
}

@Composable
private fun AntiSpamPanel(repo: AdminControlRepository) {
    var data by remember { mutableStateOf(AdminAntiSpamConfigV2()) }
    var loaded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { repo.antiSpamConfig().onSuccess { data = it; loaded = true } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Panel("Anti-spam configurable", "Seuils d’activité et score bot centralisés.") {
            LongField("Posts / heure", data.postsPerHour) { data = data.copy(postsPerHour = it) }
            LongField("Commentaires / heure", data.commentsPerHour) { data = data.copy(commentsPerHour = it) }
            LongField("Messages / heure", data.messagesPerHour) { data = data.copy(messagesPerHour = it) }
            LongField("Tentatives login / heure", data.loginAttemptsPerHour) { data = data.copy(loginAttemptsPerHour = it) }
            LongField("Signalements / heure", data.reportsPerHour) { data = data.copy(reportsPerHour = it) }
            LongField("Fenêtre duplicats (min)", data.duplicateWindowMinutes) { data = data.copy(duplicateWindowMinutes = it) }
            LongField("Seuil score bot", data.botScoreThreshold) { data = data.copy(botScoreThreshold = it) }
            Button(onClick = { scope.launch { repo.setAntiSpamConfig(data) } }, enabled = loaded) { Text("Enregistrer les seuils") }
        } }
    }
}

@Composable
private fun LongField(label: String, value: Long, onChange: (Long) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(text, { text = it; onChange(it.toLongOrNull() ?: value) }, Modifier.fillMaxWidth(), label = { Text(label) })
}

@Composable
private fun SupportPanel(repo: AdminControlRepository) {
    var rows by remember { mutableStateOf(emptyList<com.thehub.hb.data.repository.AdminSupportTicketV2>()) }
    var ticketId by remember { mutableStateOf("") }
    var adminId by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun load() = scope.launch { repo.supportTickets().onSuccess { rows = it }.onFailure { status = it.message } }
    LaunchedEffect(Unit) { load() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Panel("Support", "Assignation, réponse, statut et historique par ticket.") {
            OutlinedTextField(ticketId, { ticketId = it }, Modifier.fillMaxWidth(), label = { Text("Ticket ID") })
            OutlinedTextField(adminId, { adminId = it }, Modifier.fillMaxWidth(), label = { Text("Admin assigné") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { scope.launch { repo.assignTicket(ticketId, adminId).onSuccess { status = "Assigné." }.onFailure { status = it.message }; load() } }) { Text("Assigner") }
                OutlinedButton(onClick = { load() }) { Text("Actualiser") }
            }
            OutlinedTextField(reply, { reply = it }, Modifier.fillMaxWidth(), label = { Text("Réponse") }, minLines = 3)
            Button(onClick = { scope.launch { repo.replyTicket(ticketId, reply).onSuccess { status = "Réponse enregistrée." }.onFailure { status = it.message }; load() } }) { Text("Répondre") }
            status?.let { Text(it) }
        } }
        items(rows, key = { it.id }) { t ->
            Panel(t.subject, "Ticket " + t.id + " • " + t.status + " • " + t.priority) {
                Text(t.message, maxLines = 4)
                Text("Utilisateur : " + t.userId + " • assigné : " + t.assignedAdminId.ifBlank { "non assigné" })
            }
        }
    }
}
