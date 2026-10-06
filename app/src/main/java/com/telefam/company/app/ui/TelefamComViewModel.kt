package com.telefam.company.app.ui

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.telefam.company.app.backend.TelefamComKtorBackend
import com.telefam.company.app.data.TelefamChatDao
import com.telefam.company.app.data.TelefamComAudioRecorderManager
import com.telefam.company.app.data.TelefamComContactsManager
import com.telefam.company.app.data.TelefamComCountries
import com.telefam.company.app.data.TelefamComRepository
import com.telefam.company.app.data.TelefamComStickerService
import com.telefam.company.app.data.TelefamConversationEntity
import com.telefam.company.app.data.TelefamCountry
import com.telefam.company.app.data.TelefamDirectoryUserEntity
import com.telefam.company.app.data.TelefamMessageEntity
import com.telefam.company.app.data.TelefamMessageStatus
import com.telefam.company.app.data.TelefamMessageType
import com.telefam.company.app.data.TelefamPendingActionEntity
import com.telefam.company.app.data.TelefamPrivacySettingsState
import com.telefam.company.app.data.TelefamSignalSessionEntity
import com.telefam.company.app.data.TelefamStickerItem
import com.telefam.company.app.data.TelefamUserProfileEntity
import com.telefam.company.app.data.TelefamUserReportEntity
import com.telefam.company.app.security.TelefamComSignalProtocolEngine
import com.telefam.company.app.ui.screens.PrivacyToggleKey
import com.telefam.company.app.ui.theme.TelefamThemeMode
import com.telefam.company.app.worker.TelefamComPendingActionsWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class TelefamScreenDestination {
    WELCOME,
    AUTH_EMAIL,
    AUTH_OTP,
    PROFILE_SETUP,
    HOME,
    PRIVACY_SETTINGS,
    INDIVIDUAL_CHAT,
    PUBLIC_PROFILE
}

enum class TelefamHomeTab(val label: String) {
    CHATS("Chats"),
    CONTACTS("Contacts"),
    POST("Post"),
    FEEDS("Feeds"),
    PROFILE("Profile")
}

data class TelefamUiState(
    val currentScreen: TelefamScreenDestination = TelefamScreenDestination.WELCOME,
    val themeMode: TelefamThemeMode = TelefamThemeMode.SYSTEM,

    // Auth state
    val emailInput: String = "",
    val emailError: String? = null,
    val otpDigits: String = "",
    val otpError: String? = null,
    val isSendingOtp: Boolean = false,
    val isVerifyingOtp: Boolean = false,
    val countdownSeconds: Int = 0,
    val dispatchedOtpCodeNotification: String? = null,
    val authStatusMessage: String? = null,
    val showGoogleAccountSheet: Boolean = false,

    // Authenticated user context
    val authenticatedEmail: String = "",
    val authenticatedUid: String = "",

    // Profile Setup state
    val fullName: String = "",
    val username: String = "",
    val selectedCountry: TelefamCountry = TelefamComCountries.defaultCountry,
    val phoneNumber: String = "",
    val location: String = "",
    val gender: String = "",
    val dateOfBirthFormatted: String = "",
    val dateOfBirthMillis: Long? = null,
    val bio: String = "",
    val profilePhotoPath: String = "",
    val profileError: String? = null,
    val isSavingProfile: Boolean = false,

    // Saved profile
    val savedProfile: TelefamUserProfileEntity? = null,

    // Home & Chats state
    val selectedHomeTab: TelefamHomeTab = TelefamHomeTab.CHATS,
    val searchQuery: String = "",
    val conversations: List<TelefamConversationEntity> = emptyList(),
    val activeConversationId: String? = null,
    val activeConversation: TelefamConversationEntity? = null,
    val activeMessages: List<TelefamMessageEntity> = emptyList(),
    val activeSignalSession: TelefamSignalSessionEntity? = null,

    // Stickers state
    val stickers: List<TelefamStickerItem> = emptyList(),
    val stickerSearchQuery: String = "",
    val stickerNextOffset: Int? = 0,
    val isLoadingStickers: Boolean = false,
    val isLoadingNextStickers: Boolean = false,
    val stickerError: String? = null,

    // 3-Dots Privacy Settings state
    val privacySettings: TelefamPrivacySettingsState = TelefamPrivacySettingsState(),
    
    // Public profile state
    val publicProfileEmail: String? = null
)

class TelefamComViewModel(
    private val appContext: Context,
    private val repository: TelefamComRepository,
    private val chatDao: TelefamChatDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(TelefamUiState())
    val uiState: StateFlow<TelefamUiState> = _uiState.asStateFlow()

    private val signalEngine = TelefamComSignalProtocolEngine(appContext, chatDao)
    private val stickerService = TelefamComStickerService(chatDao)
    private val audioRecorderManager = TelefamComAudioRecorderManager(appContext)

    private var countdownJob: Job? = null
    private var activeMessagesJob: Job? = null
    private var activeConversationJob: Job? = null
    private var activeSignalJob: Job? = null
    private var stickerSearchJob: Job? = null

    // Contacts Directory State
    private val _directoryUsers = MutableStateFlow<List<TelefamDirectoryUserEntity>>(emptyList())
    val directoryUsers: StateFlow<List<TelefamDirectoryUserEntity>> = _directoryUsers.asStateFlow()

    init {
        TelefamComKtorBackend.ensureStarted(appContext)
        initializeSession()

        viewModelScope.launch {
            repository.activeProfileFlow.collect { profile ->
                _uiState.update { it.copy(savedProfile = profile) }
            }
        }

        viewModelScope.launch {
            chatDao.observeAllConversations().collect { convList ->
                _uiState.update { it.copy(conversations = convList) }
            }
        }

        // Subscribe to real-time Ktor Backend events so all actions update live without refresh
        viewModelScope.launch {
            TelefamComKtorBackend.realtimeEventFlow.collect { event ->
                handleRealtimeBackendEvent(event)
            }
        }

        // Pre-fetch initial sticker pack into Room cache
        loadStickers(query = "", reset = true)
        
        // Observe Directory
        viewModelScope.launch {
             val myEmail = repository.getLatestProfile()?.email ?: ""
             chatDao.observeAllDirectoryUsers(myEmail).collect {
                 _directoryUsers.value = it
             }
        }
    }

    fun syncContacts() {
        val manager = TelefamComContactsManager(appContext)
        if (!manager.hasContactsPermission()) return
        
        viewModelScope.launch {
            val hashes = manager.readAndHashDeviceContacts()
            val myEmail = repository.getLatestProfile()?.email ?: ""
            val remoteUsers = TelefamComKtorBackend.uploadContactHashesToKtor(
                myEmail, 
                hashes
            )
            remoteUsers?.let { users ->
                chatDao.upsertDirectoryUsers(users)
            }
        }
    }

    fun toggleFollowUser(targetEmail: String, follow: Boolean) {
        val manager = TelefamComContactsManager(appContext)
        val check = manager.checkAndRecordFollowActionRateLimit()
        if (!check.allowed) {
            // Should show some UI error, for now silently fail/log
            return
        }

        viewModelScope.launch {
            val myEmail = repository.getLatestProfile()?.email ?: ""
            val result = TelefamComKtorBackend.postFollowActionToKtor(
                myEmail,
                targetEmail,
                follow
            )
            if (result.success) {
                chatDao.updateFollowRelationship(
                    targetEmail,
                    result.isFollowing,
                    result.isFollowedBy,
                    result.targetFollowersCount,
                    result.targetFollowingCount
                )
            }
        }
    }

    private suspend fun handleRealtimeBackendEvent(event: com.telefam.company.app.backend.TelefamRealtimeEvent) {
        val payload = runCatching { JSONObject(event.payloadJson) }.getOrNull()
        when (event.eventType) {
            "MESSAGE_UNSENT" -> {
                val msgId = event.messageId
                if (msgId.isNotBlank()) {
                    chatDao.deleteMessageById(msgId)
                    chatDao.clearReplyReferencesToDeletedMessage(msgId)
                    chatDao.deletePendingActionsForMessage(msgId)
                    syncConversationLastPreview(event.conversationId)
                }
            }
            "MESSAGE_REACTED" -> {
                val emoji = payload?.optString("reactionEmoji", "")?.ifBlank { null }
                if (event.messageId.isNotBlank()) {
                    chatDao.updateMessageReaction(event.messageId, emoji)
                }
            }
            "MESSAGE_PINNED" -> {
                val pinned = payload?.optBoolean("isPinned", false) ?: false
                if (event.messageId.isNotBlank()) {
                    chatDao.updateMessagePinned(event.messageId, pinned)
                }
            }
            "MESSAGE_FAVOURITED" -> {
                val fav = payload?.optBoolean("isFavourite", false) ?: false
                if (event.messageId.isNotBlank()) {
                    chatDao.updateMessageFavourite(event.messageId, fav)
                }
            }
            "PRESENCE_CHANGED" -> {
                val isOnline = payload?.optBoolean("isOnline", true) ?: true
                val isTyping = payload?.optBoolean("isTyping", false) ?: false
                if (event.conversationId.isNotBlank()) {
                    chatDao.updatePeerPresence(event.conversationId, isOnline, isTyping)
                }
            }
        }
    }

    private fun initializeSession() {
        val savedModeStr = repository.getSavedThemeMode()
        val initialTheme = runCatching { TelefamThemeMode.valueOf(savedModeStr) }
            .getOrDefault(TelefamThemeMode.SYSTEM)
        val initialPrivacy = repository.loadPrivacySettings()

        _uiState.update {
            it.copy(
                themeMode = initialTheme,
                privacySettings = initialPrivacy
            )
        }

        viewModelScope.launch {
            chatDao.purgeExpiredDisappearingMessages(System.currentTimeMillis())
            TelefamComPendingActionsWorker.enqueueSync(appContext)

            val activeEmail = repository.getCurrentSignedInEmail()
            if (!activeEmail.isNullOrBlank()) {
                val existing = repository.getSavedProfileForEmail(activeEmail)
                if (existing != null && existing.isProfileCompleted) {
                    _uiState.update {
                        it.copy(
                            authenticatedEmail = activeEmail,
                            authenticatedUid = existing.uid,
                            savedProfile = existing,
                            currentScreen = TelefamScreenDestination.HOME
                        )
                    }
                    return@launch
                } else {
                    prefillProfileFieldsFromEmail(activeEmail, existing?.fullName)
                    _uiState.update {
                        it.copy(
                            authenticatedEmail = activeEmail,
                            currentScreen = TelefamScreenDestination.PROFILE_SETUP
                        )
                    }
                    return@launch
                }
            }

            if (repository.hasSeenWelcome()) {
                _uiState.update { it.copy(currentScreen = TelefamScreenDestination.AUTH_EMAIL) }
            } else {
                _uiState.update { it.copy(currentScreen = TelefamScreenDestination.WELCOME) }
            }
        }
    }

    fun setThemeMode(mode: TelefamThemeMode) {
        repository.setSavedThemeMode(mode.name)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun onWelcomeFinished() {
        repository.markWelcomeSeen()
        _uiState.update { it.copy(currentScreen = TelefamScreenDestination.AUTH_EMAIL) }
    }

    fun onEmailChanged(newEmail: String) {
        _uiState.update {
            it.copy(
                emailInput = newEmail,
                emailError = null,
                authStatusMessage = null
            )
        }
    }

    fun sendOtpCode() {
        val email = _uiState.value.emailInput.trim()
        if (!repository.isValidEmail(email)) {
            _uiState.update { it.copy(emailError = "Please enter a valid email address") }
            return
        }

        _uiState.update { it.copy(isSendingOtp = true, emailError = null, otpError = null) }
        viewModelScope.launch {
            val result = repository.sendPasswordlessOtpToEmail(email)
            if (result.success) {
                startCountdownTimer(result.rateLimitedSecondsRemaining.coerceAtLeast(30))
                _uiState.update {
                    it.copy(
                        isSendingOtp = false,
                        emailInput = result.email,
                        otpDigits = "",
                        otpError = null,
                        dispatchedOtpCodeNotification = result.generatedCode,
                        authStatusMessage = result.message,
                        currentScreen = TelefamScreenDestination.AUTH_OTP
                    )
                }
            } else {
                if (result.rateLimitedSecondsRemaining > 0) {
                    startCountdownTimer(result.rateLimitedSecondsRemaining)
                }
                _uiState.update {
                    it.copy(
                        isSendingOtp = false,
                        emailError = result.message,
                        otpError = result.message
                    )
                }
            }
        }
    }

    fun resendOtpCode() {
        val currentCountdown = _uiState.value.countdownSeconds
        if (currentCountdown > 0) {
            _uiState.update {
                it.copy(otpError = "Please wait ${formatTimer(currentCountdown)} before resending")
            }
            return
        }
        sendOtpCode()
    }

    private fun startCountdownTimer(seconds: Int) {
        countdownJob?.cancel()
        _uiState.update { it.copy(countdownSeconds = seconds) }
        countdownJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000L)
                remaining -= 1
                _uiState.update { it.copy(countdownSeconds = remaining) }
            }
        }
    }

    fun onOtpDigitsChanged(newOtp: String) {
        val filtered = newOtp.filter { it.isDigit() }.take(6)
        _uiState.update {
            it.copy(
                otpDigits = filtered,
                otpError = null
            )
        }
    }

    fun openEmailAppWithOtp() {
        val email = _uiState.value.emailInput
        val code = _uiState.value.dispatchedOtpCodeNotification ?: return
        runCatching {
            val intent = repository.createEmailClientIntent(email, code)
            appContext.startActivity(intent)
        }
    }

    fun verifyOtpCode() {
        val email = _uiState.value.emailInput.trim()
        val otp = _uiState.value.otpDigits.trim()
        if (otp.length < 6) {
            _uiState.update { it.copy(otpError = "Please enter all 6 digits of your OTP") }
            return
        }

        _uiState.update { it.copy(isVerifyingOtp = true, otpError = null) }
        viewModelScope.launch {
            val verifyResult = repository.verifyEmailOtp(email, otp)
            if (verifyResult.success) {
                prefillProfileFieldsFromEmail(verifyResult.email, verifyResult.displayName)
                val targetScreen = if (verifyResult.isFirstTimeProfileNeeded) {
                    TelefamScreenDestination.PROFILE_SETUP
                } else {
                    TelefamScreenDestination.HOME
                }
                _uiState.update {
                    it.copy(
                        isVerifyingOtp = false,
                        authenticatedEmail = verifyResult.email,
                        authenticatedUid = verifyResult.uid,
                        dispatchedOtpCodeNotification = null,
                        currentScreen = targetScreen
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isVerifyingOtp = false,
                        otpError = verifyResult.errorMessage
                    )
                }
            }
        }
    }

    fun onChangeEmailClicked() {
        countdownJob?.cancel()
        _uiState.update {
            it.copy(
                currentScreen = TelefamScreenDestination.AUTH_EMAIL,
                otpDigits = "",
                otpError = null,
                dispatchedOtpCodeNotification = null
            )
        }
    }

    fun initiateGoogleSignIn(activity: Activity) {
        viewModelScope.launch {
            val result = repository.signInWithGoogle(activity, fallbackGoogleEmail = null)
            if (result.success) {
                prefillProfileFieldsFromEmail(result.email, result.displayName)
                val target = if (result.isFirstTimeProfileNeeded) {
                    TelefamScreenDestination.PROFILE_SETUP
                } else {
                    TelefamScreenDestination.HOME
                }
                _uiState.update {
                    it.copy(
                        authenticatedEmail = result.email,
                        authenticatedUid = result.uid,
                        showGoogleAccountSheet = false,
                        currentScreen = target
                    )
                }
            } else if (!result.errorMessage.isNullOrBlank() && result.errorMessage != "GOOGLE_ACCOUNT_PROMPT_REQUIRED") {
                _uiState.update { it.copy(emailError = result.errorMessage) }
            }
        }
    }

    private fun prefillProfileFieldsFromEmail(email: String, explicitDisplayName: String? = null) {
        val autoFullName = if (!explicitDisplayName.isNullOrBlank()) {
            explicitDisplayName
        } else {
            TelefamComRepository.deriveFullNameFromEmail(email)
        }
        val autoUsername = TelefamComRepository.deriveUsernameFromEmail(email)

        _uiState.update { state ->
            state.copy(
                fullName = state.fullName.ifBlank { autoFullName },
                username = state.username.ifBlank { autoUsername },
                profileError = null
            )
        }
    }

    fun onFullNameChanged(value: String) {
        _uiState.update { it.copy(fullName = value, profileError = null) }
    }

    fun onUsernameChanged(value: String) {
        val sanitized = value.lowercase(Locale.US).replace(" ", "_")
        _uiState.update { it.copy(username = sanitized, profileError = null) }
    }

    fun onCountrySelected(country: TelefamCountry) {
        _uiState.update { it.copy(selectedCountry = country, profileError = null) }
    }

    fun onPhoneNumberChanged(value: String) {
        val filtered = value.filter { it.isDigit() || it == ' ' || it == '-' }
        _uiState.update { it.copy(phoneNumber = filtered, profileError = null) }
    }

    fun onLocationChanged(value: String) {
        _uiState.update { it.copy(location = value, profileError = null) }
    }

    fun onGenderSelected(value: String) {
        _uiState.update { it.copy(gender = value, profileError = null) }
    }

    fun onDateOfBirthSelected(millis: Long) {
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val formatted = formatter.format(Date(millis))
        _uiState.update {
            it.copy(
                dateOfBirthMillis = millis,
                dateOfBirthFormatted = formatted,
                profileError = null
            )
        }
    }

    fun onBioChanged(value: String) {
        if (value.length <= 150) {
            _uiState.update { it.copy(bio = value, profileError = null) }
        }
    }

    fun saveCroppedProfileBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            runCatching {
                val avatarsDir = File(appContext.filesDir, "telefam_avatars").apply { mkdirs() }
                val outFile = File(avatarsDir, "avatar_${System.currentTimeMillis()}.jpg")
                FileOutputStream(outFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
                }
                _uiState.update {
                    it.copy(
                        profilePhotoPath = outFile.absolutePath,
                        profileError = null
                    )
                }
            }
        }
    }

    fun submitProfileSetup() {
        val state = _uiState.value
        if (state.fullName.trim().length < 2) {
            _uiState.update { it.copy(profileError = "Please enter your full name") }
            return
        }
        if (state.username.trim().length < 3) {
            _uiState.update { it.copy(profileError = "Username must be at least 3 characters") }
            return
        }
        val phoneErr = TelefamComCountries.validatePhoneDigits(state.selectedCountry, state.phoneNumber)
        if (phoneErr != null) {
            _uiState.update { it.copy(profileError = phoneErr) }
            return
        }
        if (state.location.trim().isEmpty()) {
            _uiState.update { it.copy(profileError = "Please enter your location") }
            return
        }
        if (state.gender.isBlank()) {
            _uiState.update { it.copy(profileError = "Please select your gender") }
            return
        }
        if (state.dateOfBirthFormatted.isBlank() || state.dateOfBirthMillis == null) {
            _uiState.update { it.copy(profileError = "Please select your date of birth (13+ years)") }
            return
        }

        _uiState.update { it.copy(isSavingProfile = true, profileError = null) }
        viewModelScope.launch {
            val cleanPhoneDigits = state.phoneNumber.filter { it.isDigit() }
            val emailKey = state.authenticatedEmail.ifBlank {
                state.emailInput.trim().lowercase(Locale.US).ifBlank { "member@telefam.com" }
            }
            val entity = TelefamUserProfileEntity(
                email = emailKey,
                uid = state.authenticatedUid.ifBlank { "telefam_${emailKey.hashCode().toUInt()}" },
                fullName = state.fullName.trim(),
                username = state.username.trim().removePrefix("@"),
                countryIso = state.selectedCountry.isoCode,
                countryDialCode = state.selectedCountry.dialCode,
                phoneNumber = cleanPhoneDigits,
                location = state.location.trim(),
                gender = state.gender,
                dateOfBirth = state.dateOfBirthFormatted,
                dateOfBirthMillis = state.dateOfBirthMillis,
                bio = state.bio.trim(),
                photoPath = state.profilePhotoPath,
                isProfileCompleted = true
            )
            repository.saveUserProfile(entity)
            _uiState.update {
                it.copy(
                    isSavingProfile = false,
                    savedProfile = entity,
                    currentScreen = TelefamScreenDestination.HOME
                )
            }
        }
    }

    fun onBackFromProfileSetup() {
        _uiState.update {
            it.copy(
                currentScreen = TelefamScreenDestination.AUTH_EMAIL,
                profileError = null
            )
        }
    }

    fun onHomeTabSelected(tab: TelefamHomeTab) {
        _uiState.update { it.copy(selectedHomeTab = tab) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    // ---------------------------------------------------------
    // Individual Chats + Signal E2EE + Ktor Backend + Room Queue
    // ---------------------------------------------------------

    fun startOrOpenEncryptedChat(peerEmailRaw: String, peerNameRaw: String) {
        val peerEmail = peerEmailRaw.trim().lowercase(Locale.US)
        val peerName = peerNameRaw.trim().ifBlank { TelefamComRepository.deriveFullNameFromEmail(peerEmail) }
        val peerUsername = TelefamComRepository.deriveUsernameFromEmail(peerEmail)
        val convId = "conv_${peerEmail.replace(Regex("[^a-z0-9]"), "_")}"
        val myEmail = currentLocalUserEmail()

        viewModelScope.launch {
            val existingConv = chatDao.getConversationById(convId)
            if (existingConv == null) {
                // Fetch peer prekey bundle from Ktor Edge Backend
                val bundleJson = TelefamComKtorBackend.fetchPeerPreKeyBundleFromKtor(peerEmail)
                val peerIdentityPubB64 = bundleJson?.optString("identityPublicKeyBase64")
                val peerSignedPreKeyB64 = bundleJson?.optString("signedPreKeyPublicBase64").orEmpty()
                val peerRegId = bundleJson?.optInt("registrationId", 24109) ?: 24109

                val session = signalEngine.getOrEstablishSession(
                    localUserEmail = myEmail,
                    peerEmail = peerEmail,
                    peerIdentityPublicKeyFromServer = peerIdentityPubB64
                )

                val newConversation = TelefamConversationEntity(
                    conversationId = convId,
                    peerEmail = peerEmail,
                    peerFullName = peerName,
                    peerUsername = peerUsername,
                    peerAvatarUrl = "",
                    lastMessagePreview = "",
                    lastMessageType = TelefamMessageType.TEXT.name,
                    lastMessageStatus = TelefamMessageStatus.SEEN.name,
                    lastMessageSenderEmail = myEmail,
                    lastMessageTimestamp = System.currentTimeMillis(),
                    unreadCount = 0,
                    isPeerOnline = true,
                    isPeerTyping = false,
                    isBlocked = false,
                    disappearingTimerMillis = 0L,
                    peerIdentityPublicKeyBase64 = session.peerIdentityPublicKeyBase64,
                    peerSignedPreKeyBase64 = peerSignedPreKeyB64,
                    peerRegistrationId = peerRegId
                )
                chatDao.upsertConversation(newConversation)
            }
            openConversationById(convId)
        }
    }

    fun openConversationById(conversationId: String) {
        activeMessagesJob?.cancel()
        activeConversationJob?.cancel()
        activeSignalJob?.cancel()

        viewModelScope.launch {
            // Immediately clear unread count when conversation is opened
            chatDao.clearUnreadCount(conversationId)
            chatDao.markAllConversationMessagesSeen(conversationId)
            chatDao.purgeExpiredDisappearingMessages(System.currentTimeMillis())

            val conv = chatDao.getConversationById(conversationId) ?: return@launch
            val myEmail = currentLocalUserEmail()
            signalEngine.getOrEstablishSession(
                localUserEmail = myEmail,
                peerEmail = conv.peerEmail,
                peerIdentityPublicKeyFromServer = conv.peerIdentityPublicKeyBase64.ifBlank { null }
            )

            _uiState.update {
                it.copy(
                    activeConversationId = conversationId,
                    activeConversation = conv,
                    currentScreen = TelefamScreenDestination.INDIVIDUAL_CHAT
                )
            }

            activeConversationJob = viewModelScope.launch {
                chatDao.observeConversationById(conversationId).collect { updatedConv ->
                    _uiState.update { it.copy(activeConversation = updatedConv) }
                }
            }

            activeMessagesJob = viewModelScope.launch {
                chatDao.observeMessagesForConversation(
                    conversationId = conversationId,
                    nowMillis = System.currentTimeMillis()
                ).collect { msgList ->
                    _uiState.update { it.copy(activeMessages = msgList) }
                }
            }

            activeSignalJob = viewModelScope.launch {
                chatDao.observeSignalSession(conv.peerEmail).collect { session ->
                    _uiState.update { it.copy(activeSignalSession = session) }
                }
            }
        }
    }

    fun closeIndividualChatBackToHome() {
        audioRecorderManager.stopPlayback()
        val convId = _uiState.value.activeConversationId
        if (convId != null) {
            viewModelScope.launch {
                chatDao.clearUnreadCount(convId)
            }
        }
        _uiState.update {
            it.copy(currentScreen = TelefamScreenDestination.HOME)
        }
    }

    fun sendTextMessage(text: String, replyTarget: TelefamMessageEntity? = null) {
        val conv = _uiState.value.activeConversation ?: return
        if (conv.isBlocked) return
        dispatchEncryptedMessageInternal(
            conversation = conv,
            messageType = TelefamMessageType.TEXT,
            plaintextContent = text.trim(),
            mediaUrl = "",
            localAudioPath = "",
            audioDurationMs = 0L,
            isViewOnce = false,
            replyTarget = replyTarget
        )
    }

    fun sendStickerMessage(sticker: TelefamStickerItem, replyTarget: TelefamMessageEntity? = null) {
        val conv = _uiState.value.activeConversation ?: return
        if (conv.isBlocked) return
        dispatchEncryptedMessageInternal(
            conversation = conv,
            messageType = TelefamMessageType.STICKER,
            plaintextContent = sticker.title.ifBlank { "Sticker" },
            mediaUrl = sticker.fullUrl,
            localAudioPath = "",
            audioDurationMs = 0L,
            isViewOnce = false,
            replyTarget = replyTarget
        )
    }

    fun sendVoiceNoteMessage(
        audioPath: String,
        durationMs: Long,
        isViewOnce: Boolean,
        replyTarget: TelefamMessageEntity? = null
    ) {
        val conv = _uiState.value.activeConversation ?: return
        if (conv.isBlocked) return
        val label = if (isViewOnce) "View-once voice note" else "Voice note (${(durationMs / 1000L).coerceAtLeast(1)}s)"
        dispatchEncryptedMessageInternal(
            conversation = conv,
            messageType = TelefamMessageType.VOICE_NOTE,
            plaintextContent = label,
            mediaUrl = "",
            localAudioPath = audioPath,
            audioDurationMs = durationMs,
            isViewOnce = isViewOnce,
            replyTarget = replyTarget
        )
    }

    private fun dispatchEncryptedMessageInternal(
        conversation: TelefamConversationEntity,
        messageType: TelefamMessageType,
        plaintextContent: String,
        mediaUrl: String,
        localAudioPath: String,
        audioDurationMs: Long,
        isViewOnce: Boolean,
        replyTarget: TelefamMessageEntity?
    ) {
        val myEmail = currentLocalUserEmail()
        val messageId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()
        val expiresAt = if (conversation.disappearingTimerMillis > 0L) {
            now + conversation.disappearingTimerMillis
        } else {
            null
        }

        val replySenderName = replyTarget?.let { target ->
            if (target.senderEmail == conversation.peerEmail) {
                conversation.peerFullName
            } else {
                _uiState.value.savedProfile?.fullName?.ifBlank { "You" } ?: "You"
            }
        }
        val replyPreview = replyTarget?.let { target ->
            when (target.messageType) {
                TelefamMessageType.STICKER.name -> "Sticker • ${target.plaintextContent}"
                TelefamMessageType.VOICE_NOTE.name -> target.plaintextContent
                else -> target.plaintextContent
            }
        }

        viewModelScope.launch {
            // 1. Encrypt with Signal Protocol (X3DH + Double Ratchet AES-256-GCM)
            val envelope = signalEngine.encryptOutgoingMessage(
                localUserEmail = myEmail,
                peerEmail = conversation.peerEmail,
                plaintext = plaintextContent
            )

            // 2. Insert into local Room immediately with SENDING status (Clock icon)
            val entity = TelefamMessageEntity(
                messageId = messageId,
                conversationId = conversation.conversationId,
                senderEmail = myEmail,
                receiverEmail = conversation.peerEmail,
                messageType = messageType.name,
                plaintextContent = plaintextContent,
                cipherTextBase64 = envelope.cipherTextBase64,
                ephemeralPublicKeyBase64 = envelope.ephemeralPublicKeyBase64,
                ratchetStepIndex = envelope.ratchetStepIndex,
                mediaUrl = mediaUrl,
                localAudioPath = localAudioPath,
                audioDurationMs = audioDurationMs,
                isViewOnce = isViewOnce,
                isViewOnceConsumed = false,
                status = TelefamMessageStatus.SENDING.name,
                replyToMessageId = replyTarget?.messageId,
                replyToSenderName = replySenderName,
                replyToPreviewText = replyPreview,
                replyToMessageType = replyTarget?.messageType,
                createdAt = now,
                expiresAt = expiresAt
            )
            chatDao.insertMessage(entity)

            val updatedConv = conversation.copy(
                lastMessagePreview = plaintextContent,
                lastMessageType = messageType.name,
                lastMessageStatus = TelefamMessageStatus.SENDING.name,
                lastMessageSenderEmail = myEmail,
                lastMessageTimestamp = now
            )
            chatDao.upsertConversation(updatedConv)

            // 3. Queue in Room telefam_pending_actions & trigger WorkManager for offline-resilient delivery
            val payloadJson = JSONObject()
                .put("messageId", messageId)
                .put("conversationId", conversation.conversationId)
                .put("senderEmail", myEmail)
                .put("receiverEmail", conversation.peerEmail)
                .put("messageType", messageType.name)
                .put("cipherTextBase64", envelope.cipherTextBase64)
                .put("ephemeralPublicKeyBase64", envelope.ephemeralPublicKeyBase64)
                .put("ratchetStepIndex", envelope.ratchetStepIndex)
                .apply {
                    if (replyTarget != null) put("replyToMessageId", replyTarget.messageId)
                    if (expiresAt != null) put("expiresAt", expiresAt)
                }

            val actionId = chatDao.enqueuePendingAction(
                TelefamPendingActionEntity(
                    actionType = "SEND_MESSAGE",
                    conversationId = conversation.conversationId,
                    targetMessageId = messageId,
                    payloadJson = payloadJson.toString()
                )
            )

            // Execute immediate coroutine sync + WorkManager retry fallback
            TelefamComPendingActionsWorker.enqueueSync(appContext)
            processImmediateMessageDeliveryLifecycle(
                actionId = actionId,
                conversationId = conversation.conversationId,
                messageId = messageId,
                payload = payloadJson
            )
        }
    }

    fun reactToMessage(messageId: String, emoji: String) {
        val conv = _uiState.value.activeConversation ?: return
        val normalizedEmoji = emoji.trim().ifBlank { null }
        viewModelScope.launch {
            chatDao.updateMessageReaction(messageId, normalizedEmoji)
            val payload = JSONObject()
                .put("messageId", messageId)
                .put("conversationId", conv.conversationId)
                .put("senderEmail", currentLocalUserEmail())
                .put("reactionEmoji", normalizedEmoji ?: "")

            val actionId = chatDao.enqueuePendingAction(
                TelefamPendingActionEntity(
                    actionType = "REACT_MESSAGE",
                    conversationId = conv.conversationId,
                    targetMessageId = messageId,
                    payloadJson = payload.toString()
                )
            )
            if (TelefamComKtorBackend.postReactionToKtor(payload)) {
                chatDao.deletePendingAction(actionId)
            } else {
                TelefamComPendingActionsWorker.enqueueSync(appContext)
            }
        }
    }

    fun togglePinMessage(messageId: String, pin: Boolean) {
        val conv = _uiState.value.activeConversation ?: return
        viewModelScope.launch {
            chatDao.updateMessagePinned(messageId, pin)
            val payload = JSONObject()
                .put("messageId", messageId)
                .put("conversationId", conv.conversationId)
                .put("isPinned", pin)

            val actionId = chatDao.enqueuePendingAction(
                TelefamPendingActionEntity(
                    actionType = "PIN_MESSAGE",
                    conversationId = conv.conversationId,
                    targetMessageId = messageId,
                    payloadJson = payload.toString()
                )
            )
            if (TelefamComKtorBackend.postPinToKtor(payload)) {
                chatDao.deletePendingAction(actionId)
            } else {
                TelefamComPendingActionsWorker.enqueueSync(appContext)
            }
        }
    }

    fun toggleFavouriteMessage(messageId: String, favourite: Boolean) {
        val conv = _uiState.value.activeConversation ?: return
        viewModelScope.launch {
            chatDao.updateMessageFavourite(messageId, favourite)
            val payload = JSONObject()
                .put("messageId", messageId)
                .put("conversationId", conv.conversationId)
                .put("isFavourite", favourite)

            val actionId = chatDao.enqueuePendingAction(
                TelefamPendingActionEntity(
                    actionType = "FAV_MESSAGE",
                    conversationId = conv.conversationId,
                    targetMessageId = messageId,
                    payloadJson = payload.toString()
                )
            )
            if (TelefamComKtorBackend.postFavouriteToKtor(payload)) {
                chatDao.deletePendingAction(actionId)
            } else {
                TelefamComPendingActionsWorker.enqueueSync(appContext)
            }
        }
    }

    fun editOwnMessage(messageId: String, newPlaintextRaw: String) {
        val conv = _uiState.value.activeConversation ?: return
        val newPlaintext = newPlaintextRaw.trim()
        if (newPlaintext.isEmpty()) return
        val myEmail = currentLocalUserEmail()
        val editedAt = System.currentTimeMillis()

        viewModelScope.launch {
            val targetMsg = chatDao.getMessageById(messageId) ?: return@launch
            if (targetMsg.senderEmail != myEmail) return@launch

            val envelope = signalEngine.encryptOutgoingMessage(
                localUserEmail = myEmail,
                peerEmail = conv.peerEmail,
                plaintext = newPlaintext
            )

            chatDao.updateEditedMessage(
                messageId = messageId,
                newPlaintext = newPlaintext,
                newCipherTextBase64 = envelope.cipherTextBase64,
                newEphemeralKeyBase64 = envelope.ephemeralPublicKeyBase64,
                newRatchetStep = envelope.ratchetStepIndex,
                editedAt = editedAt
            )
            chatDao.updateRepliesPointingToEditedMessage(
                targetMessageId = messageId,
                newReplyPreview = newPlaintext
            )
            syncConversationLastPreview(conv.conversationId)

            val payload = JSONObject()
                .put("messageId", messageId)
                .put("conversationId", conv.conversationId)
                .put("senderEmail", myEmail)
                .put("cipherTextBase64", envelope.cipherTextBase64)
                .put("ephemeralPublicKeyBase64", envelope.ephemeralPublicKeyBase64)
                .put("ratchetStepIndex", envelope.ratchetStepIndex)
                .put("editedAt", editedAt)

            val actionId = chatDao.enqueuePendingAction(
                TelefamPendingActionEntity(
                    actionType = "EDIT_MESSAGE",
                    conversationId = conv.conversationId,
                    targetMessageId = messageId,
                    payloadJson = payload.toString()
                )
            )
            if (TelefamComKtorBackend.postEditMessageToKtor(payload)) {
                chatDao.deletePendingAction(actionId)
            } else {
                TelefamComPendingActionsWorker.enqueueSync(appContext)
            }
        }
    }

    fun unsendMessageForEveryone(messageId: String) {
        val conv = _uiState.value.activeConversation ?: return
        val myEmail = currentLocalUserEmail()

        viewModelScope.launch {
            val existing = chatDao.getMessageById(messageId)
            if (existing != null && existing.localAudioPath.isNotBlank()) {
                runCatching { File(existing.localAudioPath).delete() }
            }

            // 1. Purge all traces locally in Room (message row, reply references, queued send actions)
            chatDao.deletePendingActionsForMessage(messageId)
            chatDao.clearReplyReferencesToDeletedMessage(messageId)
            chatDao.deleteMessageById(messageId)
            syncConversationLastPreview(conv.conversationId)

            // 2. Purge all traces on the Ktor Backend for both users
            val payload = JSONObject()
                .put("messageId", messageId)
                .put("conversationId", conv.conversationId)
                .put("senderEmail", myEmail)
                .put("purgeAllTraces", true)

            val actionId = chatDao.enqueuePendingAction(
                TelefamPendingActionEntity(
                    actionType = "UNSEND_MESSAGE",
                    conversationId = conv.conversationId,
                    targetMessageId = messageId,
                    payloadJson = payload.toString()
                )
            )
            if (TelefamComKtorBackend.postUnsendMessageToKtor(payload)) {
                chatDao.deletePendingAction(actionId)
            } else {
                TelefamComPendingActionsWorker.enqueueSync(appContext)
            }
        }
    }

    fun deleteMessageForMe(messageId: String) {
        val conv = _uiState.value.activeConversation ?: return
        viewModelScope.launch {
            val existing = chatDao.getMessageById(messageId)
            if (existing != null && existing.localAudioPath.isNotBlank()) {
                runCatching { File(existing.localAudioPath).delete() }
            }
            chatDao.deletePendingActionsForMessage(messageId)
            chatDao.clearReplyReferencesToDeletedMessage(messageId)
            chatDao.deleteMessageById(messageId)
            syncConversationLastPreview(conv.conversationId)
        }
    }

    fun deleteSelectedMessages(messageIds: Set<String>) {
        if (messageIds.isEmpty()) return
        val conv = _uiState.value.activeConversation ?: return
        val myEmail = currentLocalUserEmail()

        viewModelScope.launch {
            val idsList = messageIds.toList()
            for (id in idsList) {
                val msg = chatDao.getMessageById(id)
                if (msg != null && msg.localAudioPath.isNotBlank()) {
                    runCatching { File(msg.localAudioPath).delete() }
                }
                chatDao.deletePendingActionsForMessage(id)
                chatDao.clearReplyReferencesToDeletedMessage(id)

                // If user is the owner of the message, also purge it on backend
                if (msg != null && msg.senderEmail == myEmail) {
                    val payload = JSONObject()
                        .put("messageId", id)
                        .put("conversationId", conv.conversationId)
                        .put("senderEmail", myEmail)
                        .put("purgeAllTraces", true)
                    val actionId = chatDao.enqueuePendingAction(
                        TelefamPendingActionEntity(
                            actionType = "UNSEND_MESSAGE",
                            conversationId = conv.conversationId,
                            targetMessageId = id,
                            payloadJson = payload.toString()
                        )
                    )
                    if (TelefamComKtorBackend.postUnsendMessageToKtor(payload)) {
                        chatDao.deletePendingAction(actionId)
                    } else {
                        TelefamComPendingActionsWorker.enqueueSync(appContext)
                    }
                }
            }
            chatDao.deleteMessagesByIds(idsList)
            syncConversationLastPreview(conv.conversationId)
        }
    }

    private suspend fun syncConversationLastPreview(conversationId: String) {
        val conv = chatDao.getConversationById(conversationId) ?: return
        val latest = chatDao.getLatestMessageForConversation(conversationId)
        if (latest != null) {
            chatDao.upsertConversation(
                conv.copy(
                    lastMessagePreview = latest.plaintextContent,
                    lastMessageType = latest.messageType,
                    lastMessageStatus = latest.status,
                    lastMessageSenderEmail = latest.senderEmail,
                    lastMessageTimestamp = latest.createdAt
                )
            )
        } else {
            chatDao.upsertConversation(
                conv.copy(
                    lastMessagePreview = "",
                    unreadCount = 0
                )
            )
        }
    }

    private suspend fun processImmediateMessageDeliveryLifecycle(
        actionId: Long,
        conversationId: String,
        messageId: String,
        payload: JSONObject
    ) {
        delay(240L)
        // Transition SENDING (Clock) -> SENT (1 Tick)
        chatDao.updateMessageStatus(messageId, TelefamMessageStatus.SENT.name)
        chatDao.getConversationById(conversationId)?.let {
            chatDao.upsertConversation(it.copy(lastMessageStatus = TelefamMessageStatus.SENT.name))
        }

        val deliveredToKtor = TelefamComKtorBackend.postEncryptedMessageToKtor(payload)
        if (deliveredToKtor) {
            delay(320L)
            // Transition SENT (1 Tick) -> DELIVERED (Small Red Badge with Checkmark)
            chatDao.updateMessageStatus(messageId, TelefamMessageStatus.DELIVERED.name)
            chatDao.getConversationById(conversationId)?.let {
                chatDao.upsertConversation(it.copy(lastMessageStatus = TelefamMessageStatus.DELIVERED.name))
            }

            delay(480L)
            // Transition DELIVERED -> SEEN (Receiver Small Avatar once seen)
            val privacy = _uiState.value.privacySettings
            if (privacy.readReceipts == com.telefam.company.app.data.TelefamPrivacyAudience.ANYONE) {
                TelefamComKtorBackend.postReceiptToKtor(messageId, TelefamMessageStatus.SEEN.name)
                chatDao.updateMessageStatus(messageId, TelefamMessageStatus.SEEN.name)
                chatDao.getConversationById(conversationId)?.let {
                    chatDao.upsertConversation(it.copy(lastMessageStatus = TelefamMessageStatus.SEEN.name))
                }
            }
            chatDao.deletePendingAction(actionId)
        }
    }

    fun startVoiceNoteRecording(): String {
        return audioRecorderManager.startRecording()
    }

    fun stopVoiceNoteRecording(): Pair<String, Long> {
        return audioRecorderManager.stopRecording()
    }

    fun playVoiceNoteFile(filePath: String, onFinished: () -> Unit) {
        audioRecorderManager.playAudioFile(filePath, onFinished)
    }

    fun stopVoiceNotePlayback() {
        audioRecorderManager.stopPlayback()
    }

    fun consumeViewOnceVoiceNote(messageId: String) {
        viewModelScope.launch {
            val msg = chatDao.getMessageById(messageId)
            if (msg != null && msg.localAudioPath.isNotBlank()) {
                runCatching { File(msg.localAudioPath).delete() }
            }
            chatDao.markViewOnceVoiceNoteConsumed(messageId)
        }
    }

    fun onStickerSearchChanged(newQuery: String) {
        _uiState.update { it.copy(stickerSearchQuery = newQuery) }
        stickerSearchJob?.cancel()
        stickerSearchJob = viewModelScope.launch {
            delay(320L)
            loadStickers(query = newQuery, reset = true)
        }
    }

    fun loadMoreStickers() {
        val state = _uiState.value
        val nextOffset = state.stickerNextOffset ?: return
        if (state.isLoadingStickers || state.isLoadingNextStickers) return
        loadStickers(query = state.stickerSearchQuery, reset = false, offset = nextOffset)
    }

    fun retryLoadStickers() {
        loadStickers(query = _uiState.value.stickerSearchQuery, reset = true)
    }

    private fun loadStickers(query: String, reset: Boolean, offset: Int = 0) {
        viewModelScope.launch {
            if (reset) {
                _uiState.update {
                    it.copy(
                        isLoadingStickers = true,
                        stickerError = null
                    )
                }
            } else {
                _uiState.update { it.copy(isLoadingNextStickers = true) }
            }

            val result = stickerService.fetchStickers(rawQuery = query, offset = offset)
            _uiState.update { prev ->
                val merged = if (reset) {
                    result.items
                } else {
                    (prev.stickers + result.items).distinctBy { it.id }
                }
                prev.copy(
                    stickers = merged,
                    stickerNextOffset = result.nextOffset,
                    isLoadingStickers = false,
                    isLoadingNextStickers = false,
                    stickerError = if (merged.isEmpty()) result.errorMessage else null
                )
            }
        }
    }

    fun toggleBlockActivePeer(block: Boolean) {
        val conv = _uiState.value.activeConversation ?: return
        viewModelScope.launch {
            chatDao.updateBlockedState(conv.conversationId, block)
            val payload = JSONObject()
                .put("conversationId", conv.conversationId)
                .put("reporterEmail", currentLocalUserEmail())
                .put("targetEmail", conv.peerEmail)
                .put("actionKind", if (block) "BLOCK_USER" else "UNBLOCK_USER")
            chatDao.enqueuePendingAction(
                TelefamPendingActionEntity(
                    actionType = "BLOCK_USER",
                    conversationId = conv.conversationId,
                    payloadJson = payload.toString()
                )
            )
            TelefamComPendingActionsWorker.enqueueSync(appContext)
        }
    }

    fun submitPeerReport(
        reasonAnswer1: String,
        severityAnswer2: String,
        actionAnswer3: String,
        additionalNotes: String
    ) {
        val conv = _uiState.value.activeConversation ?: return
        val reporter = currentLocalUserEmail()
        viewModelScope.launch {
            chatDao.insertUserReport(
                TelefamUserReportEntity(
                    conversationId = conv.conversationId,
                    reportedPeerEmail = conv.peerEmail,
                    reporterEmail = reporter,
                    reasonAnswer1 = reasonAnswer1,
                    severityAnswer2 = severityAnswer2,
                    actionTakenAnswer3 = actionAnswer3,
                    additionalNotes = additionalNotes
                )
            )
            if (actionAnswer3.contains("block", ignoreCase = true)) {
                chatDao.updateBlockedState(conv.conversationId, true)
            }
            if (actionAnswer3.contains("clear", ignoreCase = true)) {
                chatDao.deleteAllMessagesInConversation(conv.conversationId)
            }

            val payload = JSONObject()
                .put("conversationId", conv.conversationId)
                .put("reporterEmail", reporter)
                .put("targetEmail", conv.peerEmail)
                .put("actionKind", "REPORT_USER")
                .put("reasonAnswer1", reasonAnswer1)
                .put("severityAnswer2", severityAnswer2)
                .put("actionAnswer3", actionAnswer3)
                .put("additionalNotes", additionalNotes)

            chatDao.enqueuePendingAction(
                TelefamPendingActionEntity(
                    actionType = "REPORT_USER",
                    conversationId = conv.conversationId,
                    payloadJson = payload.toString()
                )
            )
            TelefamComPendingActionsWorker.enqueueSync(appContext)
        }
    }

    fun setDisappearingMessagesTimer(timerMillis: Long) {
        val conv = _uiState.value.activeConversation ?: return
        viewModelScope.launch {
            chatDao.updateDisappearingTimer(conv.conversationId, timerMillis)
            val payload = JSONObject()
                .put("conversationId", conv.conversationId)
                .put("reporterEmail", currentLocalUserEmail())
                .put("targetEmail", conv.peerEmail)
                .put("actionKind", "SET_DISAPPEARING")
                .put("timerMillis", timerMillis)

            chatDao.enqueuePendingAction(
                TelefamPendingActionEntity(
                    actionType = "SET_DISAPPEARING",
                    conversationId = conv.conversationId,
                    payloadJson = payload.toString()
                )
            )
            TelefamComPendingActionsWorker.enqueueSync(appContext)
        }
    }

    fun clearActiveConversationMessages() {
        val conv = _uiState.value.activeConversation ?: return
        viewModelScope.launch {
            chatDao.deleteAllMessagesInConversation(conv.conversationId)
            chatDao.upsertConversation(
                conv.copy(
                    lastMessagePreview = "",
                    unreadCount = 0
                )
            )
            val payload = JSONObject()
                .put("conversationId", conv.conversationId)
                .put("reporterEmail", currentLocalUserEmail())
                .put("targetEmail", conv.peerEmail)
                .put("actionKind", "CLEAR_CHAT")

            chatDao.enqueuePendingAction(
                TelefamPendingActionEntity(
                    actionType = "CLEAR_CHAT",
                    conversationId = conv.conversationId,
                    payloadJson = payload.toString()
                )
            )
            TelefamComPendingActionsWorker.enqueueSync(appContext)
        }
    }

    fun toggleSignalSessionVerified(verified: Boolean) {
        val conv = _uiState.value.activeConversation ?: return
        viewModelScope.launch {
            chatDao.updateSignalSessionVerified(conv.peerEmail, verified)
        }
    }

    // ---------------------------------------------------------
    // 3-Dots Privacy Settings
    // ---------------------------------------------------------

    fun openPrivacySettingsFromThreeDots() {
        _uiState.update { it.copy(currentScreen = TelefamScreenDestination.PRIVACY_SETTINGS) }
    }

    fun closePrivacySettingsBackToHome() {
        _uiState.update { it.copy(currentScreen = TelefamScreenDestination.HOME) }
    }

    fun togglePrivacyAudience(key: PrivacyToggleKey) {
        val current = _uiState.value.privacySettings
        val updated = when (key) {
            PrivacyToggleKey.MESSAGE_REQUESTS ->
                current.copy(messageRequests = current.messageRequests.toggle())
            PrivacyToggleKey.WHO_CAN_CALL_ME ->
                current.copy(whoCanCallMe = current.whoCanCallMe.toggle())
            PrivacyToggleKey.WHO_CAN_SCREENSHOT_CHATS ->
                current.copy(whoCanScreenshotChats = current.whoCanScreenshotChats.toggle())
            PrivacyToggleKey.WHO_CAN_SHARE_CHATS ->
                current.copy(whoCanShareChats = current.whoCanShareChats.toggle())
            PrivacyToggleKey.WHO_CAN_COPY_MESSAGES ->
                current.copy(whoCanCopyMessages = current.whoCanCopyMessages.toggle())
            PrivacyToggleKey.WHO_CAN_DOWNLOAD_MY_MEDIA ->
                current.copy(whoCanDownloadMyMedia = current.whoCanDownloadMyMedia.toggle())
            PrivacyToggleKey.READ_RECEIPTS ->
                current.copy(readReceipts = current.readReceipts.toggle())
            PrivacyToggleKey.TYPING_INDICATORS ->
                current.copy(typingIndicators = current.typingIndicators.toggle())
            PrivacyToggleKey.ONLINE_INDICATORS ->
                current.copy(onlineIndicators = current.onlineIndicators.toggle())
        }
        repository.savePrivacySettings(updated)
        _uiState.update { it.copy(privacySettings = updated) }
    }

    fun selectOfflineChatTheme(themeId: String) {
        val updated = _uiState.value.privacySettings.copy(selectedChatThemeId = themeId)
        repository.savePrivacySettings(updated)
        _uiState.update { it.copy(privacySettings = updated) }
    }

    fun selectMessageBubbleColor(colorId: String) {
        val updated = _uiState.value.privacySettings.copy(selectedBubbleColorId = colorId)
        repository.savePrivacySettings(updated)
        _uiState.update { it.copy(privacySettings = updated) }
    }

    // ---------------------------------------------------------
    // Public Profile
    // ---------------------------------------------------------

    fun navigateToPublicProfile(email: String) {
        _uiState.update { it.copy(currentScreen = TelefamScreenDestination.PUBLIC_PROFILE, publicProfileEmail = email) }
    }

    fun closePublicProfile() {
        _uiState.update { it.copy(currentScreen = TelefamScreenDestination.HOME, publicProfileEmail = null) }
    }

    private fun currentLocalUserEmail(): String {
        val state = _uiState.value
        return state.authenticatedEmail.ifBlank {
            state.emailInput.trim().lowercase(Locale.US).ifBlank { "me@telefam.com" }
        }
    }

    companion object {
        fun formatTimer(seconds: Int): String {
            val mins = seconds / 60
            val secs = seconds % 60
            return String.format(Locale.US, "%02d:%02d", mins, secs)
        }

        fun provideFactory(
            context: Context,
            repository: TelefamComRepository,
            chatDao: TelefamChatDao
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TelefamComViewModel(
                    context.applicationContext,
                    repository,
                    chatDao
                ) as T
            }
        }
    }
}
