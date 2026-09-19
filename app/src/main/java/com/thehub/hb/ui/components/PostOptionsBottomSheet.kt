package com.thehub.hb.ui.components

import android.content.ClipData
import com.thehub.hb.ui.theme.HubSurface
import com.thehub.hb.ui.theme.HubOutline
import androidx.compose.material3.MaterialTheme
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
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubDarkGray
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite

/**
 * Bottom Sheet displaying context options for a Post.
 * - Own post: "Modifier" (edit text only), "Supprimer" (delete post, subcollections, decrement postsCount).
 * - Other user's post: "Signaler" (report flow), "Copier le lien".
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
    val isMyPost = currentUserId != null && currentUserId == post.authorId

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HubCard,
        contentColor = HubWhite,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(HubDarkGray)
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
                text = "Options de la publication",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = HubWhite,
                modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
            )

            if (isMyPost) {
                // Modifier
                PostOptionRow(
                    icon = Icons.Outlined.Edit,
                    label = "Modifier",
                    sublabel = "Modifier le texte de votre publication",
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
                    label = "Supprimer",
                    sublabel = "Supprimer définitivement cette publication",
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
                    label = "Copier le lien",
                    sublabel = "Partager le lien vers cette publication",
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
                    label = "Signaler",
                    sublabel = "Signaler un contenu inapproprié ou abusif",
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

@Composable
private fun PostOptionRow(
    icon: ImageVector,
    label: String,
    sublabel: String,
    color: Color,
    testTag: String,
    onClick: () -> Unit
) {
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
                .background(if (color == HubError) HubError.copy(alpha = 0.12f) else HubSurfaceElevated),
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
 * Confirmation dialog shown before deleting a post.
 */
@Composable
fun DeletePostConfirmationDialog(
    isOpen: Boolean,
    isDeleting: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    AlertDialog(
        onDismissRequest = {
            if (!isDeleting) onDismiss()
        },
        containerColor = HubCard,
        title = {
            Text(
                text = "Supprimer la publication ?",
                color = HubWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Cette action est irréversible. La publication, ses commentaires et ses mentions J'aime seront définitivement supprimés.",
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
                        text = "Supprimer",
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
                    text = "Annuler",
                    color = HubMuted
                )
            }
        },
        modifier = Modifier.testTag("delete_post_dialog")
    )
}

/**
 * Utility helper to copy a post's link to the Android clipboard.
 */
fun copyPostLinkToClipboard(context: Context, postId: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Lien de la publication", "https://thehub.app/post/$postId")
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Lien copié dans le presse-papiers", Toast.LENGTH_SHORT).show()
}
