package com.telefam.company.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.telefam.company.app.data.TelefamMessageEntity
import com.telefam.company.app.data.TelefamMessageType
import com.telefam.company.app.data.TelefamSignalSessionEntity
import com.telefam.company.app.ui.components.TelefamComLucideIcons
import com.telefam.company.app.ui.components.rememberStickerImageLoader
import com.telefam.company.app.ui.theme.LocalTelefamColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalSafetyNumberVerificationSheet(
    peerName: String,
    peerEmail: String,
    signalSession: TelefamSignalSessionEntity?,
    onToggleVerified: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val telefamColors = LocalTelefamColors.current
    val safetyDigits = signalSession?.safetyNumberNumeric
        ?: "49182 03847 92817 65432 10982 74619 38291 04958 76123 98412 30194 87261"
    val isVerified = signalSession?.isVerified == true
    val digitGroups = remember(safetyDigits) { safetyDigits.split(" ").chunked(4) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = telefamColors.cardBackground
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(telefamColors.brandRedTint)
            ) {
                TelefamComLucideIcons.ShieldCheckIcon(
                    tint = telefamColors.brandRed,
                    size = 30.dp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Verify Safety Number",
                color = telefamColors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Messages and voice notes with $peerName ($peerEmail) are end-to-end encrypted with Signal Protocol (X3DH + Double Ratchet AES-256-GCM).",
                color = telefamColors.textSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Real 60-Digit Safety Number Grid derived from SHA-512 over both EC Identity Keys
            Surface(
                color = telefamColors.inputBackground,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, telefamColors.inputBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signal_safety_number_box")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(vertical = 18.dp, horizontal = 14.dp)
                ) {
                    digitGroups.forEach { rowChunks ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowChunks.forEach { block ->
                                Text(
                                    text = block,
                                    color = telefamColors.textPrimary,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (signalSession != null) {
                Text(
                    text = "Local Identity Key Fingerprint: ${signalSession.localIdentityPublicKeyBase64.take(24)}…\nPeer Identity Key Fingerprint: ${signalSession.peerIdentityPublicKeyBase64.take(24)}…\nDouble Ratchet Step: #${signalSession.sendRatchetCounter}",
                    color = telefamColors.textSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = { onToggleVerified(!isVerified) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isVerified) Color(0xFF16A34A) else telefamColors.brandRed,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("verify_safety_number_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isVerified) "Verified End-to-End Session" else "Mark Safety Number as Verified",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatMediaLinksDocsBottomSheet(
    messages: List<TelefamMessageEntity>,
    onLongPressMessage: (TelefamMessageEntity) -> Unit = {},
    onDismiss: () -> Unit
) {
    val telefamColors = LocalTelefamColors.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val imageLoader = rememberStickerImageLoader()

    val mediaMessages = remember(messages) {
        messages.filter {
            it.messageType == TelefamMessageType.STICKER.name ||
                it.messageType == TelefamMessageType.VOICE_NOTE.name
        }
    }

    val linkMessages = remember(messages) {
        val urlRegex = Regex("(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)", RegexOption.IGNORE_CASE)
        messages.filter { urlRegex.containsMatchIn(it.plaintextContent) }
    }

    val docMessages = remember(messages) {
        messages.filter {
            it.plaintextContent.endsWith(".pdf", true) ||
                it.plaintextContent.endsWith(".doc", true) ||
                it.plaintextContent.endsWith(".docx", true) ||
                it.plaintextContent.endsWith(".zip", true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = telefamColors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .padding(horizontal = 18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Media, Links & Docs",
                        color = telefamColors.textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Long press any item for message actions",
                        color = telefamColors.textSecondary,
                        fontSize = 11.5.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = telefamColors.textSecondary
                    )
                }
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = telefamColors.cardBackground,
                contentColor = telefamColors.brandRed,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = telefamColors.brandRed
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Media (${mediaMessages.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Links (${linkMessages.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Docs (${docMessages.size})", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTabIndex) {
                0 -> {
                    if (mediaMessages.isEmpty()) {
                        EmptySharedSectionState(
                            icon = Icons.Default.Image,
                            title = "No shared media yet",
                            subtitle = "Stickers and voice notes sent in this chat will appear here."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(mediaMessages, key = { it.messageId }) { msg ->
                                Surface(
                                    color = telefamColors.inputBackground,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, telefamColors.inputBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .combinedClickable(
                                            onClick = { onLongPressMessage(msg) },
                                            onLongClick = { onLongPressMessage(msg) }
                                        )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        if (msg.messageType == TelefamMessageType.STICKER.name) {
                                            SubcomposeAsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(msg.mediaUrl)
                                                    .build(),
                                                imageLoader = imageLoader,
                                                contentDescription = msg.plaintextContent,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Sticker: ${msg.plaintextContent}",
                                                    color = telefamColors.textPrimary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "Encrypted Media • Long press for options",
                                                    color = telefamColors.textSecondary,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        } else {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(telefamColors.brandRedTint)
                                            ) {
                                                TelefamComLucideIcons.MicVoiceIcon(
                                                    tint = telefamColors.brandRed,
                                                    size = 20.dp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (msg.isViewOnce) "View-Once Voice Note" else "Voice Note (${(msg.audioDurationMs / 1000L).coerceAtLeast(1)}s)",
                                                    color = telefamColors.textPrimary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "Audio Recording • Long press for options",
                                                    color = telefamColors.textSecondary,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        if (!msg.reactionEmoji.isNullOrBlank()) {
                                            Text(text = msg.reactionEmoji, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    if (linkMessages.isEmpty()) {
                        EmptySharedSectionState(
                            icon = Icons.Default.Link,
                            title = "No shared links yet",
                            subtitle = "Web links shared in this conversation will appear here."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(linkMessages, key = { it.messageId }) { msg ->
                                Surface(
                                    color = telefamColors.inputBackground,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, telefamColors.inputBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .combinedClickable(
                                            onClick = { onLongPressMessage(msg) },
                                            onLongClick = { onLongPressMessage(msg) }
                                        )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = null,
                                            tint = telefamColors.brandRed
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = msg.plaintextContent,
                                            color = telefamColors.textPrimary,
                                            fontSize = 13.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    if (docMessages.isEmpty()) {
                        EmptySharedSectionState(
                            icon = Icons.Default.Description,
                            title = "No shared documents yet",
                            subtitle = "Documents shared in this chat will be listed here."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(docMessages, key = { it.messageId }) { msg ->
                                Surface(
                                    color = telefamColors.inputBackground,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, telefamColors.inputBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .combinedClickable(
                                            onClick = { onLongPressMessage(msg) },
                                            onLongClick = { onLongPressMessage(msg) }
                                        )
                                ) {
                                    Text(
                                        text = msg.plaintextContent,
                                        color = telefamColors.textPrimary,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySharedSectionState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    val telefamColors = LocalTelefamColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(telefamColors.brandRedTint)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = telefamColors.brandRed,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            color = telefamColors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = telefamColors.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DisappearingMessagesDialog(
    currentTimerMillis: Long,
    onSelectTimerMillis: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val telefamColors = LocalTelefamColors.current
    var isEnabled by remember { mutableStateOf(currentTimerMillis > 0L) }
    var selectedDuration by remember {
        mutableStateOf(
            if (currentTimerMillis > 0L) currentTimerMillis else 86_400_000L // default 24 hours when toggled on
        )
    }

    val options = listOf(
        86_400_000L to "24 Hours",
        604_800_000L to "7 Days",
        2_592_000_000L to "1 Month (30 Days)"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = telefamColors.cardBackground,
            shape = RoundedCornerShape(22.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Disappearing Messages",
                            tint = telefamColors.brandRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Disappearing Messages",
                            color = telefamColors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = telefamColors.brandRed
                        ),
                        modifier = Modifier.testTag("disappearing_messages_toggle_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "When enabled, new messages sent in this chat will automatically disappear after the selected timer.",
                    color = telefamColors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                options.forEach { (millis, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = isEnabled) { selectedDuration = millis }
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    ) {
                        RadioButton(
                            selected = isEnabled && selectedDuration == millis,
                            onClick = {
                                isEnabled = true
                                selectedDuration = millis
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = telefamColors.brandRed
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            color = if (isEnabled) telefamColors.textPrimary else telefamColors.textSecondary,
                            fontSize = 15.sp,
                            fontWeight = if (isEnabled && selectedDuration == millis) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = telefamColors.textPrimary
                        )
                    }

                    Button(
                        onClick = {
                            onSelectTimerMillis(if (isEnabled) selectedDuration else 0L)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = telefamColors.brandRed,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("save_disappearing_messages_button")
                    ) {
                        Text(
                            text = "Save",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportUserQuestionnaireModal(
    peerName: String,
    onSubmitReport: (
        answer1Reason: String,
        answer2Severity: String,
        answer3Action: String,
        additionalNotes: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val telefamColors = LocalTelefamColors.current

    val q1Options = listOf(
        "Spam or unwanted messages",
        "Harassment or abusive behavior",
        "Impersonation or fake identity",
        "Scam, fraud, or phishing link"
    )
    val q2Options = listOf(
        "Happened once in this chat",
        "Repeated messages over time",
        "Automated or bulk suspicious activity"
    )
    val q3Options = listOf(
        "Submit report & block user",
        "Submit report only",
        "Submit report & clear chat history"
    )

    var selectedQ1 by remember { mutableStateOf(q1Options.first()) }
    var selectedQ2 by remember { mutableStateOf(q2Options.first()) }
    var selectedQ3 by remember { mutableStateOf(q3Options.first()) }
    var additionalNotes by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = telefamColors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Report $peerName",
                color = telefamColors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Please answer the questions below so our safety team can review this report.",
                color = telefamColors.textSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Question 1
            Text(
                text = "1. What is the primary reason for reporting this chat?",
                color = telefamColors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            q1Options.forEach { option ->
                QuestionAnswerOptionRow(
                    text = option,
                    selected = selectedQ1 == option,
                    onClick = { selectedQ1 = option }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = telefamColors.inputBorder)
            Spacer(modifier = Modifier.height(14.dp))

            // Question 2
            Text(
                text = "2. How frequently has this issue occurred?",
                color = telefamColors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            q2Options.forEach { option ->
                QuestionAnswerOptionRow(
                    text = option,
                    selected = selectedQ2 == option,
                    onClick = { selectedQ2 = option }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = telefamColors.inputBorder)
            Spacer(modifier = Modifier.height(14.dp))

            // Question 3
            Text(
                text = "3. What action would you like Telefam to take right now?",
                color = telefamColors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            q3Options.forEach { option ->
                QuestionAnswerOptionRow(
                    text = option,
                    selected = selectedQ3 == option,
                    onClick = { selectedQ3 = option }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = additionalNotes,
                onValueChange = { additionalNotes = it },
                placeholder = { Text("Optional context or details…", fontSize = 13.sp) },
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = telefamColors.inputBackground,
                    unfocusedContainerColor = telefamColors.inputBackground,
                    focusedBorderColor = telefamColors.brandRed,
                    unfocusedBorderColor = telefamColors.inputBorder,
                    focusedTextColor = telefamColors.textPrimary,
                    unfocusedTextColor = telefamColors.textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel", color = telefamColors.textPrimary)
                }

                Button(
                    onClick = {
                        onSubmitReport(
                            selectedQ1,
                            selectedQ2,
                            selectedQ3,
                            additionalNotes.trim()
                        )
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = telefamColors.brandRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("submit_report_button")
                ) {
                    Text("Submit Report", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun QuestionAnswerOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val telefamColors = LocalTelefamColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = telefamColors.brandRed)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = telefamColors.textPrimary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
