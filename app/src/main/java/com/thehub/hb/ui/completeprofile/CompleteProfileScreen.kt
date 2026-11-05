package com.thehub.hb.ui.completeprofile

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubErrorBanner
import com.thehub.hb.ui.components.HubTextField
import com.thehub.hb.ui.components.WheelDatePickerBottomSheet
import com.thehub.hb.ui.components.formatBirthdate
import com.thehub.hb.ui.components.parseBirthdateOrDefault
import com.thehub.hb.ui.theme.HubAccent
import com.thehub.hb.ui.theme.HubFabGlow
import com.thehub.hb.ui.theme.HubGlassStrong
import com.thehub.hb.ui.theme.HubHighlight
import com.thehub.hb.ui.theme.HubStrokeSoft
import com.thehub.hb.ui.theme.HubSuccess
import com.thehub.hb.ui.theme.HubTextPrimary
import com.thehub.hb.ui.theme.HubTextSecondary
import com.thehub.hb.ui.theme.HubTextTertiary
import com.thehub.hb.ui.theme.HubThemeBackground
import com.thehub.hb.ui.theme.LocalHubColors
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.hubPrimaryGradient

// Progression de la maquette : « Étape 2 sur 3 », barre à 66 %.
private const val PROFILE_STEP_INDEX = 2
private const val PROFILE_STEP_TOTAL = 3
private const val PROFILE_STEP_PROGRESS = 0.66f

@Composable
fun CompleteProfileScreen(
    viewModel: CompleteProfileViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToFeed: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val strings = LocalHubStrings.current
    var showDatePicker by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.onImageSelected(context, uri)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CompleteProfileNavigationEvent.NavigateToFeed -> onNavigateToFeed()
            }
        }
    }

    if (showDatePicker) {
        val initialLocalDate = remember(uiState.birthdate) {
            parseBirthdateOrDefault(uiState.birthdate)
        }
        WheelDatePickerBottomSheet(
            initialDate = initialLocalDate,
            onDateConfirmed = { confirmedDate ->
                viewModel.onBirthdateChange(formatBirthdate(confirmedDate))
            },
            onDismiss = { showDatePicker = false }
        )
    }

    HubThemeBackground(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            // Appbar : iconbtn verre + « Étape 2 sur 3 » (05-complete-profile.html).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HubGlassIconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("complete_profile_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = strings.back,
                        tint = HubTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = strings.stepIndicatorFormat.format(
                        PROFILE_STEP_INDEX,
                        PROFILE_STEP_TOTAL
                    ),
                    color = HubTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp
                )
                Spacer(Modifier.weight(1f))
            }

            // Barre de progression de la maquette (.pad + .progress à 66 %).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 2.dp)
                    .height(7.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(HubGlassStrong)
                    .border(1.dp, HubStrokeSoft, RoundedCornerShape(6.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(PROFILE_STEP_PROGRESS)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(6.dp),
                            clip = false,
                            ambientColor = Color.Transparent,
                            spotColor = HubAccent.copy(alpha = 0.7f)
                        )
                        .background(brush = hubPrimaryGradient())
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column {
                    Text(
                        text = buildAnnotatedString {
                            append("${strings.cpTitleTop} ")
                            withStyle(style = SpanStyle(brush = hubPrimaryGradient())) {
                                append(strings.cpTitleGrad)
                            }
                        },
                        color = HubTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 25.sp,
                        letterSpacing = (-0.4).sp
                    )

                    Text(
                        text = strings.cpBody,
                        color = HubTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 7.dp)
                    )
                }

                // Sélecteur de photo : avatar 96 anneau accent + caméra 36/13.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .border(
                                    width = 2.5.dp,
                                    // .avatar.ring : outline accent 70 %, offset 2.5
                                    color = HubAccent.copy(alpha = 0.7f),
                                    shape = CircleShape
                                )
                                .padding(2.5.dp)
                                .testTag("profile_photo_picker")
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                        ) {
                            ProfileAvatar(
                                size = 96.dp,
                                photoUri = uiState.selectedImageUri,
                                initials = deriveInitials(
                                    displayName = uiState.displayName,
                                    fallback = uiState.username
                                ),
                                photoDescription = strings.selectedPhotoDescription
                            )
                        }

                        // .sendbtn : 36 px, rayon 13, dégradé de marque + lueur.
                        Box(
                            modifier = Modifier
                                .padding(bottom = 0.dp)
                                .size(36.dp)
                                .shadow(
                                    elevation = 14.dp,
                                    shape = RoundedCornerShape(13.dp),
                                    clip = false,
                                    ambientColor = Color.Transparent,
                                    spotColor = HubFabGlow
                                )
                                .clip(RoundedCornerShape(13.dp))
                                .background(brush = hubPrimaryGradient())
                                .drawBehind {
                                    // inset 0 1px 0 rgba(255,255,255,.3)
                                    drawRect(
                                        color = Color.White.copy(alpha = 0.3f),
                                        topLeft = Offset(0f, 0f),
                                        size = Size(size.width, 1.dp.toPx())
                                    )
                                }
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CameraAlt,
                                contentDescription = strings.selectPhotoDescription,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    Text(
                        text = if (uiState.selectedImageUri != null) {
                            strings.changePhotoLabel
                        } else {
                            strings.choosePhotoLabel
                        },
                        color = HubTextTertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                    )
                }

                // Nom d'utilisateur : accent + coche succès quand disponible.
                LabeledField(label = strings.usernameLabel) {
                    Column {
                        HubTextField(
                            value = uiState.username,
                            onValueChange = viewModel::onUsernameChange,
                            placeholder = strings.usernamePlaceholder,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.AlternateEmail,
                                    contentDescription = null,
                                    tint = HubAccent,
                                    modifier = Modifier.size(17.dp)
                                )
                            },
                            trailingIcon = if (uiState.usernameError == null &&
                                uiState.username.length >= 3
                            ) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = HubSuccess,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            } else null,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            errorMessage = uiState.usernameError,
                            testTag = "complete_profile_username_input"
                        )
                        // État « succès » de la maquette : « ✓ Nom disponible ».
                        if (uiState.usernameError == null && uiState.username.length >= 3) {
                            Text(
                                text = strings.usernameAvailable,
                                color = HubSuccess,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                            )
                        }
                    }
                }

                // Nom complet.
                LabeledField(label = strings.fullNameLabel) {
                    HubTextField(
                        value = uiState.displayName,
                        onValueChange = viewModel::onDisplayNameChange,
                        placeholder = strings.fullNamePlaceholder,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = HubTextTertiary,
                                modifier = Modifier.size(17.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        errorMessage = uiState.displayNameError,
                        testTag = "complete_profile_display_name_input"
                    )
                }

                // Bio (état ViewModel conservé) + compteur 150.
                LabeledField(label = strings.bioLabel) {
                    Column {
                        HubTextField(
                            value = uiState.bio,
                            onValueChange = viewModel::onBioChange,
                            placeholder = strings.bioPlaceholder,
                            singleLine = false,
                            maxLines = 4,
                            minHeight = 96,
                            errorMessage = uiState.bioError,
                            testTag = "complete_profile_bio_input"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = strings.bioCounterFormat.format(uiState.bio.length),
                                color = HubTextTertiary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Date de naissance : champ verre cliquable vers la roue.
                LabeledField(label = strings.birthdateLabel) {
                    BirthdateField(
                        value = uiState.birthdate,
                        placeholder = strings.birthdatePlaceholder,
                        onClick = { showDatePicker = true },
                        modifier = Modifier.testTag("complete_profile_birthdate_input")
                    )
                }

                // État « erreur » : bannière .banner.danger de la maquette.
                if (uiState.generalError != null) {
                    HubErrorBanner(message = uiState.generalError.orEmpty())
                }

                HubButton(
                    text = strings.cpContinue,
                    onClick = viewModel::completeProfile,
                    isLoading = uiState.isLoading,
                    modifier = Modifier.padding(top = 6.dp),
                    testTag = "complete_profile_submit_button"
                )

                Text(
                    text = strings.birthdatePrivate,
                    color = HubTextTertiary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Étiquette .field label : 12/700/--text-2, interlettrage .3, écart 7 px.
 */
@Composable
private fun LabeledField(
    label: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = label,
            color = HubTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp
        )
        Spacer(Modifier.height(7.dp))
        content()
    }
}

/**
 * Avatar 96 px de la maquette : .av-g1 (#8B5CF6 → #6366F1) avec initiales 34/800,
 * ou la photo choisie recadrée en cercle.
 */
@Composable
private fun ProfileAvatar(
    size: androidx.compose.ui.unit.Dp,
    photoUri: android.net.Uri?,
    initials: String,
    photoDescription: String
) {
    if (photoUri != null) {
        AsyncImage(
            model = photoUri,
            contentDescription = photoDescription,
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

/** Initiales du nom affiché (« Aïcha Moussa » → « AM »), repli « U ». */
private fun deriveInitials(displayName: String, fallback: String): String {
    val source = displayName.trim().ifBlank { fallback.trim() }
    if (source.isBlank()) return "U"
    val words = source.split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> (words[0].first().toString() + words[1].first().toString()).uppercase()
        else -> source.take(1).uppercase()
    }
}

/**
 * Champ date de la maquette (.input) : fond --field, contour --stroke-soft,
 * rayon 18, icône calendrier --text-3, valeur 14/500, chevron bas 16.
 */
@Composable
private fun BirthdateField(
    value: String,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalHubColors.current
    val highlight = HubHighlight
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(palette.field)
            .drawBehind {
                // inset 0 1px 0 var(--hl)
                drawRect(
                    color = highlight,
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, 1.dp.toPx())
                )
            }
            .border(1.dp, HubStrokeSoft, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = null,
                tint = HubTextTertiary,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = value.ifBlank { placeholder },
                color = if (value.isBlank()) HubTextTertiary else HubTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = HubTextTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Iconbtn de la maquette (.iconbtn) : 40 dp, rayon --r-sm 14, verre --glass,
 * contour --stroke, ombre --shadow-sm et reflet interne haut 1 px.
 */
@Composable
private fun HubGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val palette = LocalHubColors.current
    val highlight = HubHighlight
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(40.dp)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                clip = false,
                ambientColor = palette.shadowColor,
                spotColor = palette.shadowColor
            )
            .clip(shape)
            .background(palette.glass)
            .drawBehind {
                drawRect(
                    color = highlight,
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, 1.dp.toPx())
                )
            }
            .border(1.dp, palette.stroke, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
