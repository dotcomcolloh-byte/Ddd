package com.telefam.company.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.telefam.company.app.R
import com.telefam.company.app.data.TelefamConversationEntity
import com.telefam.company.app.data.TelefamMessageType
import com.telefam.company.app.data.TelefamPrivacyAudience
import com.telefam.company.app.ui.TelefamComViewModel
import com.telefam.company.app.ui.TelefamHomeTab
import com.telefam.company.app.ui.TelefamUiState
import com.telefam.company.app.ui.components.TelefamComLucideIcons
import com.telefam.company.app.ui.components.TelefamPaperPlaneLogo
import com.telefam.company.app.ui.theme.LocalTelefamColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TelefamComHomeScreen(
    uiState: TelefamUiState,
    conversations: List<TelefamConversationEntity>,
    viewModel: TelefamComViewModel,
    onTabSelected: (TelefamHomeTab) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onOpenConversation: (String) -> Unit,
    onStartNewChat: (peerEmail: String, peerName: String) -> Unit,
    onThreeDotsClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val telefamColors = LocalTelefamColors.current
    var showNewChatDialog by remember { mutableStateOf(false) }

    val filteredConversations = remember(conversations, uiState.searchQuery) {
        val q = uiState.searchQuery.trim()
        if (q.isEmpty()) {
            conversations
        } else {
            conversations.filter {
                it.peerFullName.contains(q, ignoreCase = true) ||
                    it.peerUsername.contains(q, ignoreCase = true) ||
                    it.peerEmail.contains(q, ignoreCase = true) ||
                    it.lastMessagePreview.contains(q, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = telefamColors.pageBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SlimRedTopHeaderSheet(
                searchQuery = uiState.searchQuery,
                onSearchQueryChanged = onSearchQueryChanged,
                onThreeDotsClicked = onThreeDotsClicked
            )
        },
        floatingActionButton = {
            if (uiState.selectedHomeTab == TelefamHomeTab.CHATS) {
                FloatingActionButton(
                    onClick = { showNewChatDialog = true },
                    containerColor = telefamColors.brandRed,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("start_new_chat_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "New Encrypted Chat"
                    )
                }
            }
        },
        bottomBar = {
            MediumTelefamBottomBar(
                selectedTab = uiState.selectedHomeTab,
                onTabSelected = onTabSelected
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = uiState.selectedHomeTab,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(160))
                },
                label = "home_tab_switch"
            ) { tab ->
                if (tab == TelefamHomeTab.CHATS) {
                    ChatsTabContent(
                        conversations = filteredConversations,
                        currentUserEmail = uiState.authenticatedEmail,
                        privacySettings = uiState.privacySettings,
                        onConversationClicked = onOpenConversation,
                        onStartNewChatClicked = { showNewChatDialog = true }
                    )
                } else if (tab == TelefamHomeTab.CONTACTS) {
                    TelefamComContactsScreen(
                        viewModel = viewModel,
                        onNavigateToProfile = { email -> /* TODO: Navigate to Profile */ }
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                    ) {
                        MinimalistComingSoonCenterpiece(featureLabel = tab.label)
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        StartNewEncryptedChatDialog(
            onStartChat = { email, name ->
                showNewChatDialog = false
                onStartNewChat(email, name)
            },
            onDismiss = { showNewChatDialog = false }
        )
    }
}

@Composable
private fun ChatsTabContent(
    conversations: List<TelefamConversationEntity>,
    currentUserEmail: String,
    privacySettings: com.telefam.company.app.data.TelefamPrivacySettingsState,
    onConversationClicked: (String) -> Unit,
    onStartNewChatClicked: () -> Unit
) {
    val telefamColors = LocalTelefamColors.current

    if (conversations.isEmpty()) {
        // Real empty state (NO seeded or mock conversations)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 340.dp)
            ) {
                TelefamPaperPlaneLogo(size = 72.dp)
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "No Chats Yet",
                    color = telefamColors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Start a new Signal end-to-end encrypted conversation with stickers, voice notes, and disappearing messages.",
                    color = telefamColors.textSecondary,
                    fontSize = 13.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onStartNewChatClicked,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = telefamColors.brandRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("empty_state_start_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Encrypted Chat",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_conversations_list")
        ) {
            items(
                items = conversations,
                key = { it.conversationId }
            ) { conv ->
                ConversationListItemRow(
                    conversation = conv,
                    isLastFromMe = conv.lastMessageSenderEmail.equals(currentUserEmail, ignoreCase = true) ||
                        conv.lastMessageSenderEmail != conv.peerEmail,
                    showOnlineIndicator = privacySettings.onlineIndicators == TelefamPrivacyAudience.ANYONE,
                    showTypingIndicator = privacySettings.typingIndicators == TelefamPrivacyAudience.ANYONE,
                    onClick = { onConversationClicked(conv.conversationId) }
                )
                HorizontalDivider(
                    color = telefamColors.inputBorder.copy(alpha = 0.55f),
                    thickness = 0.7.dp,
                    modifier = Modifier.padding(start = 78.dp, end = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun ConversationListItemRow(
    conversation: TelefamConversationEntity,
    isLastFromMe: Boolean,
    showOnlineIndicator: Boolean,
    showTypingIndicator: Boolean,
    onClick: () -> Unit
) {
    val telefamColors = LocalTelefamColors.current
    val formattedTime = remember(conversation.lastMessageTimestamp) {
        if (conversation.lastMessageTimestamp > 0L) {
            SimpleDateFormat("HH:mm", Locale.US).format(Date(conversation.lastMessageTimestamp))
        } else {
            ""
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("conversation_row_${conversation.conversationId}")
    ) {
        // Peer Avatar + Online Indicator Dot
        Box(modifier = Modifier.size(50.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(telefamColors.brandRedTint)
            ) {
                Text(
                    text = conversation.peerFullName.take(1).uppercase(Locale.US),
                    color = telefamColors.brandRed,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            if (conversation.isPeerOnline && showOnlineIndicator) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                        .border(2.dp, telefamColors.pageBackground, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = conversation.peerFullName,
                    color = telefamColors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formattedTime,
                    color = if (conversation.unreadCount > 0) telefamColors.brandRed else telefamColors.textSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (conversation.isPeerTyping && showTypingIndicator) {
                    Text(
                        text = "typing…",
                        color = telefamColors.brandRed,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    // Outgoing message status icon (Sending clock, Sent single tick, Delivered red badge, Seen avatar)
                    if (isLastFromMe && conversation.lastMessagePreview.isNotBlank()) {
                        MessageStatusIndicatorIcon(
                            status = conversation.lastMessageStatus,
                            peerName = conversation.peerFullName,
                            tint = telefamColors.textSecondary
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    }

                    // Lucide Mic Voice Icon when last message is a Voice Note (no emojis)
                    if (conversation.lastMessageType == TelefamMessageType.VOICE_NOTE.name) {
                        TelefamComLucideIcons.MicVoiceIcon(
                            tint = telefamColors.brandRed,
                            size = 15.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    } else if (conversation.lastMessageType == TelefamMessageType.STICKER.name) {
                        TelefamComLucideIcons.StickerSmileIcon(
                            tint = telefamColors.brandRed,
                            size = 15.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = conversation.lastMessagePreview.ifBlank { "Tap to send an encrypted message" },
                        color = if (conversation.unreadCount > 0) telefamColors.textPrimary else telefamColors.textSecondary,
                        fontSize = 13.5.sp,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Unread Message Count Badge when not opened (removed automatically when opened)
                if (conversation.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(20.dp)
                            .widthIn(min = 20.dp)
                            .clip(CircleShape)
                            .background(telefamColors.brandRed)
                            .padding(horizontal = 6.dp)
                            .testTag("unread_badge_${conversation.conversationId}")
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StartNewEncryptedChatDialog(
    onStartChat: (peerEmail: String, peerName: String) -> Unit,
    onDismiss: () -> Unit
) {
    val telefamColors = LocalTelefamColors.current
    var peerEmail by remember { mutableStateOf("") }
    var peerName by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = telefamColors.cardBackground,
            shape = RoundedCornerShape(22.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TelefamComLucideIcons.ShieldCheckIcon(
                        tint = telefamColors.brandRed,
                        size = 20.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New Encrypted Chat",
                        color = telefamColors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter the recipient's email or name to establish a Signal E2EE session.",
                    color = telefamColors.textSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = peerName,
                    onValueChange = {
                        peerName = it
                        errorText = null
                    },
                    singleLine = true,
                    label = { Text("Recipient Name") },
                    placeholder = { Text("e.g. Sarah Wanjiku") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = telefamColors.brandRed,
                        unfocusedBorderColor = telefamColors.inputBorder,
                        focusedTextColor = telefamColors.textPrimary,
                        unfocusedTextColor = telefamColors.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_chat_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = peerEmail,
                    onValueChange = {
                        peerEmail = it
                        errorText = null
                    },
                    singleLine = true,
                    label = { Text("Recipient Email or Username") },
                    placeholder = { Text("e.g. sarah@telefam.com") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = telefamColors.brandRed,
                        unfocusedBorderColor = telefamColors.inputBorder,
                        focusedTextColor = telefamColors.textPrimary,
                        unfocusedTextColor = telefamColors.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_chat_email_input")
                )

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorText!!,
                        color = Color(0xFFD32F2F),
                        fontSize = 12.sp
                    )
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
                        Text("Cancel", color = telefamColors.textPrimary)
                    }

                    Button(
                        onClick = {
                            val cleanEmail = peerEmail.trim().ifBlank {
                                if (peerName.isNotBlank()) {
                                    "${peerName.trim().lowercase(Locale.US).replace(" ", ".")}@telefam.com"
                                } else ""
                            }
                            if (cleanEmail.isBlank()) {
                                errorText = "Please enter a name or email"
                            } else {
                                val resolvedName = peerName.trim().ifBlank {
                                    cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                                }
                                onStartChat(cleanEmail, resolvedName)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = telefamColors.brandRed,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("confirm_new_chat_button")
                    ) {
                        Text("Start Chat", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SlimRedTopHeaderSheet(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onThreeDotsClicked: () -> Unit
) {
    val telefamColors = LocalTelefamColors.current

    Surface(
        color = telefamColors.brandRed,
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 8.dp)
        ) {
            // Slightly Slim Search Bar (34.dp height)
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color.White.copy(alpha = if (telefamColors.isDark) 0.16f else 0.95f))
                    .padding(horizontal = 12.dp)
                    .testTag("home_slim_search_bar")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (telefamColors.isDark) Color.White.copy(alpha = 0.85f) else telefamColors.brandRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_placeholder),
                                color = if (telefamColors.isDark) {
                                    Color.White.copy(alpha = 0.7f)
                                } else {
                                    Color(0xFF6B7280)
                                },
                                fontSize = 13.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChanged,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = if (telefamColors.isDark) Color.White else Color(0xFF1A1C1E),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(
                                if (telefamColors.isDark) Color.White else telefamColors.brandRed
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = if (telefamColors.isDark) Color.White else Color(0xFF6B7280),
                            modifier = Modifier
                                .size(15.dp)
                                .clickable { onSearchQueryChanged("") }
                        )
                    }
                }
            }

            // 3 Dots Top Bar Button -> Opens Privacy Settings
            IconButton(
                onClick = onThreeDotsClicked,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("top_bar_three_dots_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Privacy Settings",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun MediumTelefamBottomBar(
    selectedTab: TelefamHomeTab,
    onTabSelected: (TelefamHomeTab) -> Unit
) {
    val telefamColors = LocalTelefamColors.current

    Surface(
        color = telefamColors.cardBackground,
        tonalElevation = 4.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(0.5.dp, telefamColors.inputBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(64.dp)
                .padding(horizontal = 8.dp)
        ) {
            BottomNavTabItem(
                tab = TelefamHomeTab.CHATS,
                selected = selectedTab == TelefamHomeTab.CHATS,
                filledIcon = Icons.Filled.Chat,
                outlinedIcon = Icons.AutoMirrored.Outlined.Chat,
                onClick = { onTabSelected(TelefamHomeTab.CHATS) },
                modifier = Modifier.weight(1f)
            )

            BottomNavTabItem(
                tab = TelefamHomeTab.CONTACTS,
                selected = selectedTab == TelefamHomeTab.CONTACTS,
                filledIcon = Icons.Filled.Contacts,
                outlinedIcon = Icons.Outlined.Contacts,
                onClick = { onTabSelected(TelefamHomeTab.CONTACTS) },
                modifier = Modifier.weight(1f)
            )

            // Center "Post" Action Item
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f)
            ) {
                val isPostSelected = selectedTab == TelefamHomeTab.POST
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onTabSelected(TelefamHomeTab.POST) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("bottom_nav_post")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(
                                elevation = if (isPostSelected) 6.dp else 3.dp,
                                shape = CircleShape
                            )
                            .clip(CircleShape)
                            .background(telefamColors.brandRed)
                            .border(
                                width = if (isPostSelected) 2.dp else 0.dp,
                                color = if (isPostSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.nav_post),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.nav_post),
                        color = if (isPostSelected) telefamColors.brandRed else telefamColors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isPostSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            BottomNavTabItem(
                tab = TelefamHomeTab.FEEDS,
                selected = selectedTab == TelefamHomeTab.FEEDS,
                filledIcon = Icons.Filled.DynamicFeed,
                outlinedIcon = Icons.Outlined.DynamicFeed,
                onClick = { onTabSelected(TelefamHomeTab.FEEDS) },
                modifier = Modifier.weight(1f)
            )

            BottomNavTabItem(
                tab = TelefamHomeTab.PROFILE,
                selected = selectedTab == TelefamHomeTab.PROFILE,
                filledIcon = Icons.Filled.Person,
                outlinedIcon = Icons.Outlined.Person,
                onClick = { onTabSelected(TelefamHomeTab.PROFILE) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomNavTabItem(
    tab: TelefamHomeTab,
    selected: Boolean,
    filledIcon: ImageVector,
    outlinedIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val telefamColors = LocalTelefamColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("bottom_nav_${tab.name.lowercase()}")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (selected) telefamColors.brandRedTint else Color.Transparent
                )
                .padding(horizontal = 14.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = if (selected) filledIcon else outlinedIcon,
                contentDescription = tab.label,
                tint = if (selected) telefamColors.brandRed else telefamColors.textSecondary,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tab.label,
            color = if (selected) telefamColors.brandRed else telefamColors.textSecondary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun MinimalistComingSoonCenterpiece(
    featureLabel: String
) {
    val telefamColors = LocalTelefamColors.current

    val infiniteTransition = rememberInfiniteTransition(label = "coming_soon_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp)
            .padding(24.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.scale(pulseScale)
        ) {
            TelefamPaperPlaneLogo(size = 76.dp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            color = telefamColors.brandRedTint,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, telefamColors.brandRed.copy(alpha = 0.25f))
        ) {
            Text(
                text = featureLabel.uppercase(),
                color = telefamColors.brandRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.4.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.coming_soon),
            color = telefamColors.textPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("coming_soon_heading")
        )
    }
}
