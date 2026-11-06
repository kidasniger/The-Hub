package com.thehub.hb.ui.imageviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.thehub.hb.ui.components.HubEmptyState
import com.thehub.hb.ui.theme.HubStroke
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubColors
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.utils.ImageSaver
import com.thehub.hb.ui.components.rememberRemoteImageUnavailable
import kotlinx.coroutines.launch

/**
 * Lecteur d'image plein écran — fond noir scénique, boutons verre (fermer /
 * enregistrer), zoom double-tap et pincement, état « photo indisponible ».
 */
@Composable
fun ImageViewerScreen(
    imageUrl: String,
    onClose: () -> Unit
) {
    val strings = LocalHubStrings.current
    val palette = LocalHubColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    val remoteImageUnavailable = rememberRemoteImageUnavailable(imageUrl)

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("image_viewer_screen")
    ) {
        // Image zoomable et déplaçable.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1.2f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale > 1f) {
                            val maxX = (size.width * (scale - 1)) / 2f
                            val maxY = (size.height * (scale - 1)) / 2f
                            offset = Offset(
                                x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                            )
                        } else {
                            offset = Offset.Zero
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (remoteImageUnavailable) {
                ImageViewerUnavailable()
            } else {
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = strings.lot3.postImageDesc,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = HubWhite,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    },
                    error = {
                        ImageViewerUnavailable()
                    }
                )
            }
        }

        // Fermer — pastille verre (haut gauche).
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopStart)
                .size(44.dp)
                .clip(CircleShape)
                .background(palette.scrim)
                .border(1.dp, HubStroke, CircleShape)
                .clickable(onClick = onClose)
                .testTag("image_viewer_close_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = strings.dialogClose,
                tint = HubWhite,
                modifier = Modifier.size(22.dp)
            )
        }

        // Enregistrer l'image — pastille verre (haut droite).
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopEnd)
                .size(44.dp)
                .clip(CircleShape)
                .background(palette.scrim)
                .border(1.dp, HubStroke, CircleShape)
                .clickable {
                    if (!isSaving && imageUrl.isNotBlank()) {
                        isSaving = true
                        coroutineScope.launch {
                            ImageSaver.saveImageToGallery(context, imageUrl)
                            isSaving = false
                        }
                    }
                }
                .testTag("image_viewer_save_button"),
            contentAlignment = Alignment.Center
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = HubWhite,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = strings.lot3.saveImageDesc,
                    tint = HubWhite,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/** État « photo indisponible » — HubEmptyState sur fond sombre. */
@Composable
private fun ImageViewerUnavailable() {
    val strings = LocalHubStrings.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        HubEmptyState(
            title = strings.lot3.photoUnavailableTitle,
            description = strings.lot3.photoUnavailableDesc,
            icon = Icons.Outlined.BrokenImage
        )
    }
}
