package com.thehub.hb.ui.settings

import android.widget.Toast
import com.thehub.hb.ui.theme.HubSurface
import com.thehub.hb.ui.theme.HubOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.BuildConfig
import com.thehub.hb.ui.components.HubTextField
import com.thehub.hb.ui.theme.AppThemeMode
import com.thehub.hb.ui.theme.HubBlack
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
    onNavigateToBlockedUsers: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onSignedOut: () -> Unit,
    onAccountDeleted: () -> Unit,
    onCheckForUpdates: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val notifLikes by viewModel.notifLikes.collectAsState()
    val notifComments by viewModel.notifComments.collectAsState()
    val notifFollows by viewModel.notifFollows.collectAsState()
    val notifMessages by viewModel.notifMessages.collectAsState()
    val currentThemeMode by viewModel.currentThemeMode.collectAsState()

    val strings = com.thehub.hb.ui.theme.LocalHubStrings.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    var showThemeDialog by remember { mutableStateOf(false) }
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
                        contentDescription = strings.dialogClose,
                        tint = HubWhite
                    )
                }
                Text(
                    text = strings.settingsTitle,
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
                SettingsSectionTitle(strings.accountSection)
                SettingsCard {
                    SettingsActionRow(
                        icon = Icons.Default.Person,
                        title = strings.emailPlaceholder,
                        subtitle = uiState.userEmail.takeIf { it.isNotBlank() } ?: "—",
                        onClick = null,
                        testTag = "settings_row_email"
                    )
                }

                Spacer(Modifier.height(24.dp))

                SettingsSectionTitle(strings.privacySection)
                SettingsCard {
                    SettingsActionRow(
                        icon = Icons.Default.Block,
                        title = strings.blockedUsers,
                        subtitle = strings.blockedUsersSubtitle,
                        onClick = onNavigateToBlockedUsers,
                        testTag = "settings_row_blocked_users"
                    )
                }

                Spacer(Modifier.height(24.dp))

                SettingsSectionTitle(strings.appearanceSection)
                SettingsCard {
                    SettingsActionRow(
                        icon = Icons.Default.Palette,
                        title = strings.themeModeTitle,
                        subtitle = currentThemeMode.titleFr,
                        onClick = { showThemeDialog = true },
                        testTag = "settings_row_theme"
                    )
                }

                Spacer(Modifier.height(24.dp))

                SettingsSectionTitle(strings.notificationsSection)
                SettingsCard {
                    SettingsToggleRow(strings.notifLikes, strings.notifLikesSubtitle, notifLikes) {
                        viewModel.setNotifLikes(it)
                    }
                    Divider()
                    SettingsToggleRow(strings.notifComments, strings.notifCommentsSubtitle, notifComments) {
                        viewModel.setNotifComments(it)
                    }
                    Divider()
                    SettingsToggleRow(strings.notifFollows, strings.notifFollowsSubtitle, notifFollows) {
                        viewModel.setNotifFollows(it)
                    }
                    Divider()
                    SettingsToggleRow(strings.notifMessages, strings.notifMessagesSubtitle, notifMessages) {
                        viewModel.setNotifMessages(it)
                    }
                }

                Spacer(Modifier.height(24.dp))

                SettingsSectionTitle(strings.aboutSection)
                SettingsCard {
                    SettingsActionRow(
                        icon = Icons.Default.Description,
                        title = strings.termsTitle,
                        subtitle = strings.termsSubtitle,
                        onClick = onNavigateToTerms,
                        testTag = "settings_row_terms"
                    )
                    Divider()
                    SettingsActionRow(
                        icon = Icons.Default.Info,
                        title = strings.appVersionTitle,
                        subtitle = "v${BuildConfig.VERSION_NAME} (The Hub)",
                        onClick = null,
                        testTag = "settings_row_version"
                    )
                    if (onCheckForUpdates != null) {
                        Divider()
                        SettingsActionRow(
                            icon = Icons.Default.SystemUpdate,
                            title = strings.checkForUpdatesTitle,
                            subtitle = strings.checkForUpdatesSubtitle,
                            onClick = onCheckForUpdates,
                            testTag = "settings_row_check_update"
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                SettingsCard {
                    SettingsActionRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = strings.signOutTitle,
                        onClick = { showSignOutDialog = true },
                        testTag = "settings_row_sign_out"
                    )
                    Divider()
                    SettingsActionRow(
                        icon = Icons.Default.DeleteForever,
                        title = strings.deleteAccountTitle,
                        subtitle = strings.deleteAccountSubtitle,
                        titleColor = HubError,
                        onClick = { showDeleteStep1Dialog = true },
                        testTag = "settings_row_delete_account"
                    )
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(strings.dialogThemeTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HubWhite)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemeMode.entries.forEach { mode ->
                        val isSelected = mode == currentThemeMode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) HubSurfaceElevated else HubBlack.copy(alpha = 0.5f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    mode.titleFr,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) HubWhite else HubSecondary
                                )
                                Text(mode.descriptionFr, fontSize = 12.sp, color = HubMuted)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(strings.dialogClose, color = HubWhite)
                }
            }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(strings.dialogSignOutTitle, fontWeight = FontWeight.Bold, color = HubWhite) },
            text = { Text(strings.dialogSignOutMessage, color = HubSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut(onSignedOut)
                    },
                    modifier = Modifier.testTag("confirm_sign_out_button")
                ) {
                    Text(strings.signOutTitle, color = HubWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(strings.dialogCancel, color = HubSecondary)
                }
            }
        )
    }

    if (showDeleteStep1Dialog) {
        AlertDialog(
            onDismissRequest = { showDeleteStep1Dialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(strings.dialogDeleteAccountTitle, fontWeight = FontWeight.Bold, color = HubWhite) },
            text = { Text(strings.dialogDeleteAccountMessage, color = HubSecondary, lineHeight = 20.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteStep1Dialog = false
                        deleteConfirmInput = ""
                        showDeleteStep2Dialog = true
                    },
                    modifier = Modifier.testTag("confirm_delete_step1_button")
                ) {
                    Text(strings.dialogConfirm, color = HubError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteStep1Dialog = false }) {
                    Text(strings.dialogCancel, color = HubSecondary)
                }
            }
        )
    }

    if (showDeleteStep2Dialog) {
        val requiredWord = strings.deleteAccountConfirmationWord
        val isInputMatching = deleteConfirmInput.trim() == requiredWord

        AlertDialog(
            onDismissRequest = { if (!uiState.isDeletingAccount) showDeleteStep2Dialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(strings.dialogDeleteAccountTitle, fontWeight = FontWeight.Bold, color = HubWhite) },
            text = {
                Column {
                    Text(strings.deleteAccountConfirmationPrompt, color = HubSecondary)
                    Spacer(Modifier.height(12.dp))
                    HubTextField(
                        value = deleteConfirmInput,
                        onValueChange = { deleteConfirmInput = it },
                        placeholder = requiredWord,
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
                            Toast.makeText(context, "Compte supprimé", Toast.LENGTH_SHORT).show()
                            onAccountDeleted()
                        }
                    },
                    enabled = isInputMatching && !uiState.isDeletingAccount,
                    modifier = Modifier.testTag("confirm_delete_step2_button")
                ) {
                    if (uiState.isDeletingAccount) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = HubError)
                    } else {
                        Text(
                            strings.deleteAccountTitle,
                            color = if (isInputMatching) HubError else HubMuted,
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
                    Text(strings.dialogCancel, color = HubSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HubSurface, MaterialTheme.shapes.medium)
            .border(1.dp, HubOutline, MaterialTheme.shapes.medium)
    ) {
        content()
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(color = HubMuted.copy(alpha = 0.2f), thickness = 1.dp)
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
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = titleColor)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = HubSecondary)
            }
        }
        if (onClick != null) {
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
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = HubWhite)
            Text(subtitle, fontSize = 12.sp, color = HubSecondary)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = HubWhite,
                checkedTrackColor = HubWhite.copy(alpha = 0.4f),
                uncheckedThumbColor = HubSecondary,
                uncheckedTrackColor = HubSurfaceElevated
            )
        )
    }
}
