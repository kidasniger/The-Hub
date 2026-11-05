package com.thehub.hb.ui.createpost

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.thehub.hb.data.local.DataStoreManager
import com.thehub.hb.data.model.Post
import com.thehub.hb.ui.components.AppLogo
import com.thehub.hb.ui.components.HubErrorBanner
import com.thehub.hb.ui.components.LinkPreviewCard
import com.thehub.hb.ui.components.VideoLinkCard
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubGlass
import com.thehub.hb.ui.theme.HubGlassSoft
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubStroke
import com.thehub.hb.ui.theme.HubStrokeSoft
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubColors
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.hubPrimaryGradient
import com.thehub.hb.utils.VideoLinkDetector
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CreatePostScreen(
    viewModel: CreatePostViewModel,
    onNavigateBack: () -> Unit,
    onPostCreated: (Post) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val strings = LocalHubStrings.current
    val palette = LocalHubColors.current
    val dataStoreManager = remember(context) { DataStoreManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val textBringIntoViewRequester = remember { BringIntoViewRequester() }
    var textFieldFocused by remember { mutableStateOf(false) }
    var showVideoDialog by remember { mutableStateOf(false) }
    var videoDialogUrl by remember { mutableStateOf("") }
    var videoDialogError by remember { mutableStateOf<String?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!uiState.isEditMode && uiState.text.isBlank()) {
            val draft = dataStoreManager.postDraftText.first()
            if (draft.isNotBlank()) viewModel.updateText(draft)
        }
    }

    LaunchedEffect(uiState.text, uiState.isEditMode, uiState.isPublished) {
        if (!uiState.isEditMode && !uiState.isPublished) {
            delay(500)
            dataStoreManager.savePostDraft(uiState.text)
        }
    }

    // Native Photo Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val bytes = com.thehub.hb.utils.ImageCompressor.compressImageFromUri(context, uri)
                if (bytes != null) {
                    viewModel.setImage(uri, bytes)
                } else {
                    viewModel.showErrorMessage(strings.lot3.imagePrepareError)
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
            .statusBarsPadding()
            .imePadding()
            .testTag("create_post_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // .appbar — fermer (verre 40/14), titre 16.5/800, bouton primaire sm.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconBox(
                    modifier = Modifier.testTag("create_post_back_button"),
                    onClick = {
                        if (uiState.text.isNotBlank() || uiState.selectedImageUri != null || !uiState.videoUrl.isNullOrBlank()) {
                            showDiscardDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.dialogClose,
                        tint = HubWhite,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    text = if (uiState.isEditMode) strings.lot3.editPostTitle else strings.lot3.newPostTitle,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    color = HubWhite
                )

                Spacer(Modifier.weight(1f))

                // .btn.btn-primary.sm — dégradé de marque, rayon 12, 12.5/700 ;
                // désactivé : verre doux, texte atténué.
                val canPublish = uiState.canPublish && !uiState.isLoading
                Row(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (canPublish) {
                                Modifier.background(brush = hubPrimaryGradient())
                            } else {
                                Modifier
                                    .background(HubGlassSoft)
                                    .border(1.dp, HubStrokeSoft, RoundedCornerShape(12.dp))
                            }
                        )
                        .clickable(
                            enabled = canPublish
                        ) {
                            viewModel.publishPost { newPost ->
                                coroutineScope.launch {
                                    dataStoreManager.clearPostDraft()
                                    onPostCreated(newPost)
                                }
                            }
                        }
                        .padding(horizontal = 18.dp)
                        .testTag("create_post_publish_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.isLoading) {
                        AppLogo(size = 22.dp, animated = true)
                    } else {
                        Text(
                            text = if (uiState.isEditMode) strings.lot3.saveLabel else strings.publish,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canPublish) Color.White else HubMuted
                        )
                    }
                }
            }

            // .app-scroll — 6 20 24, gap 15.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                if (uiState.errorMessage != null) {
                    HubErrorBanner(message = uiState.errorMessage ?: "")
                }

                // Zone de texte — 15.5 / 1.55, compteur 10.5.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp)
                ) {
                    if (uiState.text.isEmpty()) {
                        Text(
                            text = strings.whatOnYourMind,
                            color = HubMuted,
                            fontSize = 15.5.sp,
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
                            fontSize = 15.5.sp,
                            lineHeight = 24.sp
                        ),
                        cursorBrush = SolidColor(palette.accent)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = if (uiState.charCount == 0) strings.lot3.readyToPublish else strings.lot3.charCountFormat.format(uiState.charCount),
                        fontSize = 10.5.sp,
                        color = HubMuted,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("create_post_char_counter")
                    )
                }

                if (uiState.text.isNotBlank() && uiState.videoUrl.isNullOrBlank()) {
                    LinkPreviewCard(text = uiState.text)
                }

                // Aperçu image — rayon 20, pastille de fermeture scrim.
                if (uiState.selectedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .testTag("create_post_image_preview")
                    ) {
                        AsyncImage(
                            model = uiState.selectedImageUri,
                            contentDescription = strings.lot3.selectedImageDesc,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.FillWidth
                        )

                        CloseOverlayButton(
                            onClick = { viewModel.clearImage() },
                            contentDescription = strings.lot3.removeImageDesc,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .testTag("create_post_remove_image")
                        )
                    }
                }

                if (!uiState.videoUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_post_video_preview")
                    ) {
                        VideoLinkCard(
                            videoUrl = uiState.videoUrl!!,
                            onClick = null
                        )
                        CloseOverlayButton(
                            onClick = { viewModel.clearVideoUrl() },
                            contentDescription = strings.lot3.removeVideoDesc,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .testTag("create_post_remove_video")
                        )
                    }
                }
            }

            // Barre d'attachements — chips verre de la maquette (Photo / Vidéo).
            if (!uiState.isEditMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AttachmentChip(
                        modifier = Modifier.testTag("create_post_pick_image_button"),
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = null,
                                tint = HubWhite,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = if (uiState.selectedImageUri != null) strings.changePhotoLabel else strings.addPhoto
                    )

                    AttachmentChip(
                        modifier = Modifier.testTag("create_post_add_video_link_button"),
                        onClick = {
                            videoDialogUrl = uiState.videoUrl.orEmpty()
                            videoDialogError = null
                            showVideoDialog = true
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.VideoLibrary,
                                contentDescription = strings.lot3.addVideoLinkDesc,
                                tint = HubWhite,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = strings.lot3.addVideoLinkLabel
                    )
                }
            }
        }

        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                containerColor = palette.bg1,
                title = {
                    Text(
                        strings.lot3.discardTitle,
                        color = HubWhite,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                text = {
                    Text(
                        strings.lot3.discardMessage,
                        color = HubSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDiscardDialog = false
                            coroutineScope.launch {
                                dataStoreManager.clearPostDraft()
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("create_post_discard_confirm")
                    ) {
                        Text(strings.lot3.leaveLabel, color = HubError, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardDialog = false }) {
                        Text(strings.lot3.continueLabel, color = HubWhite)
                    }
                }
            )
        }

        if (showVideoDialog) {
            AlertDialog(
                onDismissRequest = { showVideoDialog = false },
                containerColor = palette.bg1,
                title = {
                    Text(
                        strings.lot3.addVideoTitle,
                        color = HubWhite,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = strings.lot3.addVideoHelp,
                            color = HubSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = videoDialogUrl,
                            onValueChange = {
                                videoDialogUrl = it
                                videoDialogError = null
                            },
                            singleLine = true,
                            label = { Text(strings.lot3.videoLinkLabel) },
                            isError = videoDialogError != null,
                            supportingText = {
                                videoDialogError?.let { Text(it, color = HubError) }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = HubWhite,
                                unfocusedTextColor = HubWhite,
                                focusedBorderColor = palette.accent,
                                unfocusedBorderColor = HubStrokeSoft,
                                focusedLabelColor = palette.accent,
                                unfocusedLabelColor = HubMuted
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_post_video_link_input")
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val normalized = VideoLinkDetector.normalizeManualUrl(videoDialogUrl)
                            if (normalized == null) {
                                videoDialogError = strings.lot3.invalidVideoUrl
                            } else {
                                viewModel.setVideoUrl(normalized)
                                showVideoDialog = false
                            }
                        },
                        modifier = Modifier.testTag("create_post_video_link_confirm")
                    ) {
                        Text(strings.lot3.addLabel, color = palette.accent, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showVideoDialog = false }) {
                        Text(strings.dialogCancel, color = HubMuted)
                    }
                }
            )
        }
    }
}

/** Iconbtn verre 40/14 de la maquette (.appbar .iconbtn). */
@Composable
private fun GlassIconBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(HubGlass)
            .border(1.dp, HubStroke, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** Pastille de fermeture sur média : scrim + blur visuel (maquette 07). */
@Composable
private fun CloseOverlayButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(LocalHubColors.current.scrim)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = contentDescription,
            tint = HubWhite,
            modifier = Modifier.size(14.dp)
        )
    }
}

/** Chip d'attachement .chip : pilule verre, icône + label 12.5/600. */
@Composable
private fun AttachmentChip(
    onClick: () -> Unit,
    label: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(HubGlassSoft)
            .border(1.dp, HubStrokeSoft, RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        icon()
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = HubWhite
        )
    }
}
