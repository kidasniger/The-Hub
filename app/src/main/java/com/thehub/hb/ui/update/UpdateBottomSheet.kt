package com.thehub.hb.ui.update

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import com.thehub.hb.ui.theme.HubOutline
import com.thehub.hb.ui.theme.HubSurface
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.model.AppUpdateInfo
import com.thehub.hb.data.remote.DownloadStatus
import com.thehub.hb.data.remote.formatFileSize
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubBorderLight
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubDarkGray
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubLightGray
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSuccess
import com.thehub.hb.ui.theme.HubSurfaceDark
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.utils.ApkInstaller

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateBottomSheet(
    viewModel: UpdateViewModel,
    onDismissRequest: () -> Unit = { viewModel.dismiss() }
) {
    val context = LocalContext.current
    val updateInfo by viewModel.updateInfo.collectAsState()
    val downloadStatus by viewModel.downloadStatus.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val currentUpdate = updateInfo
        if (currentUpdate == null) return@rememberLauncherForActivityResult

        if (!granted) {
            Toast.makeText(
                context,
                "Les notifications sont désactivées ; le téléchargement va continuer.",
                Toast.LENGTH_SHORT
            ).show()
        }
        viewModel.startDownload(currentUpdate)
    }

    val startDownloadWithNotificationPermission = {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.startDownload(updateInfo!!)
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    if (updateInfo != null) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            containerColor = HubSurfaceDark,
            scrimColor = Color.Black.copy(alpha = 0.75f),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 8.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(HubBorderLight)
                )
            }
        ) {
            UpdateContent(
                info = updateInfo!!,
                status = downloadStatus,
                onStartDownload = startDownloadWithNotificationPermission,
                onDismiss = onDismissRequest
            )
        }
    }
}

@Composable
fun UpdateContent(
    info: AppUpdateInfo,
    status: DownloadStatus,
    onStartDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .padding(bottom = 24.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(HubSurface)
                        .border(1.dp, HubOutline, MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Mise à jour",
                        tint = HubWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = "Mise à jour disponible",
                        color = HubWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "v${info.currentVersion}",
                            color = HubSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "→",
                            color = HubMuted,
                            fontSize = 12.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(HubWhite.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = info.latestVersion,
                                color = HubWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(HubCard)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fermer",
                    tint = HubSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Release notes card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(HubSurface)
                .border(1.dp, HubOutline, MaterialTheme.shapes.medium)
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Modifications apportées",
                        color = HubWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (info.apkSizeInBytes > 0) {
                        Text(
                            text = formatFileSize(info.apkSizeInBytes),
                            color = HubMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .verticalScroll(scrollState)
                ) {
                    val notesText = com.thehub.hb.data.remote.GitHubUpdateService()
                        .sanitizeReleaseNotes(info.releaseNotes)
                    Text(
                        text = notesText,
                        color = HubLightGray,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Download progress or Status indicator
        when (status) {
            is DownloadStatus.Downloading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Téléchargement en cours...",
                            color = HubWhite,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${status.progressPercent}%",
                            color = HubWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { status.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = HubWhite,
                        trackColor = HubBorderLight
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${formatFileSize(status.bytesDownloaded)} / ${formatFileSize(status.totalBytes)}",
                        color = HubMuted,
                        fontSize = 11.sp
                    )
                }
            }

            is DownloadStatus.Completed -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(HubSuccess.copy(alpha = 0.12f))
                        .border(1.dp, HubSuccess.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Prêt",
                        tint = HubSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Mise à jour prête à être installée.",
                        color = HubWhite,
                        fontSize = 12.sp
                    )
                }
            }

            is DownloadStatus.Failed -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(HubError.copy(alpha = 0.12f))
                        .border(1.dp, HubError.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Erreur",
                        tint = HubError,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = status.reason,
                        color = HubError,
                        fontSize = 12.sp
                    )
                }
            }

            is DownloadStatus.Idle -> {
                // No extra status text
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("update_later_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = HubLightGray
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, HubBorderLight)
            ) {
                Text(
                    text = "Plus tard",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }

            when (status) {
                is DownloadStatus.Downloading -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("update_downloading_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = HubSurfaceElevated,
                            disabledContentColor = HubMuted
                        )
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = HubWhite,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Téléchargement...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                is DownloadStatus.Completed -> {
                    Button(
                        onClick = {
                            if (!ApkInstaller.canRequestPackageInstalls(context)) {
                                Toast.makeText(
                                    context,
                                    "Veuillez autoriser l'installation d'applications inconnues pour The Hub",
                                    Toast.LENGTH_LONG
                                ).show()
                                ApkInstaller.openInstallPermissionSettings(context, status.file)
                            } else {
                                val success = ApkInstaller.installApk(context, status.file)
                                if (!success) {
                                    Toast.makeText(
                                        context,
                                        "Impossible de lancer l'installation.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("update_install_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HubWhite,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Installer la mise à jour",
                            modifier = Modifier.size(18.dp),
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Installer la mise à jour",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                else -> {
                    Button(
                        onClick = onStartDownload,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("update_download_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HubWhite,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Télécharger",
                            modifier = Modifier.size(18.dp),
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Télécharger",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
