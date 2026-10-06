package com.telefam.company.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.telefam.company.app.data.TelefamComPrivacyCatalog
import com.telefam.company.app.data.TelefamConversationEntity
import com.telefam.company.app.data.TelefamMessageEntity
import com.telefam.company.app.data.TelefamMessageStatus
import com.telefam.company.app.data.TelefamMessageType
import com.telefam.company.app.data.TelefamPrivacyAudience
import com.telefam.company.app.data.TelefamPrivacySettingsState
import com.telefam.company.app.data.TelefamSignalSessionEntity
import com.telefam.company.app.data.TelefamStickerItem
import com.telefam.company.app.ui.components.StickerSkeletonBox
import com.telefam.company.app.ui.components.TelefamComLucideIcons
import com.telefam.company.app.ui.components.TelefamComMessageActionMenuSheet
import com.telefam.company.app.ui.components.TelefamComStickerBottomSheet
import com.telefam.company.app.ui.components.rememberStickerImageLoader
import com.telefam.company.app.ui.theme.LocalTelefamColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TelefamComIndividualChatScreen(
    activity: Activity,
    conversation: TelefamConversationEntity,
    messages: List<TelefamMessageEntity>,
    privacySettings: TelefamPrivacySettingsState,
    signalSession: TelefamSignalSessionEntity?,
    stickers: List<TelefamStickerItem>,
    stickerSearchQuery: String,
    isLoadingStickers: Boolean,
    isLoadingNextStickers: Boolean,
    stickerError: String?,
    onStickerSearchChanged: (String) -> Unit,
    onLoadMoreStickers: () -> Unit,
    onRetryStickers: () -> Unit,
    onSendTextMessage: (String, TelefamMessageEntity?) -> Unit,
    onSendStickerMessage: (TelefamStickerItem, TelefamMessageEntity?) -> Unit,
    onStartVoiceRecording: () -> String,
    onStopVoiceRecording: () -> Pair<String, Long>,
    onPlayAudioFile: (String, () -> Unit) -> Unit,
    onStopAudioPlayback: () -> Unit,
    onSendVoiceNoteMessage: (audioPath: String, durationMs: Long, isViewOnce: Boolean, replyTarget: TelefamMessageEntity?) -> Unit,
    onConsumeViewOnceVoiceNote: (String) -> Unit,
    onReactToMessage: (messageId: String, emoji: String) -> Unit,
    onTogglePinMessage: (messageId: String, pin: Boolean) -> Unit,
    onToggleFavouriteMessage: (messageId: String, favourite: Boolean) -> Unit,
    onEditOwnMessage: (messageId: String, newPlaintext: String) -> Unit,
    onUnsendMessageForEveryone: (messageId: String) -> Unit,
    onDeleteMessageForMe: (messageId: String) -> Unit,
    onDeleteSelectedMessages: (Set<String>) -> Unit,
    onToggleBlockPeer: (Boolean) -> Unit,
    onSubmitReportPeer: (String, String, String, String) -> Unit,
    onSetDisappearingTimer: (Long) -> Unit,
    onClearChatMessages: () -> Unit,
    onToggleSignalVerified: (Boolean) -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val telefamColors = LocalTelefamColors.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val imageLoader = rememberStickerImageLoader()

    // Enforce FLAG_SECURE when user privacy setting "Who Can Screenshot Chats" is set to NOBODY
    DisposableEffect(privacySettings.whoCanScreenshotChats) {
        val window = activity.window
        if (privacySettings.whoCanScreenshotChats == TelefamPrivacyAudience.NOBODY) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Resolve user's selected Offline Chat Theme (defaults to first theme "crimson_aurora") & Bubble Color (defaults to "red")
    val activeTheme = remember(privacySettings.selectedChatThemeId) {
        TelefamComPrivacyCatalog.getThemeById(privacySettings.selectedChatThemeId)
    }
    val activeBubbleColor = remember(privacySettings.selectedBubbleColorId) {
        TelefamComPrivacyCatalog.getBubbleColorById(privacySettings.selectedBubbleColorId).color
    }
    val isCopyAllowedByPrivacy = privacySettings.whoCanCopyMessages == TelefamPrivacyAudience.ANYONE

    var composerText by remember { mutableStateOf("") }
    var isInChatSearchActive by remember { mutableStateOf(false) }
    var inChatSearchQuery by remember { mutableStateOf("") }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showStickerSheet by remember { mutableStateOf(false) }
    var showSignalSafetySheet by remember { mutableStateOf(false) }
    var showMediaLinksDocsSheet by remember { mutableStateOf(false) }
    var showDisappearingDialog by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }

    // Swipe-to-Reply, Long-Press Menu, Owner Edit, and Multi-Selection states
    var replyingToMessage by remember { mutableStateOf<TelefamMessageEntity?>(null) }
    var editingMessage by remember { mutableStateOf<TelefamMessageEntity?>(null) }
    var activeMenuMessageId by remember { mutableStateOf<String?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedMessageIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    BackHandler {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedMessageIds = emptySet()
        } else if (editingMessage != null) {
            editingMessage = null
            composerText = ""
        } else if (replyingToMessage != null) {
            replyingToMessage = null
        } else {
            onBackClicked()
        }
    }

    // Resolve live message for long-press menu so real-time reactions/pins/edits reflect immediately
    val activeMenuMessage = remember(messages, activeMenuMessageId) {
        activeMenuMessageId?.let { targetId ->
            messages.find { it.messageId == targetId }
        }
    }

    // Latest pinned message in conversation for top Pinned Banner
    val latestPinnedMessage = remember(messages) {
        messages.lastOrNull { it.isPinned }
    }

    // Voice note recording & preview states
    var isHoldingMicToRecord by remember { mutableStateOf(false) }
    var recordingElapsedMs by remember { mutableStateOf(0L) }
    var recordedPreviewPath by remember { mutableStateOf<String?>(null) }
    var recordedPreviewDurationMs by remember { mutableStateOf(0L) }
    var isVoiceNoteViewOnce by remember { mutableStateOf(false) }
    var isPlayingPreviewAudio by remember { mutableStateOf(false) }
    var currentlyPlayingMessageId by remember { mutableStateOf<String?>(null) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(isHoldingMicToRecord) {
        if (isHoldingMicToRecord) {
            recordingElapsedMs = 0L
            while (isHoldingMicToRecord) {
                delay(200L)
                recordingElapsedMs += 200L
            }
        }
    }

    val filteredMessages = remember(messages, inChatSearchQuery) {
        val q = inChatSearchQuery.trim()
        if (q.isEmpty()) {
            messages
        } else {
            messages.filter { it.plaintextContent.contains(q, ignoreCase = true) }
        }
    }

    // Clean up selection / reply / edit references if a message is unsent in real time
    LaunchedEffect(messages) {
        val existingIds = messages.map { it.messageId }.toSet()
        if (selectedMessageIds.any { it !in existingIds }) {
            selectedMessageIds = selectedMessageIds.intersect(existingIds)
            if (selectedMessageIds.isEmpty()) {
                isSelectionMode = false
            }
        }
        if (replyingToMessage != null && replyingToMessage?.messageId !in existingIds) {
            replyingToMessage = null
        }
        if (editingMessage != null && editingMessage?.messageId !in existingIds) {
            editingMessage = null
            composerText = ""
        }
    }

    LaunchedEffect(filteredMessages.size) {
        if (filteredMessages.isNotEmpty()) {
            runCatching { listState.animateScrollToItem(filteredMessages.lastIndex) }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isSelectionMode) {
                    // Multi-Message Selection Top Bar with Mark All & Delete (using Lucide Icons)
                    val allMarked = filteredMessages.isNotEmpty() &&
                        selectedMessageIds.size == filteredMessages.size
                    Surface(
                        color = telefamColors.brandRed,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_selection_top_bar")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .height(58.dp)
                                .padding(horizontal = 8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    isSelectionMode = false
                                    selectedMessageIds = emptySet()
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .testTag("selection_close_button")
                            ) {
                                TelefamComLucideIcons.XCloseLucideIcon(
                                    tint = Color.White,
                                    size = 19.dp
                                )
                            }

                            Text(
                                text = "${selectedMessageIds.size} selected",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )

                            // Mark All / Unmark All Option
                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable {
                                        selectedMessageIds = if (allMarked) {
                                            emptySet()
                                        } else {
                                            filteredMessages.map { it.messageId }.toSet()
                                        }
                                    }
                                    .testTag("selection_mark_all_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                ) {
                                    TelefamComLucideIcons.CheckSquareSelectLucideIcon(
                                        tint = Color.White,
                                        isChecked = allMarked,
                                        size = 16.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (allMarked) "Unmark All" else "Mark All",
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Delete Selected Messages Button
                            IconButton(
                                onClick = {
                                    if (selectedMessageIds.isNotEmpty()) {
                                        val count = selectedMessageIds.size
                                        onDeleteSelectedMessages(selectedMessageIds)
                                        selectedMessageIds = emptySet()
                                        isSelectionMode = false
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Deleted $count message(s)")
                                        }
                                    }
                                },
                                enabled = selectedMessageIds.isNotEmpty(),
                                modifier = Modifier
                                    .size(42.dp)
                                    .testTag("selection_delete_button")
                            ) {
                                TelefamComLucideIcons.TrashDeleteLucideIcon(
                                    tint = if (selectedMessageIds.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.45f),
                                    size = 20.dp
                                )
                            }
                        }
                    }
                } else {
                    // Standard Individual Chat Top Bar
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
                                .height(58.dp)
                                .padding(horizontal = 6.dp)
                        ) {
                            IconButton(
                                onClick = onBackClicked,
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("chat_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }

                            // Peer Avatar + Green Online Dot (if Online Indicators enabled)
                            Box(modifier = Modifier.size(38.dp)) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.22f))
                                ) {
                                    Text(
                                        text = conversation.peerFullName.take(1).uppercase(Locale.US),
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                if (conversation.isPeerOnline &&
                                    privacySettings.onlineIndicators == TelefamPrivacyAudience.ANYONE
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(11.dp)
                                            .align(Alignment.BottomEnd)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E))
                                            .border(1.8.dp, telefamColors.brandRed, CircleShape)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Peer Name + Online / Typing status subtitle
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showSignalSafetySheet = true }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = conversation.peerFullName,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (conversation.disappearingTimerMillis > 0L) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        TelefamComLucideIcons.TimerLucideIcon(
                                            tint = Color.White.copy(alpha = 0.9f),
                                            size = 14.dp
                                        )
                                    }
                                }

                                val statusLabel = when {
                                    conversation.isBlocked -> "Blocked"
                                    conversation.isPeerTyping &&
                                        privacySettings.typingIndicators == TelefamPrivacyAudience.ANYONE -> "typing…"
                                    conversation.isPeerOnline &&
                                        privacySettings.onlineIndicators == TelefamPrivacyAudience.ANYONE -> "online"
                                    else -> "@${conversation.peerUsername}"
                                }
                                Text(
                                    text = statusLabel,
                                    color = Color.White.copy(alpha = 0.88f),
                                    fontSize = 12.sp,
                                    fontWeight = if (statusLabel == "typing…") FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }

                            // Voice Call Icon (Coming Soon)
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Voice Call — Coming Soon")
                                    }
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .testTag("chat_voice_call_button")
                            ) {
                                TelefamComLucideIcons.PhoneCallLucideIcon(
                                    tint = Color.White,
                                    size = 20.dp
                                )
                            }

                            // Video Call Icon (Coming Soon)
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Video Call — Coming Soon")
                                    }
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .testTag("chat_video_call_button")
                            ) {
                                TelefamComLucideIcons.VideoCallLucideIcon(
                                    tint = Color.White,
                                    size = 20.dp
                                )
                            }

                            // 3-Dots Top Bar Menu on Individual Chat (using Lucide Icons)
                            Box {
                                IconButton(
                                    onClick = { showOverflowMenu = true },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .testTag("chat_three_dots_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Chat Options",
                                        tint = Color.White
                                    )
                                }

                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false },
                                    modifier = Modifier.background(telefamColors.cardBackground)
                                ) {
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            TelefamComLucideIcons.FolderMediaLucideIcon(
                                                tint = telefamColors.brandRed,
                                                size = 18.dp
                                            )
                                        },
                                        text = {
                                            Text(
                                                text = "Media, Links, and Docs",
                                                color = telefamColors.textPrimary
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            showMediaLinksDocsSheet = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            TelefamComLucideIcons.SearchLucideIcon(
                                                tint = telefamColors.brandRed,
                                                size = 18.dp
                                            )
                                        },
                                        text = {
                                            Text(
                                                text = "Search",
                                                color = telefamColors.textPrimary
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            isInChatSearchActive = !isInChatSearchActive
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            TelefamComLucideIcons.TimerLucideIcon(
                                                tint = telefamColors.brandRed,
                                                size = 18.dp
                                            )
                                        },
                                        text = {
                                            val timerText = when (conversation.disappearingTimerMillis) {
                                                86_400_000L -> "Disappearing Messages (24h)"
                                                604_800_000L -> "Disappearing Messages (7d)"
                                                2_592_000_000L -> "Disappearing Messages (1mo)"
                                                else -> "Disappearing Messages (Off)"
                                            }
                                            Text(
                                                text = timerText,
                                                color = telefamColors.textPrimary
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            showDisappearingDialog = true
                                        }
                                    )
                                    HorizontalDivider(color = telefamColors.inputBorder)
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            TelefamComLucideIcons.BanBlockLucideIcon(
                                                tint = Color(0xFFD32F2F),
                                                size = 18.dp
                                            )
                                        },
                                        text = {
                                            Text(
                                                text = if (conversation.isBlocked) "Unblock ${conversation.peerFullName}" else "Block ${conversation.peerFullName}",
                                                color = telefamColors.textPrimary
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            onToggleBlockPeer(!conversation.isBlocked)
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            TelefamComLucideIcons.FlagReportLucideIcon(
                                                tint = Color(0xFFD32F2F),
                                                size = 18.dp
                                            )
                                        },
                                        text = {
                                            Text(
                                                text = "Report",
                                                color = telefamColors.textPrimary
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            showReportSheet = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            TelefamComLucideIcons.TrashDeleteLucideIcon(
                                                tint = telefamColors.textSecondary,
                                                size = 18.dp
                                            )
                                        },
                                        text = {
                                            Text(
                                                text = "Clear Chat",
                                                color = telefamColors.textPrimary
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            onClearChatMessages()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Pinned Message Bar (Real-time banner when any message is pinned)
                AnimatedVisibility(visible = latestPinnedMessage != null) {
                    val pinned = latestPinnedMessage
                    if (pinned != null) {
                        Surface(
                            color = telefamColors.cardBackground.copy(alpha = 0.96f),
                            shadowElevation = 2.dp,
                            border = BorderStroke(0.5.dp, telefamColors.inputBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val idx = filteredMessages.indexOfFirst { it.messageId == pinned.messageId }
                                    if (idx >= 0) {
                                        coroutineScope.launch {
                                            runCatching { listState.animateScrollToItem(idx) }
                                        }
                                    }
                                }
                                .testTag("pinned_message_top_banner")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                TelefamComLucideIcons.PinLucideIcon(
                                    tint = telefamColors.brandRed,
                                    isFilled = true,
                                    size = 16.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Pinned Message",
                                        color = telefamColors.brandRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = when (pinned.messageType) {
                                            TelefamMessageType.STICKER.name -> "Sticker • ${pinned.plaintextContent}"
                                            else -> pinned.plaintextContent
                                        },
                                        color = telefamColors.textPrimary,
                                        fontSize = 12.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onTogglePinMessage(pinned.messageId, false) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    TelefamComLucideIcons.XCloseLucideIcon(
                                        tint = telefamColors.textSecondary,
                                        size = 14.dp
                                    )
                                }
                            }
                        }
                    }
                }

                // In-Chat Search Bar when toggled from 3-dots menu
                AnimatedVisibility(visible = isInChatSearchActive) {
                    Surface(
                        color = telefamColors.cardBackground,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            TelefamComLucideIcons.SearchLucideIcon(
                                tint = telefamColors.brandRed,
                                size = 17.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = inChatSearchQuery,
                                onValueChange = { inChatSearchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = telefamColors.textPrimary,
                                    fontSize = 14.sp
                                ),
                                cursorBrush = SolidColor(telefamColors.brandRed),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    if (inChatSearchQuery.isEmpty()) {
                                        Text(
                                            text = "Search messages in this chat…",
                                            color = telefamColors.textSecondary,
                                            fontSize = 13.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            IconButton(
                                onClick = {
                                    inChatSearchQuery = ""
                                    isInChatSearchActive = false
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                TelefamComLucideIcons.XCloseLucideIcon(
                                    tint = telefamColors.textSecondary,
                                    size = 15.dp
                                )
                            }
                        }
                    }
                }

                // Small Clickable Top E2EE Banner for First Messages (Opens Safety Number Verification Screen)
                if (messages.size <= 3) {
                    Surface(
                        color = telefamColors.cardBackground.copy(alpha = 0.94f),
                        shadowElevation = 2.dp,
                        border = BorderStroke(0.5.dp, telefamColors.brandRed.copy(alpha = 0.30f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSignalSafetySheet = true }
                            .testTag("e2ee_top_verification_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            TelefamComLucideIcons.ShieldCheckIcon(
                                tint = telefamColors.brandRed,
                                size = 15.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Messages are end-to-end encrypted with Signal Protocol. Tap to verify safety numbers.",
                                color = telefamColors.textPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            ChatBottomComposerBar(
                composerText = composerText,
                onComposerTextChanged = { composerText = it },
                replyingToMessage = replyingToMessage,
                editingMessage = editingMessage,
                peerName = conversation.peerFullName,
                peerEmail = conversation.peerEmail,
                onCancelReplyOrEdit = {
                    if (editingMessage != null) {
                        editingMessage = null
                        composerText = ""
                    } else {
                        replyingToMessage = null
                    }
                },
                isBlocked = conversation.isBlocked,
                onUnblockClicked = { onToggleBlockPeer(false) },
                isHoldingMic = isHoldingMicToRecord,
                recordingElapsedMs = recordingElapsedMs,
                recordedPreviewPath = recordedPreviewPath,
                recordedPreviewDurationMs = recordedPreviewDurationMs,
                isVoiceNoteViewOnce = isVoiceNoteViewOnce,
                isPlayingPreviewAudio = isPlayingPreviewAudio,
                onToggleVoiceNoteViewOnce = { isVoiceNoteViewOnce = !isVoiceNoteViewOnce },
                onPlayStopPreviewAudio = {
                    val path = recordedPreviewPath ?: return@ChatBottomComposerBar
                    if (isPlayingPreviewAudio) {
                        onStopAudioPlayback()
                        isPlayingPreviewAudio = false
                    } else {
                        isPlayingPreviewAudio = true
                        onPlayAudioFile(path) {
                            isPlayingPreviewAudio = false
                        }
                    }
                },
                onDiscardVoicePreview = {
                    onStopAudioPlayback()
                    isPlayingPreviewAudio = false
                    recordedPreviewPath = null
                    recordedPreviewDurationMs = 0L
                },
                onSendVoicePreview = {
                    val path = recordedPreviewPath ?: return@ChatBottomComposerBar
                    onStopAudioPlayback()
                    isPlayingPreviewAudio = false
                    onSendVoiceNoteMessage(path, recordedPreviewDurationMs, isVoiceNoteViewOnce, replyingToMessage)
                    recordedPreviewPath = null
                    recordedPreviewDurationMs = 0L
                    isVoiceNoteViewOnce = false
                    replyingToMessage = null
                },
                onMicPressStart = {
                    val hasMicPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasMicPerm) {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    isHoldingMicToRecord = true
                    onStartVoiceRecording()
                },
                onMicPressRelease = {
                    if (isHoldingMicToRecord) {
                        isHoldingMicToRecord = false
                        val (filePath, durationMs) = onStopVoiceRecording()
                        if (filePath.isNotBlank()) {
                            recordedPreviewPath = filePath
                            recordedPreviewDurationMs = durationMs
                        }
                    }
                },
                onOpenStickersSheet = { showStickerSheet = true },
                onFileAttachmentClicked = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("File Attachment — Coming Soon")
                    }
                },
                onSendOrSaveTextClicked = {
                    val trimmed = composerText.trim()
                    if (trimmed.isNotEmpty()) {
                        val editingTarget = editingMessage
                        if (editingTarget != null) {
                            onEditOwnMessage(editingTarget.messageId, trimmed)
                            editingMessage = null
                            composerText = ""
                        } else {
                            onSendTextMessage(trimmed, replyingToMessage)
                            replyingToMessage = null
                            composerText = ""
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        // Chat Background using Selected Offline Wallpaper from Privacy Settings (Defaults to 1st Theme: Crimson Aurora)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Image(
                painter = painterResource(id = activeTheme.drawableResId),
                contentDescription = activeTheme.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("individual_chat_message_list")
            ) {
                if (messages.size > 3) {
                    item(key = "inline_e2ee_chip") {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.42f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.clickable { showSignalSafetySheet = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    TelefamComLucideIcons.ShieldCheckIcon(
                                        tint = Color.White,
                                        size = 13.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Signal E2EE Active • Tap to verify",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                items(
                    items = filteredMessages,
                    key = { it.messageId }
                ) { message ->
                    val isOutgoing = message.senderEmail != conversation.peerEmail
                    val isSelected = message.messageId in selectedMessageIds

                    SwipeableAndInteractiveMessageBubble(
                        message = message,
                        isOutgoing = isOutgoing,
                        peerName = conversation.peerFullName,
                        outgoingBubbleColor = activeBubbleColor,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        isPlayingAudio = currentlyPlayingMessageId == message.messageId,
                        onToggleSelectMessage = {
                            selectedMessageIds = if (isSelected) {
                                selectedMessageIds - message.messageId
                            } else {
                                selectedMessageIds + message.messageId
                            }
                        },
                        onSwipeToReplyTriggered = {
                            editingMessage = null
                            replyingToMessage = message
                        },
                        onLongPressTriggered = {
                            activeMenuMessageId = message.messageId
                        },
                        onQuotedReplyClicked = { quotedId ->
                            val idx = filteredMessages.indexOfFirst { it.messageId == quotedId }
                            if (idx >= 0) {
                                coroutineScope.launch {
                                    runCatching { listState.animateScrollToItem(idx) }
                                }
                            }
                        },
                        onReactionBadgeClicked = {
                            activeMenuMessageId = message.messageId
                        },
                        onPlayToggleAudio = {
                            if (message.isViewOnce && message.isViewOnceConsumed) {
                                return@SwipeableAndInteractiveMessageBubble
                            }
                            if (currentlyPlayingMessageId == message.messageId) {
                                onStopAudioPlayback()
                                currentlyPlayingMessageId = null
                            } else {
                                currentlyPlayingMessageId = message.messageId
                                onPlayAudioFile(message.localAudioPath) {
                                    currentlyPlayingMessageId = null
                                    if (message.isViewOnce) {
                                        onConsumeViewOnceVoiceNote(message.messageId)
                                    }
                                }
                            }
                        },
                        imageLoader = imageLoader
                    )
                }

                // Peer Typing Bubble when Peer is Typing and Typing Indicators are set to Anyone
                if (conversation.isPeerTyping &&
                    privacySettings.typingIndicators == TelefamPrivacyAudience.ANYONE
                ) {
                    item(key = "peer_typing_bubble") {
                        PeerTypingBubbleIndicator(peerName = conversation.peerFullName)
                    }
                }
            }
        }
    }

    // Long-Press Message Action Menu Bottom Sheet (works on Text, Stickers, Voice Notes & Media)
    if (activeMenuMessage != null) {
        val msg = activeMenuMessage
        val isOwner = msg.senderEmail != conversation.peerEmail

        TelefamComMessageActionMenuSheet(
            message = msg,
            isOwner = isOwner,
            isCopyAllowedByPrivacy = isCopyAllowedByPrivacy,
            onReactSelected = { emoji ->
                onReactToMessage(msg.messageId, emoji)
            },
            onReplyClicked = {
                editingMessage = null
                replyingToMessage = msg
            },
            onTogglePinClicked = {
                val nextPin = !msg.isPinned
                onTogglePinMessage(msg.messageId, nextPin)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        if (nextPin) "Message pinned" else "Message unpinned"
                    )
                }
            },
            onToggleFavouriteClicked = {
                val nextFav = !msg.isFavourite
                onToggleFavouriteMessage(msg.messageId, nextFav)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        if (nextFav) "Added to favourites" else "Removed from favourites"
                    )
                }
            },
            onCopyClicked = {
                if (isCopyAllowedByPrivacy) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val copyPayload = when (msg.messageType) {
                        TelefamMessageType.STICKER.name -> msg.mediaUrl.ifBlank { msg.plaintextContent }
                        else -> msg.plaintextContent
                    }
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Telefam Message", copyPayload))
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Copied to clipboard")
                    }
                }
            },
            onSelectClicked = {
                isSelectionMode = true
                selectedMessageIds = selectedMessageIds + msg.messageId
            },
            onEditClicked = {
                if (isOwner && msg.messageType == TelefamMessageType.TEXT.name) {
                    replyingToMessage = null
                    editingMessage = msg
                    composerText = msg.plaintextContent
                }
            },
            onUnsendForEveryoneClicked = {
                if (isOwner) {
                    onUnsendMessageForEveryone(msg.messageId)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Message unsent and cleared for both users")
                    }
                }
            },
            onDeleteForMeClicked = {
                onDeleteMessageForMe(msg.messageId)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Message deleted")
                }
            },
            onDismiss = { activeMenuMessageId = null }
        )
    }

    if (showStickerSheet) {
        TelefamComStickerBottomSheet(
            stickers = stickers,
            searchQuery = stickerSearchQuery,
            isLoadingInitial = isLoadingStickers,
            isLoadingNextPage = isLoadingNextStickers,
            errorMessage = stickerError,
            onSearchQueryChanged = onStickerSearchChanged,
            onLoadNextPage = onLoadMoreStickers,
            onRetryLoad = onRetryStickers,
            onStickerSendConfirmed = { sticker ->
                onSendStickerMessage(sticker, replyingToMessage)
                replyingToMessage = null
                showStickerSheet = false
            },
            onDismiss = { showStickerSheet = false }
        )
    }

    if (showSignalSafetySheet) {
        SignalSafetyNumberVerificationSheet(
            peerName = conversation.peerFullName,
            peerEmail = conversation.peerEmail,
            signalSession = signalSession,
            onToggleVerified = onToggleSignalVerified,
            onDismiss = { showSignalSafetySheet = false }
        )
    }

    if (showMediaLinksDocsSheet) {
        ChatMediaLinksDocsBottomSheet(
            messages = messages,
            onLongPressMessage = { mediaMsg ->
                showMediaLinksDocsSheet = false
                activeMenuMessageId = mediaMsg.messageId
            },
            onDismiss = { showMediaLinksDocsSheet = false }
        )
    }

    if (showDisappearingDialog) {
        DisappearingMessagesDialog(
            currentTimerMillis = conversation.disappearingTimerMillis,
            onSelectTimerMillis = onSetDisappearingTimer,
            onDismiss = { showDisappearingDialog = false }
        )
    }

    if (showReportSheet) {
        ReportUserQuestionnaireModal(
            peerName = conversation.peerFullName,
            onSubmitReport = onSubmitReportPeer,
            onDismiss = { showReportSheet = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeableAndInteractiveMessageBubble(
    message: TelefamMessageEntity,
    isOutgoing: Boolean,
    peerName: String,
    outgoingBubbleColor: Color,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isPlayingAudio: Boolean,
    onToggleSelectMessage: () -> Unit,
    onSwipeToReplyTriggered: () -> Unit,
    onLongPressTriggered: () -> Unit,
    onQuotedReplyClicked: (String) -> Unit,
    onReactionBadgeClicked: () -> Unit,
    onPlayToggleAudio: () -> Unit,
    imageLoader: coil.ImageLoader
) {
    val telefamColors = LocalTelefamColors.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 52.dp.toPx() }
    val maxDragPx = with(density) { 78.dp.toPx() }
    val dragOffsetX = remember { Animatable(0f) }

    val timeText = remember(message.createdAt) {
        SimpleDateFormat("HH:mm", Locale.US).format(Date(message.createdAt))
    }

    val bubbleBgColor = if (isOutgoing) {
        outgoingBubbleColor
    } else {
        if (telefamColors.isDark) Color(0xFF1F242D) else Color.White.copy(alpha = 0.96f)
    }
    val primaryTextColor = if (isOutgoing) Color.White else telefamColors.textPrimary
    val secondaryMetaColor = if (isOutgoing) {
        Color.White.copy(alpha = 0.82f)
    } else {
        telefamColors.textSecondary
    }

    val swipeProgress = (kotlin.math.abs(dragOffsetX.value) / swipeThresholdPx).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) telefamColors.brandRed.copy(alpha = 0.18f) else Color.Transparent
            )
            .pointerInput(message.messageId, isSelectionMode) {
                if (isSelectionMode) return@pointerInput
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        // Allow swiping right (or left for outgoing) to reply
                        val nextVal = (dragOffsetX.value + dragAmount).coerceIn(-maxDragPx, maxDragPx)
                        coroutineScope.launch {
                            dragOffsetX.snapTo(nextVal)
                        }
                    },
                    onDragEnd = {
                        val triggered = kotlin.math.abs(dragOffsetX.value) >= swipeThresholdPx
                        coroutineScope.launch {
                            dragOffsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                        if (triggered) {
                            onSwipeToReplyTriggered()
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            dragOffsetX.animateTo(0f)
                        }
                    }
                )
            }
            .testTag("message_row_${message.messageId}")
    ) {
        // Animated Swipe-to-Reply Lucide Icon Indicator revealed behind the message
        if (swipeProgress > 0.08f) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(if (dragOffsetX.value >= 0f) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 12.dp)
                    .size(30.dp)
                    .scale(0.65f + (0.35f * swipeProgress))
                    .clip(CircleShape)
                    .background(telefamColors.brandRed.copy(alpha = 0.88f))
            ) {
                TelefamComLucideIcons.ReplyCurvedLucideIcon(
                    tint = Color.White,
                    size = 16.dp
                )
            }
        }

        Row(
            horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(dragOffsetX.value.roundToInt(), 0) }
                .padding(vertical = 2.dp, horizontal = 2.dp)
        ) {
            // Multi-select checkbox when Selection Mode is active
            if (isSelectionMode) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(28.dp)
                        .clickable(onClick = onToggleSelectMessage)
                        .testTag("select_checkbox_${message.messageId}")
                ) {
                    TelefamComLucideIcons.CheckSquareSelectLucideIcon(
                        tint = if (isSelected) telefamColors.brandRed else Color.White.copy(alpha = 0.85f),
                        isChecked = isSelected,
                        size = 20.dp
                    )
                }
                if (isOutgoing) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Column(
                horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
            ) {
                Surface(
                    color = if (message.messageType == TelefamMessageType.STICKER.name && message.replyToMessageId == null) {
                        Color.Transparent
                    } else {
                        bubbleBgColor
                    },
                    shape = RoundedCornerShape(
                        topStart = 15.dp,
                        topEnd = 15.dp,
                        bottomStart = if (isOutgoing) 15.dp else 4.dp,
                        bottomEnd = if (isOutgoing) 4.dp else 15.dp
                    ),
                    shadowElevation = if (message.messageType == TelefamMessageType.STICKER.name && message.replyToMessageId == null) 0.dp else 2.dp,
                    modifier = Modifier
                        .widthIn(min = 76.dp, max = 268.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 15.dp,
                                topEnd = 15.dp,
                                bottomStart = if (isOutgoing) 15.dp else 4.dp,
                                bottomEnd = if (isOutgoing) 4.dp else 15.dp
                            )
                        )
                        .combinedClickable(
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSelectMessage()
                                } else if (message.messageType == TelefamMessageType.VOICE_NOTE.name) {
                                    onPlayToggleAudio()
                                }
                            },
                            onLongClick = onLongPressTriggered
                        )
                        .testTag("message_bubble_${message.messageId}")
                ) {
                    Column(
                        horizontalAlignment = if (isOutgoing && message.messageType == TelefamMessageType.STICKER.name) {
                            Alignment.End
                        } else {
                            Alignment.Start
                        }
                    ) {
                        // Quoted Reply Block inside the Bubble if this message is a Reply
                        if (!message.replyToMessageId.isNullOrBlank() && !message.replyToPreviewText.isNullOrBlank()) {
                            Surface(
                                color = if (isOutgoing) {
                                    Color.Black.copy(alpha = 0.18f)
                                } else {
                                    telefamColors.brandRed.copy(alpha = 0.10f)
                                },
                                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 6.dp, bottomEnd = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 6.dp, end = 6.dp, top = 6.dp)
                                    .clickable { onQuotedReplyClicked(message.replyToMessageId) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(26.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (isOutgoing) Color.White else telefamColors.brandRed)
                                    )
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Column {
                                        Text(
                                            text = message.replyToSenderName ?: peerName,
                                            color = if (isOutgoing) Color.White else telefamColors.brandRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = message.replyToPreviewText,
                                            color = secondaryMetaColor,
                                            fontSize = 11.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        when (message.messageType) {
                            TelefamMessageType.STICKER.name -> {
                                Column(
                                    horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    SubcomposeAsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(message.mediaUrl)
                                            .crossfade(true)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .build(),
                                        imageLoader = imageLoader,
                                        contentDescription = message.plaintextContent,
                                        contentScale = ContentScale.Fit,
                                        loading = {
                                            Box(modifier = Modifier.size(116.dp)) {
                                                StickerSkeletonBox()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.50f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        MessageTimestampAndReceiptRow(
                                            timeText = timeText,
                                            status = message.status,
                                            isOutgoing = isOutgoing,
                                            peerName = peerName,
                                            isPinned = message.isPinned,
                                            isFavourite = message.isFavourite,
                                            isEdited = message.isEdited,
                                            metaColor = Color.White,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            TelefamMessageType.VOICE_NOTE.name -> {
                                Column(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isOutgoing) Color.White.copy(alpha = 0.22f)
                                                    else telefamColors.brandRedTint
                                                )
                                                .clickable(onClick = onPlayToggleAudio)
                                        ) {
                                            if (message.isViewOnce && message.isViewOnceConsumed) {
                                                TelefamComLucideIcons.ViewOnceOneIcon(
                                                    tint = primaryTextColor.copy(alpha = 0.7f),
                                                    isActive = false,
                                                    size = 16.dp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = "Play Voice Note",
                                                    tint = if (isOutgoing) Color.White else telefamColors.brandRed,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f, fill = false)) {
                                            if (message.isViewOnce && message.isViewOnceConsumed) {
                                                Text(
                                                    text = "Opened View-Once Voice Note",
                                                    color = primaryTextColor.copy(alpha = 0.85f),
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            } else {
                                                VoiceWaveformBars(
                                                    barColor = primaryTextColor,
                                                    isAnimating = isPlayingAudio
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    TelefamComLucideIcons.MicVoiceIcon(
                                                        tint = secondaryMetaColor,
                                                        size = 12.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    val sec = (message.audioDurationMs / 1000L).coerceAtLeast(1L)
                                                    Text(
                                                        text = String.format(Locale.US, "0:%02d", sec),
                                                        color = secondaryMetaColor,
                                                        fontSize = 11.sp
                                                    )
                                                    if (message.isViewOnce) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        TelefamComLucideIcons.ViewOnceOneIcon(
                                                            tint = primaryTextColor,
                                                            isActive = true,
                                                            size = 13.dp
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = "View Once",
                                                            color = primaryTextColor,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))
                                    MessageTimestampAndReceiptRow(
                                        timeText = timeText,
                                        status = message.status,
                                        isOutgoing = isOutgoing,
                                        peerName = peerName,
                                        isPinned = message.isPinned,
                                        isFavourite = message.isFavourite,
                                        isEdited = message.isEdited,
                                        metaColor = secondaryMetaColor,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }

                            else -> {
                                // Compact, well-arranged text bubble
                                Column(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = message.plaintextContent,
                                        color = primaryTextColor,
                                        fontSize = 14.sp,
                                        lineHeight = 19.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    MessageTimestampAndReceiptRow(
                                        timeText = timeText,
                                        status = message.status,
                                        isOutgoing = isOutgoing,
                                        peerName = peerName,
                                        isPinned = message.isPinned,
                                        isFavourite = message.isFavourite,
                                        isEdited = message.isEdited,
                                        metaColor = secondaryMetaColor,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }

                // Emoji Reaction Pill Badge attached to the message bubble
                if (!message.reactionEmoji.isNullOrBlank()) {
                    Surface(
                        color = telefamColors.cardBackground,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, telefamColors.inputBorder),
                        modifier = Modifier
                            .padding(top = 2.dp, start = 4.dp, end = 4.dp)
                            .clickable(onClick = onReactionBadgeClicked)
                            .testTag("reaction_pill_${message.messageId}")
                    ) {
                        Text(
                            text = message.reactionEmoji,
                            fontSize = 13.5.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageTimestampAndReceiptRow(
    timeText: String,
    status: String,
    isOutgoing: Boolean,
    peerName: String,
    isPinned: Boolean = false,
    isFavourite: Boolean = false,
    isEdited: Boolean = false,
    metaColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        if (isPinned) {
            TelefamComLucideIcons.PinLucideIcon(
                tint = metaColor,
                isFilled = true,
                size = 10.dp
            )
        }
        if (isFavourite) {
            TelefamComLucideIcons.StarFavouriteLucideIcon(
                tint = Color(0xFFFBBF24),
                isFilled = true,
                size = 10.dp
            )
        }
        if (isEdited) {
            Text(
                text = "edited",
                color = metaColor,
                fontSize = 9.5.sp,
                fontStyle = FontStyle.Italic
            )
        }
        Text(
            text = timeText,
            color = metaColor,
            fontSize = 10.sp
        )
        if (isOutgoing) {
            MessageStatusIndicatorIcon(
                status = status,
                peerName = peerName,
                tint = metaColor
            )
        }
    }
}

@Composable
fun MessageStatusIndicatorIcon(
    status: String,
    peerName: String,
    tint: Color
) {
    when (status) {
        TelefamMessageStatus.SENDING.name -> {
            // Clock icon when sending
            TelefamComLucideIcons.ClockIcon(
                tint = tint,
                size = 12.dp
            )
        }

        TelefamMessageStatus.SENT.name -> {
            // One tick once sent
            TelefamComLucideIcons.SingleTickIcon(
                tint = tint,
                size = 13.dp
            )
        }

        TelefamMessageStatus.DELIVERED.name -> {
            // Red small icon with checkmark when delivered
            TelefamComLucideIcons.DeliveredRedBadgeIcon(
                badgeColor = Color(0xFFC80815),
                checkColor = Color.White,
                size = 14.dp
            )
        }

        TelefamMessageStatus.SEEN.name -> {
            // Receiver small avatar once seen
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFC80815))
                    .border(1.dp, Color.White, CircleShape)
            ) {
                Text(
                    text = peerName.take(1).uppercase(Locale.US),
                    color = Color.White,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun VoiceWaveformBars(
    barColor: Color,
    isAnimating: Boolean
) {
    val heights = remember {
        listOf(0.35f, 0.75f, 0.50f, 0.92f, 0.45f, 0.80f, 0.60f, 0.95f, 0.40f, 0.70f, 0.55f, 0.85f, 0.45f, 0.65f)
    }
    Canvas(
        modifier = Modifier
            .width(120.dp)
            .height(18.dp)
    ) {
        val barWidth = 3.dp.toPx()
        val gap = (size.width - (heights.size * barWidth)) / (heights.size - 1).coerceAtLeast(1)
        heights.forEachIndexed { index, ratio ->
            val factor = if (isAnimating && index % 2 == 0) (ratio * 0.7f) else ratio
            val barH = (size.height * factor).coerceAtLeast(3.dp.toPx())
            val top = (size.height - barH) / 2f
            drawRoundRect(
                color = barColor,
                topLeft = Offset(index * (barWidth + gap), top),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
private fun PeerTypingBubbleIndicator(peerName: String) {
    val telefamColors = LocalTelefamColors.current
    val transition = rememberInfiniteTransition(label = "typing_dots")
    val alpha1 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "d1"
    )
    val alpha2 by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "d2"
    )

    Surface(
        color = if (telefamColors.isDark) Color(0xFF1F242D) else Color.White.copy(alpha = 0.95f),
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "$peerName is typing",
                color = telefamColors.textSecondary,
                fontSize = 11.5.sp
            )
            Spacer(modifier = Modifier.width(2.dp))
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(telefamColors.brandRed.copy(alpha = alpha1))
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(telefamColors.brandRed.copy(alpha = alpha2))
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(telefamColors.brandRed.copy(alpha = alpha1))
            )
        }
    }
}

@Composable
private fun ChatBottomComposerBar(
    composerText: String,
    onComposerTextChanged: (String) -> Unit,
    replyingToMessage: TelefamMessageEntity?,
    editingMessage: TelefamMessageEntity?,
    peerName: String,
    peerEmail: String,
    onCancelReplyOrEdit: () -> Unit,
    isBlocked: Boolean,
    onUnblockClicked: () -> Unit,
    isHoldingMic: Boolean,
    recordingElapsedMs: Long,
    recordedPreviewPath: String?,
    recordedPreviewDurationMs: Long,
    isVoiceNoteViewOnce: Boolean,
    isPlayingPreviewAudio: Boolean,
    onToggleVoiceNoteViewOnce: () -> Unit,
    onPlayStopPreviewAudio: () -> Unit,
    onDiscardVoicePreview: () -> Unit,
    onSendVoicePreview: () -> Unit,
    onMicPressStart: () -> Unit,
    onMicPressRelease: () -> Unit,
    onOpenStickersSheet: () -> Unit,
    onFileAttachmentClicked: () -> Unit,
    onSendOrSaveTextClicked: () -> Unit
) {
    val telefamColors = LocalTelefamColors.current

    Surface(
        color = telefamColors.cardBackground,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Active Swipe-to-Reply or Owner Edit Banner above the input row
            AnimatedVisibility(visible = replyingToMessage != null || editingMessage != null) {
                val target = editingMessage ?: replyingToMessage
                if (target != null) {
                    val isEditMode = editingMessage != null
                    val headerTitle = if (isEditMode) {
                        "Editing Message"
                    } else {
                        val senderLabel = if (target.senderEmail == peerEmail) peerName else "You"
                        "Replying to $senderLabel"
                    }
                    val previewSubtitle = when (target.messageType) {
                        TelefamMessageType.STICKER.name -> "Sticker • ${target.plaintextContent}"
                        else -> target.plaintextContent
                    }

                    Surface(
                        color = telefamColors.inputBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(if (isEditMode) "composer_edit_banner" else "composer_reply_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            if (isEditMode) {
                                TelefamComLucideIcons.EditPencilLucideIcon(
                                    tint = telefamColors.brandRed,
                                    size = 17.dp
                                )
                            } else {
                                TelefamComLucideIcons.ReplyCurvedLucideIcon(
                                    tint = telefamColors.brandRed,
                                    size = 17.dp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = headerTitle,
                                    color = telefamColors.brandRed,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = previewSubtitle,
                                    color = telefamColors.textSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = onCancelReplyOrEdit,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("cancel_reply_or_edit_button")
                            ) {
                                TelefamComLucideIcons.XCloseLucideIcon(
                                    tint = telefamColors.textSecondary,
                                    size = 15.dp
                                )
                            }
                        }
                    }
                }
            }

            when {
                isBlocked -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "You blocked this contact.",
                            color = telefamColors.textSecondary,
                            fontSize = 13.sp
                        )
                        TextButton(onClick = onUnblockClicked) {
                            Text(
                                text = "Unblock",
                                color = telefamColors.brandRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                recordedPreviewPath != null -> {
                    // Voice Note Listen-First Preview Bar with View-Once toggle & Send button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("voice_note_preview_bar")
                    ) {
                        IconButton(
                            onClick = onDiscardVoicePreview,
                            modifier = Modifier.size(38.dp)
                        ) {
                            TelefamComLucideIcons.TrashDeleteLucideIcon(
                                tint = Color(0xFFD32F2F),
                                size = 19.dp
                            )
                        }

                        Surface(
                            color = telefamColors.inputBackground,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, telefamColors.inputBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                IconButton(
                                    onClick = onPlayStopPreviewAudio,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(telefamColors.brandRedTint)
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingPreviewAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Listen to recorded voice note",
                                        tint = telefamColors.brandRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    VoiceWaveformBars(
                                        barColor = telefamColors.brandRed,
                                        isAnimating = isPlayingPreviewAudio
                                    )
                                    val secs = (recordedPreviewDurationMs / 1000L).coerceAtLeast(1L)
                                    Text(
                                        text = String.format(Locale.US, "Preview • 0:%02d", secs),
                                        color = telefamColors.textSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                // View-Once Toggle Button ("1")
                                IconButton(
                                    onClick = onToggleVoiceNoteViewOnce,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("voice_note_view_once_toggle")
                                ) {
                                    TelefamComLucideIcons.ViewOnceOneIcon(
                                        tint = if (isVoiceNoteViewOnce) telefamColors.brandRed else telefamColors.textSecondary,
                                        isActive = isVoiceNoteViewOnce,
                                        size = 20.dp
                                    )
                                }
                            }
                        }

                        // Send Voice Note Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(telefamColors.brandRed)
                                .clickable(onClick = onSendVoicePreview)
                                .testTag("send_voice_note_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Voice Note",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                else -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        // Sticker Icon Button
                        IconButton(
                            onClick = onOpenStickersSheet,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("open_stickers_sheet_button")
                        ) {
                            TelefamComLucideIcons.StickerSmileIcon(
                                tint = telefamColors.brandRed,
                                size = 22.dp
                            )
                        }

                        // File Attachment Icon Button (Coming Soon)
                        IconButton(
                            onClick = onFileAttachmentClicked,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("file_attachment_coming_soon_button")
                        ) {
                            TelefamComLucideIcons.PaperclipIcon(
                                tint = telefamColors.textSecondary,
                                size = 21.dp
                            )
                        }

                        // Message Input Pill or Active Recording Timer Indicator
                        Surface(
                            color = telefamColors.inputBackground,
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.dp, telefamColors.inputBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.CenterStart,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp)
                            ) {
                                if (isHoldingMic) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .clip(CircleShape)
                                                .background(telefamColors.brandRed)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val recSec = (recordingElapsedMs / 1000L)
                                        Text(
                                            text = String.format(Locale.US, "Recording 0:%02d • Release to preview", recSec),
                                            color = telefamColors.brandRed,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    if (composerText.isEmpty()) {
                                        Text(
                                            text = if (editingMessage != null) "Edit message…" else "Message…",
                                            color = telefamColors.textSecondary.copy(alpha = 0.7f),
                                            fontSize = 14.sp
                                        )
                                    }
                                    BasicTextField(
                                        value = composerText,
                                        onValueChange = onComposerTextChanged,
                                        textStyle = TextStyle(
                                            color = telefamColors.textPrimary,
                                            fontSize = 14.sp
                                        ),
                                        cursorBrush = SolidColor(telefamColors.brandRed),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("chat_message_input_field")
                                    )
                                }
                            }
                        }

                        if (composerText.trim().isNotEmpty() || editingMessage != null) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(telefamColors.brandRed)
                                    .clickable(onClick = onSendOrSaveTextClicked)
                                    .testTag("chat_send_text_button")
                            ) {
                                Icon(
                                    imageVector = if (editingMessage != null) Icons.Default.Check else Icons.AutoMirrored.Filled.Send,
                                    contentDescription = if (editingMessage != null) "Save Edited Message" else "Send Message",
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        } else {
                            // Hold-to-Record / Tap-and-Release Voice Note Mic Button
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .scale(if (isHoldingMic) 1.16f else 1f)
                                    .clip(CircleShape)
                                    .background(
                                        if (isHoldingMic) telefamColors.brandRed else telefamColors.brandRedTint
                                    )
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                onMicPressStart()
                                                tryAwaitRelease()
                                                onMicPressRelease()
                                            }
                                        )
                                    }
                                    .testTag("chat_voice_record_button")
                            ) {
                                TelefamComLucideIcons.MicVoiceIcon(
                                    tint = if (isHoldingMic) Color.White else telefamColors.brandRed,
                                    size = 20.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
