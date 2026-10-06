package com.telefam.company.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telefam.company.app.R
import com.telefam.company.app.data.TelefamComCountries
import com.telefam.company.app.data.TelefamCountry
import com.telefam.company.app.ui.TelefamUiState
import com.telefam.company.app.ui.components.TelefamComPhotoCropperDialog
import com.telefam.company.app.ui.theme.LocalTelefamColors
import java.io.File
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelefamComProfileSetupScreen(
    uiState: TelefamUiState,
    onFullNameChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onCountrySelected: (TelefamCountry) -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onLocationChanged: (String) -> Unit,
    onGenderSelected: (String) -> Unit,
    onDateOfBirthSelected: (Long) -> Unit,
    onBioChanged: (String) -> Unit,
    onPhotoCropped: (Bitmap) -> Unit,
    onContinueClicked: () -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClicked() }

    val context = LocalContext.current
    val telefamColors = LocalTelefamColors.current
    val scrollState = rememberScrollState()

    var bitmapForCropping by remember { mutableStateOf<Bitmap?>(null) }
    var showCountryPickerSheet by remember { mutableStateOf(false) }
    var showGenderMenu by remember { mutableStateOf(false) }
    var showDatePickerModal by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val decoded = BitmapFactory.decodeStream(stream)
                    if (decoded != null) {
                        bitmapForCropping = decoded
                    }
                }
            }
        }
    }

    val loadedAvatarBitmap = remember(uiState.profilePhotoPath) {
        if (uiState.profilePhotoPath.isNotBlank() && File(uiState.profilePhotoPath).exists()) {
            BitmapFactory.decodeFile(uiState.profilePhotoPath)
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(telefamColors.brandRed)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Crimson Banner Header matching Reference Image 2
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 12.dp, vertical = 14.dp)
            ) {
                IconButton(
                    onClick = onBackClicked,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(48.dp)
                        .testTag("profile_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = stringResource(R.string.profile_setup),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.tell_us_about_yourself),
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 14.sp
                    )
                }
            }

            // Rounded Top Sheet matching Reference Image 2
            Surface(
                color = telefamColors.cardBackground,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .imePadding()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 520.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Circular Dashed Red Ring + Camera Icon + Red '+' Badge matching Reference Image 2
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(118.dp)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("upload_profile_photo_button")
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = telefamColors.brandRed,
                                    radius = (size.minDimension / 2f) - 2.dp.toPx(),
                                    style = Stroke(
                                        width = 1.6.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                                    )
                                )
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(104.dp)
                                    .clip(CircleShape)
                                    .background(telefamColors.brandRedTint)
                            ) {
                                if (loadedAvatarBitmap != null) {
                                    Image(
                                        bitmap = loadedAvatarBitmap.asImageBitmap(),
                                        contentDescription = "Selected Profile Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Upload Photo Camera Icon",
                                        tint = telefamColors.brandRed,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            // Bottom-right '+' circle badge
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-4).dp, y = (-4).dp)
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(telefamColors.brandRed)
                                    .border(2.dp, telefamColors.cardBackground, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(R.string.upload_profile_photo),
                            color = telefamColors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(R.string.upload_photo_subtitle),
                            color = telefamColors.textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        // 1. Full Name (autofilled from email prefix)
                        ProfileFieldHeader(
                            icon = Icons.Default.Person,
                            label = stringResource(R.string.full_name_label)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = uiState.fullName,
                            onValueChange = onFullNameChanged,
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.full_name_placeholder),
                                    color = telefamColors.textSecondary.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = profileTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_full_name_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Username (autofilled from email prefix)
                        ProfileFieldHeader(
                            icon = Icons.Default.AlternateEmail,
                            label = stringResource(R.string.username_label)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = uiState.username,
                            onValueChange = onUsernameChanged,
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.username_placeholder),
                                    color = telefamColors.textSecondary.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = profileTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_username_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. Phone Number with Country Flag + Chevron + Number input matching Reference Image 2
                        ProfileFieldHeader(
                            icon = Icons.Default.Phone,
                            label = stringResource(R.string.phone_number_label)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(telefamColors.inputBackground)
                                .border(1.dp, telefamColors.inputBorder, RoundedCornerShape(10.dp))
                        ) {
                            // Country Flag & Dial Code Selector Box
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clickable { showCountryPickerSheet = true }
                                    .padding(horizontal = 12.dp)
                                    .testTag("country_code_selector")
                            ) {
                                Text(
                                    text = uiState.selectedCountry.flagEmoji,
                                    fontSize = 22.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = uiState.selectedCountry.dialCode,
                                    color = telefamColors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select Country Code",
                                    tint = telefamColors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Vertical divider inside the unified phone field matching Reference Image 2
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight(0.68f)
                                    .background(telefamColors.inputBorder)
                            )

                            OutlinedTextField(
                                value = uiState.phoneNumber,
                                onValueChange = onPhoneNumberChanged,
                                singleLine = true,
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.phone_number_placeholder),
                                        color = telefamColors.textSecondary.copy(alpha = 0.7f),
                                        fontSize = 14.sp
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = telefamColors.textPrimary,
                                    unfocusedTextColor = telefamColors.textPrimary,
                                    cursorColor = telefamColors.brandRed
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_phone_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 4. Location field
                        ProfileFieldHeader(
                            icon = Icons.Default.LocationOn,
                            label = stringResource(R.string.location_label)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = uiState.location,
                            onValueChange = onLocationChanged,
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.location_placeholder),
                                    color = telefamColors.textSecondary.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = profileTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_location_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 5. Gender selector dropdown matching Reference Image 2
                        ProfileFieldHeader(
                            icon = Icons.Default.People,
                            label = stringResource(R.string.gender_label)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(telefamColors.inputBackground)
                                    .border(1.dp, telefamColors.inputBorder, RoundedCornerShape(10.dp))
                                    .clickable { showGenderMenu = true }
                                    .padding(horizontal = 14.dp)
                                    .testTag("profile_gender_selector")
                            ) {
                                Text(
                                    text = uiState.gender.ifBlank { stringResource(R.string.gender_placeholder) },
                                    color = if (uiState.gender.isBlank()) {
                                        telefamColors.textSecondary.copy(alpha = 0.7f)
                                    } else {
                                        telefamColors.textPrimary
                                    },
                                    fontSize = 14.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select Gender",
                                    tint = telefamColors.textSecondary
                                )
                            }

                            DropdownMenu(
                                expanded = showGenderMenu,
                                onDismissRequest = { showGenderMenu = false },
                                modifier = Modifier.background(telefamColors.cardBackground)
                            ) {
                                listOf("Male", "Female", "Non-binary", "Prefer not to say").forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option,
                                                color = telefamColors.textPrimary
                                            )
                                        },
                                        onClick = {
                                            onGenderSelected(option)
                                            showGenderMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 6. Date of Birth calendar picker with restricted age selection matching Reference Image 2
                        ProfileFieldHeader(
                            icon = Icons.Default.CalendarMonth,
                            label = stringResource(R.string.dob_label)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(telefamColors.inputBackground)
                                .border(1.dp, telefamColors.inputBorder, RoundedCornerShape(10.dp))
                                .clickable { showDatePickerModal = true }
                                .padding(horizontal = 14.dp)
                                .testTag("profile_dob_selector")
                        ) {
                            Text(
                                text = uiState.dateOfBirthFormatted.ifBlank {
                                    stringResource(R.string.dob_placeholder)
                                },
                                color = if (uiState.dateOfBirthFormatted.isBlank()) {
                                    telefamColors.textSecondary.copy(alpha = 0.7f)
                                } else {
                                    telefamColors.textPrimary
                                },
                                fontSize = 14.sp
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select Date of Birth",
                                tint = telefamColors.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 7. Bio field with 0/150 character counter inside bottom-right matching Reference Image 2
                        ProfileFieldHeader(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            label = stringResource(R.string.bio_label)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = uiState.bio,
                                onValueChange = onBioChanged,
                                minLines = 3,
                                maxLines = 4,
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.bio_placeholder),
                                        color = telefamColors.textSecondary.copy(alpha = 0.7f),
                                        fontSize = 14.sp
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = profileTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(104.dp)
                                    .testTag("profile_bio_input")
                            )

                            Text(
                                text = "${uiState.bio.length}/150",
                                color = telefamColors.textSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 12.dp, bottom = 10.dp)
                            )
                        }

                        if (uiState.profileError != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = uiState.profileError,
                                color = Color(0xFFD32F2F),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Full-width Red "Continue" Button matching Reference Image 2
                        Button(
                            onClick = onContinueClicked,
                            enabled = !uiState.isSavingProfile,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = telefamColors.brandRed,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("profile_continue_button")
                        ) {
                            if (uiState.isSavingProfile) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.continue_button),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }
            }
        }
    }

    // Photo Cropper Dialog
    val activeCropBitmap = bitmapForCropping
    if (activeCropBitmap != null) {
        TelefamComPhotoCropperDialog(
            sourceBitmap = activeCropBitmap,
            onCropConfirmed = { cropped ->
                onPhotoCropped(cropped)
                bitmapForCropping = null
            },
            onDismiss = { bitmapForCropping = null }
        )
    }

    // All Countries Code + Flag Searchable Bottom Sheet
    if (showCountryPickerSheet) {
        CountryCodePickerBottomSheet(
            selectedCountry = uiState.selectedCountry,
            onCountryChosen = {
                onCountrySelected(it)
                showCountryPickerSheet = false
            },
            onDismiss = { showCountryPickerSheet = false }
        )
    }

    // Restricted Age Date of Birth Calendar Picker (13+ to 100 years old)
    if (showDatePickerModal) {
        RestrictedAgeDateOfBirthDialog(
            initialSelectedMillis = uiState.dateOfBirthMillis,
            onDateSelected = { millis ->
                onDateOfBirthSelected(millis)
                showDatePickerModal = false
            },
            onDismiss = { showDatePickerModal = false }
        )
    }
}

@Composable
private fun ProfileFieldHeader(
    icon: ImageVector,
    label: String
) {
    val telefamColors = LocalTelefamColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = telefamColors.brandRed,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = telefamColors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun profileTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = LocalTelefamColors.current.inputBackground,
    unfocusedContainerColor = LocalTelefamColors.current.inputBackground,
    focusedBorderColor = LocalTelefamColors.current.brandRed,
    unfocusedBorderColor = LocalTelefamColors.current.inputBorder,
    focusedTextColor = LocalTelefamColors.current.textPrimary,
    unfocusedTextColor = LocalTelefamColors.current.textPrimary,
    cursorColor = LocalTelefamColors.current.brandRed
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryCodePickerBottomSheet(
    selectedCountry: TelefamCountry,
    onCountryChosen: (TelefamCountry) -> Unit,
    onDismiss: () -> Unit
) {
    val telefamColors = LocalTelefamColors.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredCountries = remember(searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            TelefamComCountries.allCountries
        } else {
            TelefamComCountries.allCountries.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.dialCode.contains(q, ignoreCase = true) ||
                    it.isoCode.contains(q, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = telefamColors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Select Country Code",
                color = telefamColors.textPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                placeholder = { Text("Search country name or dial code (+254, +1…)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Country",
                        tint = telefamColors.brandRed
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = profileTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("country_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = filteredCountries,
                    key = { it.isoCode }
                ) { country ->
                    val isSelected = country.isoCode == selectedCountry.isoCode
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) telefamColors.brandRedTint else Color.Transparent
                            )
                            .clickable { onCountryChosen(country) }
                            .padding(horizontal = 12.dp, vertical = 13.dp)
                    ) {
                        Text(
                            text = country.flagEmoji,
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = country.name,
                                color = telefamColors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = "${country.isoCode} • Requires ${country.minDigits}${if (country.maxDigits != country.minDigits) "-${country.maxDigits}" else ""} digits",
                                color = telefamColors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = country.dialCode,
                            color = telefamColors.brandRed,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(color = telefamColors.inputBorder.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestrictedAgeDateOfBirthDialog(
    initialSelectedMillis: Long?,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val telefamColors = LocalTelefamColors.current

    val maxAllowedCalendar = remember {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            add(Calendar.YEAR, -13)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
    }
    val minAllowedCalendar = remember {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            add(Calendar.YEAR, -100)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
    }

    val maxUtcMillis = maxAllowedCalendar.timeInMillis
    val minUtcMillis = minAllowedCalendar.timeInMillis
    val minYear = minAllowedCalendar.get(Calendar.YEAR)
    val maxYear = maxAllowedCalendar.get(Calendar.YEAR)

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedMillis ?: maxUtcMillis,
        initialDisplayedMonthMillis = initialSelectedMillis ?: maxUtcMillis,
        yearRange = minYear..maxYear,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis in minUtcMillis..maxUtcMillis
            }

            override fun isSelectableYear(year: Int): Boolean {
                return year in minYear..maxYear
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val chosen = datePickerState.selectedDateMillis ?: maxUtcMillis
                    onDateSelected(chosen.coerceIn(minUtcMillis, maxUtcMillis))
                }
            ) {
                Text(
                    text = "Confirm (13+ Verified)",
                    color = telefamColors.brandRed,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = telefamColors.textSecondary
                )
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = "Select Date of Birth (Minimum age 13+)",
                    color = telefamColors.brandRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                )
            }
        )
    }
}
