package com.thehub.hb.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubTextField
import com.thehub.hb.ui.theme.HubBlack
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubLightGray
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel,
    onNavigateBack: () -> Unit,
    onProfileUpdated: () -> Unit = onNavigateBack,
    modifier: Modifier = Modifier
) {
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
        val parsedMillis = remember(uiState.birthdate) {
            try {
                if (uiState.birthdate.isNotBlank()) {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(uiState.birthdate)?.time
                } else null
            } catch (_: Exception) {
                null
            }
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = parsedMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            viewModel.onBirthdateChanged(formatter.format(Date(selectedMillis)))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = HubWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Annuler", color = HubSecondary)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = HubSurfaceElevated
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = HubSurfaceElevated,
                    titleContentColor = HubWhite,
                    headlineContentColor = HubWhite,
                    weekdayContentColor = HubSecondary,
                    subheadContentColor = HubLightGray,
                    yearContentColor = HubWhite,
                    currentYearContentColor = HubWhite,
                    selectedYearContentColor = HubBlack,
                    selectedYearContainerColor = HubWhite,
                    dayContentColor = HubWhite,
                    selectedDayContentColor = HubBlack,
                    selectedDayContainerColor = HubWhite,
                    todayDateBorderColor = HubWhite,
                    todayContentColor = HubWhite
                )
            )
        }
    }

    LaunchedEffect(uiState.generalError) {
        uiState.generalError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = HubBlack,
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
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("edit_profile_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = HubWhite
                    )
                }

                Text(
                    text = "Modifier le profil",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HubWhite,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = HubWhite,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(36.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Photo Avatar with Edit Badge
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("edit_profile_avatar_picker"),
                        contentAlignment = Alignment.Center
                    ) {
                        val imageModel = uiState.selectedImageUri ?: uiState.photoUrl

                        @Composable
                        fun DefaultPersonPlaceholder() {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(HubSurfaceElevated)
                                    .border(2.dp, HubBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = HubSecondary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        if (imageModel != null) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imageModel)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Photo de profil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .border(2.dp, HubBorder, CircleShape),
                                loading = {
                                    DefaultPersonPlaceholder()
                                },
                                success = {
                                    SubcomposeAsyncImageContent()
                                },
                                error = {
                                    DefaultPersonPlaceholder()
                                }
                            )
                        } else {
                            DefaultPersonPlaceholder()
                        }

                        // Camera Icon Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(HubWhite)
                                .border(2.dp, HubBlack, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Changer la photo",
                                tint = HubBlack,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Changer la photo de profil",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = HubSecondary,
                        modifier = Modifier.clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Display Name
                    Text(
                        text = "Nom affiché",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HubSecondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    HubTextField(
                        value = uiState.displayName,
                        onValueChange = { viewModel.onDisplayNameChanged(it) },
                        placeholder = "Votre nom ou pseudonyme",
                        errorMessage = uiState.displayNameError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_display_name")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Username
                    Text(
                        text = "Nom d'utilisateur",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HubSecondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    HubTextField(
                        value = uiState.username,
                        onValueChange = { viewModel.onUsernameChanged(it) },
                        placeholder = "username",
                        errorMessage = uiState.usernameError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_username")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bio
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bio",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HubSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${uiState.bio.length} / 150",
                            fontSize = 12.sp,
                            color = if (uiState.bio.length > 150) HubError else HubMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    HubTextField(
                        value = uiState.bio,
                        onValueChange = { viewModel.onBioChanged(it) },
                        placeholder = "Quelques mots sur vous...",
                        errorMessage = uiState.bioError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_bio")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Birthdate
                    Text(
                        text = "Date de naissance",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HubSecondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF121212), RoundedCornerShape(20.dp))
                            .border(
                                width = 1.dp,
                                color = HubBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 16.dp)
                            .testTag("edit_profile_birthdate"),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = "Date de naissance",
                                tint = HubSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (uiState.birthdate.isNotBlank()) uiState.birthdate else "JJ/MM/AAAA",
                                color = if (uiState.birthdate.isNotBlank()) HubWhite else HubMuted,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Save Button
                    HubButton(
                        text = "Enregistrer",
                        onClick = {
                            viewModel.saveProfile {
                                onProfileUpdated()
                            }
                        },
                        isLoading = uiState.isSaving,
                        enabled = uiState.isValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_save_button")
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
