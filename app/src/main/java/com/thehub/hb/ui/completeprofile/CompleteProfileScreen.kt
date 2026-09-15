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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubTextField
import com.thehub.hb.ui.theme.HubBlack
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubBorderLight
import com.thehub.hb.ui.theme.HubDarkGray
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubLightGray
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceDark
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    viewModel: CompleteProfileViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToFeed: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
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
                            viewModel.onBirthdateChange(formatter.format(Date(selectedMillis)))
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HubSurfaceDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Back Button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(HubSurfaceElevated)
                .border(1.dp, HubBorder, CircleShape)
                .testTag("complete_profile_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Retour",
                tint = HubWhite,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Complète ton profil",
            color = HubWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Personnalise ton espace avant d'entrer.",
            color = HubSecondary,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Avatar selector
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(HubSurfaceElevated)
                    .border(1.dp, HubBorderLight, CircleShape)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .testTag("profile_photo_picker"),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.selectedImageUri != null) {
                    AsyncImage(
                        model = uiState.selectedImageUri,
                        contentDescription = "Photo de profil sélectionnée",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = "Choisir une photo",
                        tint = HubSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (uiState.selectedImageUri != null) "Changer la photo" else "Ajouter une photo",
                color = HubLightGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Username
            HubTextField(
                value = uiState.username,
                onValueChange = viewModel::onUsernameChange,
                placeholder = "Nom d'utilisateur (ex: alex_rivera)",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.AlternateEmail,
                        contentDescription = null,
                        tint = HubSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                errorMessage = uiState.usernameError,
                testTag = "complete_profile_username_input"
            )

            // Display name
            HubTextField(
                value = uiState.displayName,
                onValueChange = viewModel::onDisplayNameChange,
                placeholder = "Nom complet (ex: Alex Rivera)",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = HubSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                errorMessage = uiState.displayNameError,
                testTag = "complete_profile_display_name_input"
            )

            // Bio (multiline)
            Column {
                HubTextField(
                    value = uiState.bio,
                    onValueChange = viewModel::onBioChange,
                    placeholder = "Bio courte (design, centres d'intérêt, etc.)",
                    singleLine = false,
                    maxLines = 4,
                    minHeight = 96,
                    testTag = "complete_profile_bio_input"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "${uiState.bio.length}/150",
                        color = HubMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // Birthdate
            Column(modifier = Modifier.fillMaxWidth()) {
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
                        .testTag("complete_profile_birthdate_input"),
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
            }

            if (uiState.generalError != null) {
                Text(
                    text = uiState.generalError ?: "",
                    color = HubError,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            HubButton(
                text = "Terminer",
                onClick = viewModel::completeProfile,
                isLoading = uiState.isLoading,
                testTag = "complete_profile_submit_button"
            )
        }
    }
}
