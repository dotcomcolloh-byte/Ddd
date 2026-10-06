package com.telefam.company.app.data

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.telefam.company.app.R

enum class TelefamPrivacyAudience(val label: String) {
    ANYONE("Anyone"),
    NOBODY("Nobody");

    fun toggle(): TelefamPrivacyAudience = if (this == ANYONE) NOBODY else ANYONE
}

data class TelefamOfflineChatTheme(
    val id: String,
    val title: String,
    val subtitle: String,
    @DrawableRes val drawableResId: Int,
    val previewPrimaryColor: Color,
    val previewSecondaryColor: Color
)

data class TelefamBubbleColorOption(
    val id: String,
    val name: String,
    val color: Color
)

data class TelefamPrivacySettingsState(
    val messageRequests: TelefamPrivacyAudience = TelefamPrivacyAudience.ANYONE,
    val whoCanCallMe: TelefamPrivacyAudience = TelefamPrivacyAudience.ANYONE,
    val whoCanScreenshotChats: TelefamPrivacyAudience = TelefamPrivacyAudience.ANYONE,
    val whoCanShareChats: TelefamPrivacyAudience = TelefamPrivacyAudience.ANYONE,
    val whoCanCopyMessages: TelefamPrivacyAudience = TelefamPrivacyAudience.ANYONE,
    val whoCanDownloadMyMedia: TelefamPrivacyAudience = TelefamPrivacyAudience.ANYONE,
    val selectedChatThemeId: String = "crimson_aurora",
    val selectedBubbleColorId: String = "red",
    val readReceipts: TelefamPrivacyAudience = TelefamPrivacyAudience.ANYONE,
    val typingIndicators: TelefamPrivacyAudience = TelefamPrivacyAudience.ANYONE,
    val onlineIndicators: TelefamPrivacyAudience = TelefamPrivacyAudience.ANYONE
)

object TelefamComPrivacyCatalog {

    val offlineChatThemes: List<TelefamOfflineChatTheme> = listOf(
        TelefamOfflineChatTheme(
            id = "crimson_aurora",
            title = "Crimson Aurora",
            subtitle = "Abstract velvet waves & soft light",
            drawableResId = R.drawable.bg_chat_theme_crimson_aurora,
            previewPrimaryColor = Color(0xFFC80815),
            previewSecondaryColor = Color(0xFF4A0409)
        ),
        TelefamOfflineChatTheme(
            id = "emerald_dunes",
            title = "Emerald Dunes",
            subtitle = "Botanical geometric silk contours",
            drawableResId = R.drawable.bg_chat_theme_emerald_dunes,
            previewPrimaryColor = Color(0xFF10B981),
            previewSecondaryColor = Color(0xFF064E3B)
        ),
        TelefamOfflineChatTheme(
            id = "midnight_cosmos",
            title = "Midnight Cosmos",
            subtitle = "Deep sapphire nebula & starlight",
            drawableResId = R.drawable.bg_chat_theme_midnight_cosmos,
            previewPrimaryColor = Color(0xFF2563EB),
            previewSecondaryColor = Color(0xFF0F172A)
        ),
        TelefamOfflineChatTheme(
            id = "golden_horizon",
            title = "Golden Horizon",
            subtitle = "Warm amber sunset desert ridges",
            drawableResId = R.drawable.bg_chat_theme_golden_horizon,
            previewPrimaryColor = Color(0xFFF59E0B),
            previewSecondaryColor = Color(0xFF78350F)
        ),
        TelefamOfflineChatTheme(
            id = "amethyst_prism",
            title = "Amethyst Prism",
            subtitle = "Minimalist violet crystal facets",
            drawableResId = R.drawable.bg_chat_theme_amethyst_prism,
            previewPrimaryColor = Color(0xFF8B5CF6),
            previewSecondaryColor = Color(0xFF2E1065)
        )
    )

    val bubbleColors: List<TelefamBubbleColorOption> = listOf(
        TelefamBubbleColorOption(id = "red", name = "Telefam Red", color = Color(0xFFC80815)),
        TelefamBubbleColorOption(id = "green", name = "Emerald Green", color = Color(0xFF16A34A)),
        TelefamBubbleColorOption(id = "blue", name = "Ocean Blue", color = Color(0xFF2563EB)),
        TelefamBubbleColorOption(id = "purple", name = "Royal Purple", color = Color(0xFF7C3AED)),
        TelefamBubbleColorOption(id = "orange", name = "Sunset Orange", color = Color(0xFFEA580C)),
        TelefamBubbleColorOption(id = "teal", name = "Nordic Teal", color = Color(0xFF0D9488)),
        TelefamBubbleColorOption(id = "pink", name = "Rose Crimson", color = Color(0xFFE11D48)),
        TelefamBubbleColorOption(id = "indigo", name = "Midnight Indigo", color = Color(0xFF4F46E5))
    )

    fun getThemeById(id: String): TelefamOfflineChatTheme {
        return offlineChatThemes.find { it.id == id } ?: offlineChatThemes.first()
    }

    fun getBubbleColorById(id: String): TelefamBubbleColorOption {
        return bubbleColors.find { it.id == id } ?: bubbleColors.first()
    }
}
