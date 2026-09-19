package com.thehub.hb.ui.feed.components

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
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.model.Post
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubDarkGray
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePostBottomSheet(
    post: Post,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit,
    onRepost: (Post) -> Unit
) {
    val context = LocalContext.current

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
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("share_post_bottom_sheet")
        ) {
            Text(
                text = "Partager la publication",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = HubWhite,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Option 1: Reposter sur mon profil
            ShareOptionItem(
                icon = Icons.Default.Repeat,
                title = "Reposter sur mon profil",
                subtitle = "Partage cette publication avec vos abonnés",
                enabled = true,
                onClick = {
                    onRepost(post)
                    onDismiss()
                },
                testTag = "share_option_repost"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Option 2: Envoyer en message (stub disabled)
            ShareOptionItem(
                icon = Icons.AutoMirrored.Outlined.Send,
                title = "Envoyer en message",
                subtitle = "Disponible bientôt",
                enabled = false,
                onClick = {},
                testTag = "share_option_message"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Option 3: Copier le lien
            ShareOptionItem(
                icon = Icons.Outlined.ContentCopy,
                title = "Copier le lien",
                subtitle = "thehub://post/${post.id}",
                enabled = true,
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Lien The Hub", "thehub://post/${post.id}")
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Lien copié dans le presse-papier !", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                testTag = "share_option_copy_link"
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ShareOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HubSurfaceElevated)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (enabled) HubDarkGray else HubBorder),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled) HubWhite else HubMuted,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) HubWhite else HubMuted
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = if (enabled) HubSecondary else HubMuted
            )
        }
    }
}
