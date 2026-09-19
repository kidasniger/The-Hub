package com.thehub.hb.ui.createpost

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.thehub.hb.data.model.Post
import com.thehub.hb.ui.components.AppLogo
import com.thehub.hb.ui.components.LinkPreviewCard
import com.thehub.hb.ui.theme.HubBlack
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubDarkGray
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

@Composable
fun CreatePostScreen(
    viewModel: CreatePostViewModel,
    onNavigateBack: () -> Unit,
    onPostCreated: (Post) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val textBringIntoViewRequester = remember { BringIntoViewRequester() }
    var textFieldFocused by remember { mutableStateOf(false) }

    // Native Photo Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val bytes = com.thehub.hb.utils.ImageCompressor.compressImageFromUri(context, uri)
                if (bytes != null) {
                    viewModel.setImage(uri, bytes)
                }
            }
        }
    }

    LaunchedEffect(textFieldFocused) {
        if (textFieldFocused) {
            delay(100)
            textBringIntoViewRequester.bringIntoView()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HubBlack)
            .statusBarsPadding()
            .testTag("create_post_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("create_post_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = HubWhite
                    )
                }

                Text(
                    text = if (uiState.isEditMode) "Modifier la publication" else "Nouvelle publication",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = HubWhite
                )

                Button(
                    onClick = {
                        viewModel.publishPost { newPost ->
                            onPostCreated(newPost)
                        }
                    },
                    enabled = uiState.canPublish && !uiState.isLoading,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HubWhite,
                        contentColor = HubBlack,
                        disabledContainerColor = if (uiState.isLoading) HubWhite else HubSurfaceElevated,
                        disabledContentColor = if (uiState.isLoading) HubBlack else HubMuted
                    ),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("create_post_publish_button")
                ) {
                    if (uiState.isLoading) {
                        AppLogo(
                            size = 24.dp,
                            animated = true
                        )
                    } else {
                        Text(
                            text = if (uiState.isEditMode) "Enregistrer" else "Publier",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Error message banner
                if (uiState.errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(HubError.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            .border(1.dp, HubError.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = HubError,
                            fontSize = 13.sp
                        )
                    }
                }

                // Text Input Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    if (uiState.text.isEmpty()) {
                        Text(
                            text = "Quoi de neuf ?",
                            color = HubMuted,
                            fontSize = 18.sp,
                            lineHeight = 24.sp
                        )
                    }

                    BasicTextField(
                        value = uiState.text,
                        onValueChange = { viewModel.updateText(it) },
                        modifier = Modifier
                            .fillMaxSize()
                            .bringIntoViewRequester(textBringIntoViewRequester)
                            .onFocusChanged { textFieldFocused = it.isFocused }
                            .testTag("create_post_text_field"),
                        textStyle = TextStyle(
                            color = HubWhite,
                            fontSize = 18.sp,
                            lineHeight = 24.sp
                        ),
                        cursorBrush = SolidColor(HubWhite)
                    )
                }

                if (uiState.text.isNotBlank()) {
                    LinkPreviewCard(
                        text = uiState.text,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                // Character Counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "${uiState.charCount} / 500",
                        fontSize = 12.sp,
                        color = if (uiState.charCount >= 480) HubError else HubMuted,
                        fontWeight = if (uiState.charCount >= 480) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.testTag("create_post_char_counter")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Image Preview Card (if selected)
                if (uiState.selectedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(HubSurfaceElevated)
                            .border(1.dp, HubBorder, RoundedCornerShape(16.dp))
                            .testTag("create_post_image_preview")
                    ) {
                        AsyncImage(
                            model = uiState.selectedImageUri,
                            contentDescription = "Aperçu de l'image sélectionnée",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.FillWidth
                        )

                        // Remove Image Button
                        IconButton(
                            onClick = { viewModel.clearImage() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .size(32.dp)
                                .background(Color(0x99000000), CircleShape)
                                .testTag("create_post_remove_image")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Retirer l'image",
                                tint = HubWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Bottom Attachment Bar (hidden in edit mode since only text can be edited)
            if (!uiState.isEditMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HubSurfaceElevated)
                        .border(width = 1.dp, color = HubBorder)
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(HubDarkGray)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("create_post_pick_image_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = "Ajouter une image",
                            tint = HubWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.selectedImageUri != null) "Changer la photo" else "Ajouter une photo",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = HubWhite
                        )
                    }
                }
            }
        }
    }
}

private suspend fun readBytesFromUri(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        }
    } catch (_: Exception) {
        null
    }
}
