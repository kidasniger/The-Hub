package com.thehub.hb.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.thehub.hb.BuildConfig
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import com.thehub.hb.ui.theme.AppLanguage
import com.thehub.hb.ui.theme.AppThemeMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubTextField
import com.thehub.hb.ui.theme.HubBlack
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToResetPassword: () -> Unit,
    onNavigateToBlockedUsers: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onSignedOut: () -> Unit,
    onAccountDeleted: () -> Unit,
    onCheckForUpdates: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val notifLikes by viewModel.notifLikes.collectAsState()
    val notifComments by viewModel.notifComments.collectAsState()
    val notifFollows by viewModel.notifFollows.collectAsState()
    val notifMessages by viewModel.notifMessages.collectAsState()
    val currentThemeMode by viewModel.currentThemeMode.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteStep1Dialog by remember { mutableStateOf(false) }
    var showDeleteStep2Dialog by remember { mutableStateOf(false) }
    var deleteConfirmInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState.deleteError) {
        uiState.deleteError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDeleteError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = HubBlack,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("settings_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = HubWhite
                    )
                }

                Text(
                    text = "Paramètres",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HubWhite,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Section 1: Compte
                SettingsSectionTitle(title = "Compte")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(HubCard)
                ) {
                    SettingsActionRow(
                        icon = Icons.Default.Person,
                        title = "Adresse email",
                        subtitle = uiState.userEmail.takeIf { it.isNotBlank() } ?: "Non définie",
                        onClick = null,
                        showArrow = false,
                        testTag = "settings_row_email"
                    )

                    HorizontalDivider(color = HubBorder, thickness = 1.dp)

                    SettingsActionRow(
                        icon = Icons.Default.LockReset,
                        title = "Changer le mot de passe",
                        subtitle = "Recevoir un lien de réinitialisation",
                        onClick = onNavigateToResetPassword,
                        showArrow = true,
                        testTag = "settings_row_change_password"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section 2: Confidentialité
                SettingsSectionTitle(title = "Confidentialité")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(HubCard)
                ) {
                    SettingsActionRow(
                        icon = Icons.Default.Block,
                        title = "Utilisateurs bloqués",
                        subtitle = "Gérer les profils que vous avez bloqués",
                        onClick = onNavigateToBlockedUsers,
                        showArrow = true,
                        testTag = "settings_row_blocked_users"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section 3: Apparence & Langue
                SettingsSectionTitle(title = "Apparence & Langue")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(HubCard)
                ) {
                    SettingsActionRow(
                        icon = Icons.Default.Palette,
                        title = "Mode d'affichage (Thème)",
                        subtitle = when (currentThemeMode) {
                            AppThemeMode.DARK -> "Mode Sombre"
                            AppThemeMode.LIGHT -> "Mode Clair"
                            AppThemeMode.GLASS -> "Effet Glass"
                            AppThemeMode.SYSTEM -> "Système (Automatique)"
                        },
                        onClick = { showThemeDialog = true },
                        showArrow = true,
                        testTag = "settings_row_theme"
                    )

                    HorizontalDivider(color = HubBorder, thickness = 1.dp)

                    SettingsActionRow(
                        icon = Icons.Default.Language,
                        title = "Langue de l'application",
                        subtitle = "${currentLanguage.flagEmoji} ${currentLanguage.displayName}",
                        onClick = { showLanguageDialog = true },
                        showArrow = true,
                        testTag = "settings_row_language"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section 4: Notifications
                SettingsSectionTitle(title = "Notifications")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(HubCard)
                ) {
                    SettingsToggleRow(
                        title = "Mentions \"J'aime\"",
                        subtitle = "Lorsqu'un utilisateur aime vos publications",
                        checked = notifLikes,
                        onCheckedChange = { viewModel.setNotifLikes(it) },
                        testTag = "settings_switch_likes"
                    )

                    HorizontalDivider(color = HubBorder, thickness = 1.dp)

                    SettingsToggleRow(
                        title = "Commentaires",
                        subtitle = "Lorsqu'un utilisateur commente vos posts",
                        checked = notifComments,
                        onCheckedChange = { viewModel.setNotifComments(it) },
                        testTag = "settings_switch_comments"
                    )

                    HorizontalDivider(color = HubBorder, thickness = 1.dp)

                    SettingsToggleRow(
                        title = "Nouveaux abonnés",
                        subtitle = "Lorsqu'un utilisateur commence à vous suivre",
                        checked = notifFollows,
                        onCheckedChange = { viewModel.setNotifFollows(it) },
                        testTag = "settings_switch_follows"
                    )

                    HorizontalDivider(color = HubBorder, thickness = 1.dp)

                    SettingsToggleRow(
                        title = "Messages directs",
                        subtitle = "À la réception d'un nouveau message privé",
                        checked = notifMessages,
                        onCheckedChange = { viewModel.setNotifMessages(it) },
                        testTag = "settings_switch_messages"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section 4: À propos
                SettingsSectionTitle(title = "À propos")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(HubCard)
                ) {
                    SettingsActionRow(
                        icon = Icons.Default.Description,
                        title = "Conditions d'utilisation & Confidentialité",
                        subtitle = "Consulter les règles d'utilisation",
                        onClick = onNavigateToTerms,
                        showArrow = true,
                        testTag = "settings_row_terms"
                    )

                    HorizontalDivider(color = HubBorder, thickness = 1.dp)

                    SettingsActionRow(
                        icon = Icons.Default.Info,
                        title = "Version de l'application",
                        subtitle = "v${BuildConfig.VERSION_NAME} (The Hub)",
                        onClick = null,
                        showArrow = false,
                        testTag = "settings_row_version"
                    )

                    if (onCheckForUpdates != null) {
                        HorizontalDivider(color = HubBorder, thickness = 1.dp)

                        SettingsActionRow(
                            icon = Icons.Default.SystemUpdate,
                            title = "Vérifier les mises à jour",
                            subtitle = "Rechercher la dernière version GitHub",
                            onClick = onCheckForUpdates,
                            showArrow = true,
                            testTag = "settings_row_check_update"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Section 5: Déconnexion & Danger
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(HubCard)
                ) {
                    SettingsActionRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = "Se déconnecter",
                        titleColor = HubWhite,
                        onClick = { showSignOutDialog = true },
                        showArrow = false,
                        testTag = "settings_row_sign_out"
                    )

                    HorizontalDivider(color = HubBorder, thickness = 1.dp)

                    SettingsActionRow(
                        icon = Icons.Default.DeleteForever,
                        title = "Supprimer le compte",
                        subtitle = "Action irréversible — supprime toutes vos données",
                        titleColor = HubError,
                        onClick = { showDeleteStep1Dialog = true },
                        showArrow = true,
                        testTag = "settings_row_delete_account"
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = HubCard,
            title = {
                Text(
                    text = "Mode d'affichage",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HubWhite
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        val isSelected = mode == currentThemeMode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) HubSurfaceElevated else HubBlack.copy(alpha = 0.5f))
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.titleFr,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) HubWhite else HubSecondary
                                )
                                Text(
                                    text = mode.descriptionFr,
                                    fontSize = 12.sp,
                                    color = HubMuted
                                )
                            }
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = HubWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Fermer", color = HubWhite)
                }
            }
        )
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = HubCard,
            title = {
                Text(
                    text = "Langue de l'application",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HubWhite
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val isSelected = lang == currentLanguage
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) HubSurfaceElevated else HubBlack.copy(alpha = 0.5f))
                                .clickable {
                                    viewModel.setLanguage(lang)
                                    showLanguageDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lang.flagEmoji,
                                fontSize = 22.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = lang.displayName,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) HubWhite else HubSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = HubWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Fermer", color = HubWhite)
                }
            }
        )
    }

    // Sign Out Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(
                    text = "Se déconnecter ?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HubWhite
                )
            },
            text = {
                Text(
                    text = "Vous devrez saisir vos identifiants pour vous reconnecter à The Hub.",
                    fontSize = 14.sp,
                    color = HubSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut(onSignedOut)
                    },
                    modifier = Modifier.testTag("confirm_sign_out_button")
                ) {
                    Text("Déconnexion", color = HubWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Annuler", color = HubSecondary)
                }
            },
            containerColor = HubCard,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("sign_out_dialog")
        )
    }

    // Delete Step 1 Dialog
    if (showDeleteStep1Dialog) {
        AlertDialog(
            onDismissRequest = { showDeleteStep1Dialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = HubError,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Supprimer votre compte ?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = HubWhite
                    )
                }
            },
            text = {
                Text(
                    text = "Attention : cette action est définitive et irréversible. Votre profil, vos publications, vos messages et vos abonnements seront définitivement effacés.",
                    fontSize = 14.sp,
                    color = HubSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteStep1Dialog = false
                        deleteConfirmInput = ""
                        showDeleteStep2Dialog = true
                    },
                    modifier = Modifier.testTag("confirm_delete_step1_button")
                ) {
                    Text("Continuer", color = HubError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteStep1Dialog = false }) {
                    Text("Annuler", color = HubSecondary)
                }
            },
            containerColor = HubCard,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("delete_step1_dialog")
        )
    }

    // Delete Step 2 Dialog (Text Confirmation)
    if (showDeleteStep2Dialog) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isDeletingAccount) showDeleteStep2Dialog = false
            },
            title = {
                Text(
                    text = "Confirmation finale",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HubWhite
                )
            },
            text = {
                Column {
                    Text(
                        text = "Veuillez taper \"SUPPRIMER\" en majuscules pour confirmer la suppression définitive :",
                        fontSize = 14.sp,
                        color = HubSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HubTextField(
                        value = deleteConfirmInput,
                        onValueChange = { deleteConfirmInput = it },
                        placeholder = "SUPPRIMER",
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_account_confirm_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount {
                            showDeleteStep2Dialog = false
                            onAccountDeleted()
                        }
                    },
                    enabled = deleteConfirmInput == "SUPPRIMER" && !uiState.isDeletingAccount,
                    modifier = Modifier.testTag("confirm_delete_step2_button")
                ) {
                    if (uiState.isDeletingAccount) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = HubError
                        )
                    } else {
                        Text(
                            text = "Supprimer définitivement",
                            color = if (deleteConfirmInput == "SUPPRIMER") HubError else HubMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteStep2Dialog = false },
                    enabled = !uiState.isDeletingAccount
                ) {
                    Text("Annuler", color = HubSecondary)
                }
            },
            containerColor = HubCard,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("delete_step2_dialog")
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = HubSecondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: androidx.compose.ui.graphics.Color = HubWhite,
    onClick: (() -> Unit)?,
    showArrow: Boolean = false,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (titleColor == HubError) HubError else HubSecondary,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = HubSecondary
                )
            }
        }

        if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = HubMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = HubWhite
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = HubSecondary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = HubWhite,
                checkedTrackColor = HubWhite.copy(alpha = 0.4f),
                uncheckedThumbColor = HubSecondary,
                uncheckedTrackColor = HubSurfaceElevated
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
