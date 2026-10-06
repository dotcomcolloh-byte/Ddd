package com.telefam.company.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telefam.company.app.R
import com.telefam.company.app.data.TelefamBubbleColorOption
import com.telefam.company.app.data.TelefamComPrivacyCatalog
import com.telefam.company.app.data.TelefamOfflineChatTheme
import com.telefam.company.app.data.TelefamPrivacyAudience
import com.telefam.company.app.data.TelefamPrivacySettingsState
import com.telefam.company.app.ui.theme.LocalTelefamColors

enum class PrivacyToggleKey {
    MESSAGE_REQUESTS,
    WHO_CAN_CALL_ME,
    WHO_CAN_SCREENSHOT_CHATS,
    WHO_CAN_SHARE_CHATS,
    WHO_CAN_COPY_MESSAGES,
    WHO_CAN_DOWNLOAD_MY_MEDIA,
    READ_RECEIPTS,
    TYPING_INDICATORS,
    ONLINE_INDICATORS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelefamComPrivacyScreen(
    privacyState: TelefamPrivacySettingsState,
    onToggleAudience: (PrivacyToggleKey) -> Unit,
    onSelectChatTheme: (String) -> Unit,
    onSelectBubbleColor: (String) -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClicked() }

    val telefamColors = LocalTelefamColors.current
    val scrollState = rememberScrollState()

    var showChatThemeSheet by remember { mutableStateOf(false) }
    var showBubbleColorSheet by remember { mutableStateOf(false) }

    val activeTheme = remember(privacyState.selectedChatThemeId) {
        TelefamComPrivacyCatalog.getThemeById(privacyState.selectedChatThemeId)
    }
    val activeBubbleColor = remember(privacyState.selectedBubbleColorId) {
        TelefamComPrivacyCatalog.getBubbleColorById(privacyState.selectedBubbleColorId)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = telefamColors.cardBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // Red Top Bar matching uploaded Privacy reference image: Back arrow + "Telefam" + 3 dots
            Surface(
                color = telefamColors.brandRed,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .height(56.dp)
                        .padding(horizontal = 6.dp)
                ) {
                    IconButton(
                        onClick = onBackClicked,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("privacy_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = stringResource(R.string.app_name),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { /* Already in 3-dots Privacy screen */ },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
            ) {
                Spacer(modifier = Modifier.height(22.dp))

                // Large "Privacy" header + subtitle matching uploaded image
                Text(
                    text = "Privacy",
                    color = telefamColors.textPrimary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Control who can interact with you and your messages.",
                    color = telefamColors.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 1. Message Requests
                PrivacyAudienceRow(
                    title = "Message Requests",
                    subtitle = "Set who can send you message requests.",
                    audience = privacyState.messageRequests,
                    onClick = { onToggleAudience(PrivacyToggleKey.MESSAGE_REQUESTS) },
                    testTag = "privacy_row_message_requests",
                    iconContent = {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = "Message Requests",
                            tint = telefamColors.brandRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                )
                PrivacyRowDivider()

                // 2. Who Can Call Me
                PrivacyAudienceRow(
                    title = "Who Can Call Me",
                    subtitle = "Control who can call you.",
                    audience = privacyState.whoCanCallMe,
                    onClick = { onToggleAudience(PrivacyToggleKey.WHO_CAN_CALL_ME) },
                    testTag = "privacy_row_who_can_call_me",
                    iconContent = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Who Can Call Me",
                            tint = telefamColors.brandRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                )
                PrivacyRowDivider()

                // 3. Who Can Screenshot Chats (Dashed rectangle icon matching reference image)
                PrivacyAudienceRow(
                    title = "Who Can Screenshot Chats",
                    subtitle = "Choose who can take screenshots of your chats.",
                    audience = privacyState.whoCanScreenshotChats,
                    onClick = { onToggleAudience(PrivacyToggleKey.WHO_CAN_SCREENSHOT_CHATS) },
                    testTag = "privacy_row_who_can_screenshot",
                    iconContent = {
                        DashedScreenshotIcon(tint = telefamColors.brandRed)
                    }
                )
                PrivacyRowDivider()

                // 4. Who Can Share Chats (Upload/Share tray icon matching reference image)
                PrivacyAudienceRow(
                    title = "Who Can Share Chats",
                    subtitle = "Control who can share your chats.",
                    audience = privacyState.whoCanShareChats,
                    onClick = { onToggleAudience(PrivacyToggleKey.WHO_CAN_SHARE_CHATS) },
                    testTag = "privacy_row_who_can_share",
                    iconContent = {
                        Icon(
                            imageVector = Icons.Default.IosShare,
                            contentDescription = "Who Can Share Chats",
                            tint = telefamColors.brandRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                )
                PrivacyRowDivider()

                // 5. Who Can Copy Messages
                PrivacyAudienceRow(
                    title = "Who Can Copy Messages",
                    subtitle = "Control who can copy your messages.",
                    audience = privacyState.whoCanCopyMessages,
                    onClick = { onToggleAudience(PrivacyToggleKey.WHO_CAN_COPY_MESSAGES) },
                    testTag = "privacy_row_who_can_copy",
                    iconContent = {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Who Can Copy Messages",
                            tint = telefamColors.brandRed,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                )
                PrivacyRowDivider()

                // 6. Who Can Download My Media
                PrivacyAudienceRow(
                    title = "Who Can Download My Media",
                    subtitle = "Control who can download your photos, videos and files.",
                    audience = privacyState.whoCanDownloadMyMedia,
                    onClick = { onToggleAudience(PrivacyToggleKey.WHO_CAN_DOWNLOAD_MY_MEDIA) },
                    testTag = "privacy_row_who_can_download",
                    iconContent = {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Who Can Download My Media",
                            tint = telefamColors.brandRed,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                )
                PrivacyRowDivider()

                // 7. Chats Theme (Palette icon + Soft pink pill with theme dot + chevron matching reference image)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showChatThemeSheet = true }
                        .padding(vertical = 14.dp)
                        .testTag("privacy_row_chats_theme")
                ) {
                    PrivacyLeadingCircleBadge {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Chats Theme",
                            tint = telefamColors.brandRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Chats Theme",
                            color = telefamColors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Choose your chat background theme.",
                            color = telefamColors.textSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Pill with theme color circle + chevron matching reference image
                    Surface(
                        color = telefamColors.brandRedTint,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, telefamColors.brandRed.copy(alpha = 0.28f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(activeTheme.previewPrimaryColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Open Chats Theme Picker",
                                tint = telefamColors.brandRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                PrivacyRowDivider()

                // 8. Message Bubbles Colour (Overlapping circles icon + selected bubble color dot on right)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBubbleColorSheet = true }
                        .padding(vertical = 14.dp)
                        .testTag("privacy_row_message_bubbles_colour")
                ) {
                    PrivacyLeadingCircleBadge {
                        OverlappingBubblesIcon(tint = telefamColors.brandRed)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Message Bubbles Colour",
                            color = telefamColors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Choose the colour of your message bubbles.",
                            color = telefamColors.textSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(activeBubbleColor.color)
                            .border(1.5.dp, telefamColors.inputBorder, CircleShape)
                    )
                }
                PrivacyRowDivider()

                // 9. Read Receipts (Eye icon + small red eye indicator next to Anyone/Nobody)
                PrivacyAudienceRow(
                    title = "Read Receipts",
                    subtitle = "Control who can see if you read messages.",
                    audience = privacyState.readReceipts,
                    onClick = { onToggleAudience(PrivacyToggleKey.READ_RECEIPTS) },
                    testTag = "privacy_row_read_receipts",
                    iconContent = {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Read Receipts",
                            tint = telefamColors.brandRed,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    trailingPrefixIndicator = {
                        Icon(
                            imageVector = if (privacyState.readReceipts == TelefamPrivacyAudience.ANYONE) {
                                Icons.Default.Visibility
                            } else {
                                Icons.Default.VisibilityOff
                            },
                            contentDescription = null,
                            tint = if (privacyState.readReceipts == TelefamPrivacyAudience.ANYONE) {
                                telefamColors.brandRed
                            } else {
                                telefamColors.textSecondary
                            },
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                PrivacyRowDivider()

                // 10. Typing Indicators (Three horizontal dots icon + small "..." indicator next to Anyone/Nobody)
                PrivacyAudienceRow(
                    title = "Typing Indicators",
                    subtitle = "Control who can see when you are typing.",
                    audience = privacyState.typingIndicators,
                    onClick = { onToggleAudience(PrivacyToggleKey.TYPING_INDICATORS) },
                    testTag = "privacy_row_typing_indicators",
                    iconContent = {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "Typing Indicators",
                            tint = telefamColors.brandRed,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingPrefixIndicator = {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = null,
                            tint = telefamColors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                PrivacyRowDivider()

                // 11. Online Indicators (Person with green online dot badge + small green dot next to Anyone/Nobody)
                PrivacyAudienceRow(
                    title = "Online Indicators",
                    subtitle = "Control who can see when you are online.",
                    audience = privacyState.onlineIndicators,
                    onClick = { onToggleAudience(PrivacyToggleKey.ONLINE_INDICATORS) },
                    testTag = "privacy_row_online_indicators",
                    iconContent = {
                        OnlinePersonBadgeIcon(
                            brandRed = telefamColors.brandRed,
                            isOnlineEnabled = privacyState.onlineIndicators == TelefamPrivacyAudience.ANYONE
                        )
                    },
                    trailingPrefixIndicator = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (privacyState.onlineIndicators == TelefamPrivacyAudience.ANYONE) {
                                        Color(0xFF16A34A)
                                    } else {
                                        telefamColors.textSecondary
                                    }
                                )
                        )
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    if (showChatThemeSheet) {
        OfflineChatsThemePickerSheet(
            selectedThemeId = privacyState.selectedChatThemeId,
            activeBubbleColor = activeBubbleColor.color,
            onSelectTheme = { themeId ->
                onSelectChatTheme(themeId)
            },
            onDismiss = { showChatThemeSheet = false }
        )
    }

    if (showBubbleColorSheet) {
        MessageBubblesColourPickerSheet(
            selectedBubbleColorId = privacyState.selectedBubbleColorId,
            selectedTheme = activeTheme,
            onSelectBubbleColor = { colorId ->
                onSelectBubbleColor(colorId)
            },
            onDismiss = { showBubbleColorSheet = false }
        )
    }
}

@Composable
private fun PrivacyAudienceRow(
    title: String,
    subtitle: String,
    audience: TelefamPrivacyAudience,
    onClick: () -> Unit,
    testTag: String,
    iconContent: @Composable () -> Unit,
    trailingPrefixIndicator: (@Composable () -> Unit)? = null
) {
    val telefamColors = LocalTelefamColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
            .testTag(testTag)
    ) {
        PrivacyLeadingCircleBadge(content = iconContent)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = telefamColors.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = telefamColors.textSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (trailingPrefixIndicator != null) {
                trailingPrefixIndicator()
            }
            Text(
                text = audience.label,
                color = if (audience == TelefamPrivacyAudience.ANYONE) {
                    telefamColors.textSecondary
                } else {
                    telefamColors.brandRed
                },
                fontSize = 15.sp,
                fontWeight = if (audience == TelefamPrivacyAudience.NOBODY) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                }
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Toggle Anyone or Nobody",
                tint = telefamColors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun PrivacyLeadingCircleBadge(
    content: @Composable () -> Unit
) {
    val telefamColors = LocalTelefamColors.current
    val badgeBg = if (telefamColors.isDark) {
        Color(0xFF3A161A)
    } else {
        Color(0xFFF6D5D7)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(badgeBg)
    ) {
        content()
    }
}

@Composable
private fun PrivacyRowDivider() {
    val telefamColors = LocalTelefamColors.current
    HorizontalDivider(
        color = telefamColors.inputBorder.copy(alpha = 0.65f),
        thickness = 0.8.dp,
        modifier = Modifier.padding(start = 60.dp)
    )
}

@Composable
private fun DashedScreenshotIcon(tint: Color) {
    Canvas(modifier = Modifier.size(21.dp)) {
        val strokeWidth = 2.dp.toPx()
        drawRoundRect(
            color = tint,
            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(
                width = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            )
        )
    }
}

@Composable
private fun OverlappingBubblesIcon(tint: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        // Top-left circle
        drawCircle(
            color = tint,
            radius = w * 0.25f,
            center = Offset(w * 0.40f, h * 0.34f)
        )
        // Bottom-left circle
        drawCircle(
            color = tint.copy(alpha = 0.85f),
            radius = w * 0.23f,
            center = Offset(w * 0.32f, h * 0.68f)
        )
        // Bottom-right circle
        drawCircle(
            color = tint,
            radius = w * 0.25f,
            center = Offset(w * 0.68f, h * 0.64f)
        )
    }
}

@Composable
private fun OnlinePersonBadgeIcon(
    brandRed: Color,
    isOnlineEnabled: Boolean
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(26.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Online Indicators",
            tint = brandRed,
            modifier = Modifier
                .size(22.dp)
                .align(Alignment.BottomCenter)
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(if (isOnlineEnabled) Color(0xFF16A34A) else Color(0xFF9CA3AF))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfflineChatsThemePickerSheet(
    selectedThemeId: String,
    activeBubbleColor: Color,
    onSelectTheme: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val telefamColors = LocalTelefamColors.current
    val themes = TelefamComPrivacyCatalog.offlineChatThemes
    val currentTheme = TelefamComPrivacyCatalog.getThemeById(selectedThemeId)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = telefamColors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "Chats Theme",
                    color = telefamColors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "5 offline abstract & nature wallpapers crafted for Telefam chat backgrounds.",
                    color = telefamColors.textSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Live Preview Card of Selected Offline Theme + Active Bubble Colour
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = currentTheme.drawableResId),
                            contentDescription = currentTheme.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                        ) {
                            // Incoming sample bubble preview
                            Surface(
                                color = Color.White.copy(alpha = 0.90f),
                                shape = RoundedCornerShape(
                                    topStart = 14.dp,
                                    topEnd = 14.dp,
                                    bottomEnd = 14.dp,
                                    bottomStart = 4.dp
                                )
                            ) {
                                Text(
                                    text = "Theme: ${currentTheme.title}",
                                    color = Color(0xFF1A1C1E),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }

                            // Outgoing sample bubble preview using chosen Message Bubble Colour
                            Surface(
                                color = activeBubbleColor,
                                shape = RoundedCornerShape(
                                    topStart = 14.dp,
                                    topEnd = 14.dp,
                                    bottomStart = 14.dp,
                                    bottomEnd = 4.dp
                                ),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = "Works 100% offline ✨",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = themes,
                    key = { it.id }
                ) { themeItem: TelefamOfflineChatTheme ->
                    val isSelected = themeItem.id == selectedThemeId
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(114.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectTheme(themeItem.id) }
                            .testTag("chat_theme_item_${themeItem.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(152.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) telefamColors.brandRed else telefamColors.inputBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Image(
                                painter = painterResource(id = themeItem.drawableResId),
                                contentDescription = themeItem.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Mini chat bubble preview inside thumbnail
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(56.dp)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(Color.White.copy(alpha = 0.85f))
                                )
                                Box(
                                    modifier = Modifier
                                        .width(64.dp)
                                        .height(14.dp)
                                        .align(Alignment.End)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(activeBubbleColor)
                                )
                            }

                            if (isSelected) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(telefamColors.brandRed)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected Theme",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = themeItem.title,
                            color = if (isSelected) telefamColors.brandRed else telefamColors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageBubblesColourPickerSheet(
    selectedBubbleColorId: String,
    selectedTheme: TelefamOfflineChatTheme,
    onSelectBubbleColor: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val telefamColors = LocalTelefamColors.current
    val colors = TelefamComPrivacyCatalog.bubbleColors
    val activeBubble = TelefamComPrivacyCatalog.getBubbleColorById(selectedBubbleColorId)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = telefamColors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Message Bubbles Colour",
                color = telefamColors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose the colour of your outgoing message bubbles.",
                color = telefamColors.textSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Live Bubble Preview over current offline theme
            Surface(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = selectedTheme.drawableResId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = activeBubble.color,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 4.dp
                        ),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "${activeBubble.name} Bubble Preview",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Color Swatches Grid (2 rows of 4)
            val rows = colors.chunked(4)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { option: TelefamBubbleColorOption ->
                            val isSelected = option.id == selectedBubbleColorId
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectBubbleColor(option.id) }
                                    .padding(vertical = 8.dp)
                                    .testTag("bubble_color_option_${option.id}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(option.color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) telefamColors.textPrimary else Color.White.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = option.name,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = option.name.substringAfter(" "),
                                    color = if (isSelected) telefamColors.textPrimary else telefamColors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
