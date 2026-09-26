package com.thehub.hb.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubLightGray
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.utils.ImageSaver
import kotlinx.coroutines.launch

@Composable
fun PostMediaImage(
    imageUrl: String,
    contentDescription: String = "Image de la publication",
    cornerRadius: Dp = 12.dp,
    showSaveButton: Boolean = true,
    saveButtonTag: String = "post_save_image_button",
    onImageClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    maxHeight: Dp? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    val remoteImageUnavailable = rememberRemoteImageUnavailable(imageUrl)

    if (remoteImageUnavailable) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(HubSurfaceElevated)
                .border(1.dp, HubBorder, RoundedCornerShape(cornerRadius))
                .padding(vertical = 24.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.BrokenImage,
                    contentDescription = "Photo non disponible",
                    tint = HubSecondary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Photo non disponible",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HubWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Cette image a été supprimée ou n'est plus accessible.",
                    fontSize = 12.sp,
                    color = HubSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        SubcomposeAsyncImage(
        model = imageUrl,
        contentDescription = contentDescription,
        modifier = modifier
            .fillMaxWidth()
            .then(if (maxHeight != null) Modifier.heightIn(max = maxHeight) else Modifier)
            .clip(RoundedCornerShape(cornerRadius)),
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 260.dp)
                    .background(HubSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = HubWhite,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        success = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HubSurfaceElevated)
            ) {
                this@SubcomposeAsyncImage.SubcomposeAsyncImageContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (onImageClick != null) {
                                Modifier.clickable { onImageClick(imageUrl) }
                            } else Modifier
                        ),
                    contentScale = if (maxHeight != null) ContentScale.Crop else ContentScale.FillWidth
                )

                if (showSaveButton) {
                    IconButton(
                        onClick = {
                            if (!isSaving) {
                                isSaving = true
                                scope.launch {
                                    ImageSaver.saveImageToGallery(context, imageUrl)
                                    isSaving = false
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(34.dp)
                            .background(Color(0x99000000), CircleShape)
                            .testTag(saveButtonTag)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = HubWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Enregistrer l'image",
                                tint = HubWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        error = {
            // Photo deleted or unavailable -> clean fallback UI without modifying the post
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HubSurfaceElevated)
                    .border(1.dp, HubBorder, RoundedCornerShape(cornerRadius))
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BrokenImage,
                        contentDescription = "Photo non disponible",
                        tint = HubSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Photo non disponible",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HubWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cette image a été supprimée ou n'est plus accessible.",
                        fontSize = 12.sp,
                        color = HubSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    )
    }
}
