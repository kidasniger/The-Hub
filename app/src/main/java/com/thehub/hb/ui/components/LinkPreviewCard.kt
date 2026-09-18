package com.thehub.hb.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.thehub.hb.data.remote.LinkPreviewData
import com.thehub.hb.data.remote.LinkPreviewService
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.utils.extractFirstHttpUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val linkPreviewService = LinkPreviewService()

@Composable
fun LinkPreviewCard(
    text: String,
    modifier: Modifier = Modifier
) {
    val url = remember(text) { extractFirstHttpUrl(text) } ?: return
    val context = LocalContext.current
    var preview by remember(url) { mutableStateOf<LinkPreviewData?>(null) }
    var isLoading by remember(url) { mutableStateOf(true) }

    LaunchedEffect(url) {
        isLoading = true
        preview = withContext(Dispatchers.IO) {
            linkPreviewService.preview(url)
        }
        isLoading = false
    }

    val data = preview ?: returnIfLoaded(isLoading, modifier)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HubSurfaceElevated)
            .border(1.dp, HubBorder, RoundedCornerShape(12.dp))
            .clickable {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(data.finalUrl))
                    )
                }
            }
            .testTag("link_preview_card")
    ) {
        Column {
            if (!data.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = data.imageUrl,
                    contentDescription = data.title ?: "Aperçu du lien",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        tint = HubSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = data.siteName ?: Uri.parse(data.finalUrl).host.orEmpty(),
                        color = HubSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = "Ouvrir le lien",
                        tint = HubMuted,
                        modifier = Modifier.size(17.dp)
                    )
                }

                data.title?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = it,
                        color = HubWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                data.description?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        color = HubSecondary,
                        fontSize = 12.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = data.finalUrl,
                    color = HubMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (data.httpStatus == 404 || data.httpStatus == 410) {
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = "Lien indisponible",
                        color = HubSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun returnIfLoaded(
    isLoading: Boolean,
    modifier: Modifier
): @Composable (() -> Unit) {
    if (!isLoading) return { }
    return {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(HubCard)
                .border(1.dp, HubBorder, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = HubWhite,
                strokeWidth = 2.dp
            )
            Text(
                text = "Vérification du lien…",
                color = HubSecondary,
                fontSize = 12.sp
            )
        }
    }
}
