package com.telefam.company.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telefam.company.app.data.TelefamMessageEntity
import com.telefam.company.app.data.TelefamMessageType
import com.telefam.company.app.ui.theme.LocalTelefamColors

private val QUICK_REACTION_EMOJIS = listOf("❤️", "👍", "😂", "😮", "😢", "🙏", "🔥")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelefamComMessageActionMenuSheet(
    message: TelefamMessageEntity,
    isOwner: Boolean,
    isCopyAllowedByPrivacy: Boolean,
    onReactSelected: (String) -> Unit,
    onReplyClicked: () -> Unit,
    onTogglePinClicked: () -> Unit,
    onToggleFavouriteClicked: () -> Unit,
    onCopyClicked: () -> Unit,
    onSelectClicked: () -> Unit,
    onEditClicked: () -> Unit,
    onUnsendForEveryoneClicked: () -> Unit,
    onDeleteForMeClicked: () -> Unit,
    onDismiss: () -> Unit
) {
    val telefamColors = LocalTelefamColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = telefamColors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp)
        ) {
            // 1. Emoji Reaction Bar at the top of the Long-Press Menu
            Surface(
                color = telefamColors.inputBackground,
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, telefamColors.inputBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("message_action_reaction_bar")
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 6.dp)
                ) {
                    QUICK_REACTION_EMOJIS.forEach { emoji ->
                        val isSelectedReaction = message.reactionEmoji == emoji
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelectedReaction) telefamColors.brandRedTint else Color.Transparent
                                )
                                .clickable {
                                    // Tapping the same emoji toggles it off; tapping a new one sets it
                                    val target = if (isSelectedReaction) "" else emoji
                                    onReactSelected(target)
                                    onDismiss()
                                }
                                .testTag("react_emoji_$emoji")
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 21.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Brief preview header of the selected message (Text, Sticker, or Voice Note)
            Surface(
                color = telefamColors.inputBackground.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    when (message.messageType) {
                        TelefamMessageType.STICKER.name -> {
                            TelefamComLucideIcons.StickerSmileIcon(
                                tint = telefamColors.brandRed,
                                size = 16.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sticker • ${message.plaintextContent}",
                                color = telefamColors.textSecondary,
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TelefamMessageType.VOICE_NOTE.name -> {
                            TelefamComLucideIcons.MicVoiceIcon(
                                tint = telefamColors.brandRed,
                                size = 16.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message.plaintextContent,
                                color = telefamColors.textSecondary,
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        else -> {
                            Text(
                                text = message.plaintextContent,
                                color = telefamColors.textSecondary,
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action List with Lucide Icons
            Surface(
                color = telefamColors.inputBackground,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, telefamColors.inputBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Reply
                    LucideMenuActionRow(
                        label = "Reply",
                        testTag = "menu_action_reply",
                        onClick = {
                            onReplyClicked()
                            onDismiss()
                        },
                        icon = {
                            TelefamComLucideIcons.ReplyCurvedLucideIcon(
                                tint = telefamColors.textPrimary,
                                size = 19.dp
                            )
                        }
                    )
                    MenuDivider()

                    // Pin / Unpin
                    LucideMenuActionRow(
                        label = if (message.isPinned) "Unpin Message" else "Pin Message",
                        testTag = "menu_action_pin",
                        onClick = {
                            onTogglePinClicked()
                            onDismiss()
                        },
                        icon = {
                            TelefamComLucideIcons.PinLucideIcon(
                                tint = if (message.isPinned) telefamColors.brandRed else telefamColors.textPrimary,
                                isFilled = message.isPinned,
                                size = 19.dp
                            )
                        }
                    )
                    MenuDivider()

                    // Mark Favourite / Unmark Favourite
                    LucideMenuActionRow(
                        label = if (message.isFavourite) "Remove from Favourites" else "Mark Favourite",
                        testTag = "menu_action_favourite",
                        onClick = {
                            onToggleFavouriteClicked()
                            onDismiss()
                        },
                        icon = {
                            TelefamComLucideIcons.StarFavouriteLucideIcon(
                                tint = if (message.isFavourite) Color(0xFFF59E0B) else telefamColors.textPrimary,
                                isFilled = message.isFavourite,
                                size = 19.dp
                            )
                        }
                    )

                    // Copy (Strictly hidden if privacy setting "Who Can Copy Messages" is Nobody)
                    if (isCopyAllowedByPrivacy) {
                        MenuDivider()
                        LucideMenuActionRow(
                            label = "Copy",
                            testTag = "menu_action_copy",
                            onClick = {
                                onCopyClicked()
                                onDismiss()
                            },
                            icon = {
                                TelefamComLucideIcons.CopyLucideIcon(
                                    tint = telefamColors.textPrimary,
                                    size = 19.dp
                                )
                            }
                        )
                    }

                    MenuDivider()

                    // Select (Multi-selection mode with Mark All & Delete)
                    LucideMenuActionRow(
                        label = "Select",
                        testTag = "menu_action_select",
                        onClick = {
                            onSelectClicked()
                            onDismiss()
                        },
                        icon = {
                            TelefamComLucideIcons.CheckSquareSelectLucideIcon(
                                tint = telefamColors.textPrimary,
                                isChecked = false,
                                size = 19.dp
                            )
                        }
                    )

                    // Owner-Only Extended Actions: Edit & Unsend (clears all traces on both users & backend)
                    if (isOwner && message.messageType == TelefamMessageType.TEXT.name) {
                        MenuDivider()
                        LucideMenuActionRow(
                            label = "Edit",
                            testTag = "menu_action_edit",
                            onClick = {
                                onEditClicked()
                                onDismiss()
                            },
                            icon = {
                                TelefamComLucideIcons.EditPencilLucideIcon(
                                    tint = telefamColors.brandRed,
                                    size = 19.dp
                                )
                            }
                        )
                    }

                    if (isOwner) {
                        MenuDivider()
                        LucideMenuActionRow(
                            label = "Unsend (Clear for Both Users)",
                            textColor = telefamColors.brandRed,
                            testTag = "menu_action_unsend",
                            onClick = {
                                onUnsendForEveryoneClicked()
                                onDismiss()
                            },
                            icon = {
                                TelefamComLucideIcons.UnsendRotateCcwLucideIcon(
                                    tint = telefamColors.brandRed,
                                    size = 19.dp
                                )
                            }
                        )
                    }

                    MenuDivider()

                    // Delete
                    LucideMenuActionRow(
                        label = "Delete",
                        textColor = Color(0xFFD32F2F),
                        testTag = "menu_action_delete",
                        onClick = {
                            onDeleteForMeClicked()
                            onDismiss()
                        },
                        icon = {
                            TelefamComLucideIcons.TrashDeleteLucideIcon(
                                tint = Color(0xFFD32F2F),
                                size = 19.dp
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LucideMenuActionRow(
    label: String,
    testTag: String,
    onClick: () -> Unit,
    textColor: Color = LocalTelefamColors.current.textPrimary,
    icon: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp)
            .testTag(testTag)
    ) {
        icon()
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MenuDivider() {
    val telefamColors = LocalTelefamColors.current
    HorizontalDivider(
        color = telefamColors.inputBorder.copy(alpha = 0.6f),
        thickness = 0.7.dp,
        modifier = Modifier.padding(start = 48.dp)
    )
}
