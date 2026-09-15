package com.thehub.hb.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.ui.theme.HubBlack
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite

private val REPORT_REASONS = listOf(
    "Spam" to "Messages ou contenus répétitifs et non sollicités",
    "Contenu inapproprié" to "Images ou propos violents, explicites ou choquants",
    "Harcèlement" to "Menaces, intimidation ou attaques ciblées",
    "Autre" to "Autre infraction aux règles de la communauté"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportBottomSheet(
    targetType: String, // "user" or "post"
    targetId: String,
    targetName: String = "",
    onDismiss: () -> Unit,
    onSubmitReport: (reason: String, details: String?) -> Unit,
    isSubmitting: Boolean = false,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedReason by remember { mutableStateOf(REPORT_REASONS.first().first) }
    var details by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HubCard,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(HubBorder)
            )
        },
        modifier = modifier.testTag("report_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(HubSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = HubError,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (targetType == "user") "Signaler ce profil" else "Signaler la publication",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = HubWhite
                    )
                    if (targetName.isNotBlank()) {
                        Text(
                            text = if (targetType == "user") targetName else "Publié par $targetName",
                            fontSize = 13.sp,
                            color = HubSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = HubBorder)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Pourquoi signalez-vous ce contenu ?",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = HubWhite
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                REPORT_REASONS.forEach { (reason, description) ->
                    val isSelected = selectedReason == reason
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) HubSurfaceElevated else HubBlack)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) HubWhite else HubBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedReason = reason }
                            .padding(12.dp)
                            .testTag("report_reason_$reason"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = reason,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HubWhite
                            )
                            Text(
                                text = description,
                                fontSize = 12.sp,
                                color = HubSecondary
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(HubWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = HubBlack,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Détails supplémentaires (optionnel)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = HubSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            HubTextField(
                value = details,
                onValueChange = { if (it.length <= 300) details = it },
                placeholder = "Décrivez brièvement la situation...",
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("report_details_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            HubButton(
                text = "Envoyer le signalement",
                onClick = { onSubmitReport(selectedReason, details) },
                isLoading = isSubmitting,
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_report_button")
            )
        }
    }
}

@Composable
fun BlockUserConfirmationDialog(
    username: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = HubError,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bloquer @$username ?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HubWhite
                )
            }
        },
        text = {
            Text(
                text = "Cette personne ne pourra plus voir vos publications ni votre profil, et vous ne verrez plus les siennes. Vous ne serez plus abonnés l'un à l'autre.",
                fontSize = 14.sp,
                color = HubSecondary,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isLoading,
                modifier = Modifier.testTag("confirm_block_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = HubError
                    )
                } else {
                    Text("Bloquer", color = HubError, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Annuler", color = HubSecondary)
            }
        },
        containerColor = HubCard,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.testTag("block_confirmation_dialog")
    )
}
