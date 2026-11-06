package com.thehub.hb.ui.profile

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.thehub.hb.ui.components.HubSkeletonLine
import com.thehub.hb.ui.components.WheelDatePickerBottomSheet
import com.thehub.hb.ui.components.formatBirthdate
import com.thehub.hb.ui.components.parseBirthdateOrDefault
import com.thehub.hb.ui.theme.HubGlass
import com.thehub.hb.ui.theme.HubGlassStrong
import com.thehub.hb.ui.theme.HubStroke
import com.thehub.hb.ui.theme.HubStrokeSoft
import com.thehub.hb.ui.theme.HubTextSecondary
import com.thehub.hb.ui.theme.HubTextTertiary
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubColors
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.hubPrimaryGradient

/**
 * Écran 18 — Modifier le profil : appbar (fermer + Enregistrer), avatar 86 avec
 * pastille photo, champs verre (nom, pseudo, bio avec compteur, date de naissance).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel,
    onNavigateBack: () -> Unit,
    onProfileUpdated: () -> Unit = onNavigateBack,
    modifier: Modifier = Modifier
) {
    val strings = LocalHubStrings.current
    val palette = LocalHubColors.current
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.onImageSelected(context, uri)
    }

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val initialLocalDate = remember(uiState.birthdate) {
            parseBirthdateOrDefault(uiState.birthdate)
        }
        WheelDatePickerBottomSheet(
            initialDate = initialLocalDate,
            onDateConfirmed = { confirmedDate ->
                viewModel.onBirthdateChanged(formatBirthdate(confirmedDate))
            },
            onDismiss = { showDatePicker = false }
        )
    }

    LaunchedEffect(uiState.generalError) {
        uiState.generalError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("edit_profile_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // .appbar — fermer + titre + Enregistrer (dégradé sm).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(HubGlass)
                        .border(1.dp, HubStroke, RoundedCornerShape(14.dp))
                        .clickable(onClick = onNavigateBack)
                        .testTag("edit_profile_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = strings.dialogClose,
                        tint = HubWhite,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Text(
                    text = strings.editProfile,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    color = HubWhite,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )

                EditProfileSaveButton(
                    enabled = uiState.isValid && !uiState.isSaving,
                    isLoading = uiState.isSaving,
                    onClick = {
                        viewModel.saveProfile {
                            onProfileUpdated()
                        }
                    },
                    modifier = Modifier.testTag("edit_profile_save_button")
                )
            }

            if (uiState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(HubGlass)
                            .align(Alignment.CenterHorizontally)
                    )
                    HubSkeletonLine(height = 12.dp, widthFraction = 0.25f)
                    HubSkeletonLine(height = 52.dp, widthFraction = 1f)
                    HubSkeletonLine(height = 12.dp, widthFraction = 0.35f)
                    HubSkeletonLine(height = 52.dp, widthFraction = 1f)
                    HubSkeletonLine(height = 12.dp, widthFraction = 0.2f)
                    HubSkeletonLine(height = 80.dp, widthFraction = 1f)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .imePadding()
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(17.dp)
                ) {
                    // Avatar 86 + pastille appareil photo (sendbtn 32/12).
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(top = 4.dp)) {
                            val imageModel = uiState.selectedImageUri ?: uiState.photoUrl

                            @Composable
                            fun DefaultPersonPlaceholder() {
                                Box(
                                    modifier = Modifier
                                        .size(86.dp)
                                        .clip(CircleShape)
                                        .background(HubGlassStrong)
                                        .border(2.dp, HubStrokeSoft, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = HubTextTertiary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    }
                                    .testTag("edit_profile_avatar_picker")
                            ) {
                                if (imageModel != null) {
                                    SubcomposeAsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(imageModel)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = strings.lot5.profileAvatarDesc,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(86.dp),
                                        loading = { DefaultPersonPlaceholder() },
                                        success = { SubcomposeAsyncImageContent() },
                                        error = { DefaultPersonPlaceholder() }
                                    )
                                } else {
                                    DefaultPersonPlaceholder()
                                }

                                // Pastille photo (32 / rayon 12, dégradé).
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(brush = hubPrimaryGradient())
                                        .border(
                                            2.dp,
                                            palette.card,
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CameraAlt,
                                        contentDescription = strings.changePhotoLabel,
                                        tint = HubWhite,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = strings.changePhotoLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent,
                            modifier = Modifier
                                .padding(top = 9.dp)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                        )
                    }

                    // Nom complet.
                    EditProfileField(
                        label = strings.fullNameLabel,
                        value = uiState.displayName,
                        onValueChange = { viewModel.onDisplayNameChanged(it) },
                        placeholder = strings.fullNamePlaceholder,
                        errorMessage = uiState.displayNameError,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = HubTextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_display_name")
                    )

                    // Nom d'utilisateur.
                    EditProfileField(
                        label = strings.usernameLabel,
                        value = uiState.username,
                        onValueChange = { viewModel.onUsernameChanged(it) },
                        placeholder = strings.usernamePlaceholder,
                        errorMessage = uiState.usernameError,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Link,
                                contentDescription = null,
                                tint = HubTextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_username")
                    )

                    // Bio (textarea + compteur à droite).
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(
                            text = strings.bioLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp,
                            color = HubTextSecondary
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(palette.field)
                                .border(1.dp, palette.strokeSoft, RoundedCornerShape(18.dp))
                                .padding(horizontal = 16.dp, vertical = 13.dp)
                                .testTag("edit_profile_bio")
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = HubTextTertiary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    BasicTextField(
                                        value = uiState.bio,
                                        onValueChange = { viewModel.onBioChanged(it) },
                                        textStyle = TextStyle(
                                            fontSize = 13.sp,
                                            lineHeight = 20.sp,
                                            color = HubWhite
                                        ),
                                        cursorBrush = SolidColor(palette.accent),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(64.dp)
                                    )
                                    if (uiState.bio.isEmpty()) {
                                        Text(
                                            text = strings.bioPlaceholder,
                                            fontSize = 13.sp,
                                            color = HubTextTertiary
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = strings.bioCounterFormat.format(uiState.bio.length),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (uiState.bio.length > 150) {
                                palette.error
                            } else {
                                HubTextTertiary
                            },
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Date de naissance (champ cliquable → roue).
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(
                            text = strings.birthdateLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp,
                            color = HubTextSecondary
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(palette.field)
                                .border(1.dp, palette.strokeSoft, RoundedCornerShape(18.dp))
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .testTag("edit_profile_birthdate")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    tint = HubTextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = if (uiState.birthdate.isNotBlank()) {
                                        uiState.birthdate
                                    } else {
                                        strings.birthdatePlaceholder
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (uiState.birthdate.isNotBlank()) {
                                        HubWhite
                                    } else {
                                        HubTextTertiary
                                    }
                                )
                            }
                        }
                        Text(
                            text = strings.birthdatePrivate,
                            fontSize = 10.5.sp,
                            color = HubTextTertiary
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

/** Bouton sm dégradé de l'appbar : Enregistrer (spinner pendant l'envoi). */
@Composable
private fun EditProfileSaveButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush = hubPrimaryGradient(), shape = shape)
            .clickable(enabled = enabled && !isLoading, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = HubWhite,
                strokeWidth = 2.dp,
                modifier = Modifier.size(15.dp)
            )
        } else {
            Text(
                text = LocalHubStrings.current.lot3.saveLabel,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = HubWhite
            )
        }
    }
}

/** Champ .field + .input de la maquette : label 12/700 + champ verre à icône. */
@Composable
private fun EditProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    errorMessage: String?,
    leadingIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalHubColors.current
    val focused = remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp,
            color = HubTextSecondary
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(palette.field, shape)
                .border(
                    width = 1.dp,
                    color = if (focused.value) {
                        palette.accent.copy(alpha = 0.65f)
                    } else {
                        palette.strokeSoft
                    },
                    shape = shape
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            leadingIcon()
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = HubWhite
                    ),
                    cursorBrush = SolidColor(palette.accent),
                    modifier = Modifier.fillMaxWidth()
                )
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 14.sp,
                        color = HubTextTertiary,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }
            }
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.error
            )
        }
    }
}
