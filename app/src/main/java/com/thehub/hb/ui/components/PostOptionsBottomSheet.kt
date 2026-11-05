package com.thehub.hb.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.model.Post
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubGlassStrong
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubColors
import com.thehub.hb.ui.theme.LocalHubStrings

/**
 * Bottom Sheet « Aurora Glass » des options d'une publication.
 * - Propre publication : « Modifier » (texte seul), « Supprimer ».
 * - Publication d'autrui : « Signaler », « Copier le lien ».
 * Feuille sur fond bg1 (base du verre), poignée stroke, icônes en pastilles verre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOptionsBottomSheet(
    post: Post,
    currentUserId: String?,
    onDismiss: () -> Unit,
    onEdit: (Post) -> Unit,
    onDelete: (Post) -> Unit,
    onReport: (Post) -> Unit,
    onCopyLink: (Post) -> Unit = { p ->
        // Default clipboard copy action
    },
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val strings = LocalHubStrings.current
    val palette = LocalHubColors.current
    val isMyPost = currentUserId != null && currentUserId == post.authorId

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.bg1,
        contentColor = HubWhite,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(palette.stroke)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("post_options_bottom_sheet")
        ) {
            Text(
                text = strings.lot3.postOptionsTitle,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HubWhite,
                letterSpacing = (-0.2).sp,
                modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
            )

            if (isMyPost) {
                // Modifier
                PostOptionRow(
                    icon = Icons.Outlined.Edit,
                    label = strings.lot3.editPostOption,
                    sublabel = strings.lot3.editPostOptionSub,
                    color = HubWhite,
                    testTag = "post_option_edit",
                    onClick = {
                        onDismiss()
                        onEdit(post)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Supprimer
                PostOptionRow(
                    icon = Icons.Outlined.Delete,
                    label = strings.lot3.deletePostOption,
                    sublabel = strings.lot3.deletePostOptionSub,
                    color = HubError,
                    testTag = "post_option_delete",
                    onClick = {
                        onDismiss()
                        onDelete(post)
                    }
                )
            } else {
                // Copier le lien
                PostOptionRow(
                    icon = Icons.Outlined.ContentCopy,
                    label = strings.lot3.copyLinkOption,
                    sublabel = strings.lot3.copyLinkOptionSub,
                    color = HubWhite,
                    testTag = "post_option_copy_link",
                    onClick = {
                        onDismiss()
                        onCopyLink(post)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Signaler
                PostOptionRow(
                    icon = Icons.Outlined.Flag,
                    label = strings.reportPost,
                    sublabel = strings.lot3.reportPostOptionSub,
                    color = HubError,
                    testTag = "post_option_report",
                    onClick = {
                        onDismiss()
                        onReport(post)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Rangée d'option : pastille de verre 42 + label 15/700 + sous-label 12. */
@Composable
private fun PostOptionRow(
    icon: ImageVector,
    label: String,
    sublabel: String,
    color: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val isDanger = color == HubError
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (isDanger) HubError.copy(alpha = 0.12f) else HubGlassStrong
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sublabel,
                fontSize = 12.sp,
                color = HubMuted
            )
        }
    }
}

/**
 * Dialogue de confirmation avant suppression d'une publication.
 */
@Composable
fun DeletePostConfirmationDialog(
    isOpen: Boolean,
    isDeleting: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalHubStrings.current
    if (!isOpen) return

    AlertDialog(
        onDismissRequest = {
            if (!isDeleting) onDismiss()
        },
        containerColor = LocalHubColors.current.bg1,
        title = {
            Text(
                text = strings.lot3.deletePostDialogTitle,
                color = HubWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Text(
                text = strings.lot3.deletePostDialogMessage,
                color = HubSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = HubError
                ),
                modifier = Modifier.testTag("confirm_delete_post_button")
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = HubError,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = strings.lot3.deletePostOption,
                        fontWeight = FontWeight.Bold,
                        color = HubError
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = HubMuted
                ),
                modifier = Modifier.testTag("cancel_delete_post_button")
            ) {
                Text(
                    text = strings.dialogCancel,
                    color = HubMuted
                )
            }
        },
        modifier = Modifier.testTag("delete_post_dialog")
    )
}

/**
 * Utility helper to copy a post's link to the Android clipboard.
 * [copiedMessage] est le libellé du toast, fourni par Strings.kt depuis l'appelant.
 */
fun copyPostLinkToClipboard(
    context: Context,
    postId: String,
    copiedMessage: String = "Lien copié dans le presse-papiers"
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Lien de la publication", "https://thehub.app/post/$postId")
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
}
