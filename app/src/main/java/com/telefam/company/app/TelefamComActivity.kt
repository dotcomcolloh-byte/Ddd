package com.telefam.company.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.telefam.company.app.data.TelefamComDatabase
import com.telefam.company.app.data.TelefamComRepository
import com.telefam.company.app.ui.TelefamComViewModel
import com.telefam.company.app.ui.TelefamScreenDestination
import com.telefam.company.app.ui.screens.TelefamComAuthScreen
import com.telefam.company.app.ui.screens.TelefamComHomeScreen
import com.telefam.company.app.ui.screens.TelefamComIndividualChatScreen
import com.telefam.company.app.ui.screens.TelefamComPrivacyScreen
import com.telefam.company.app.ui.screens.TelefamComProfileSetupScreen
import com.telefam.company.app.ui.screens.TelefamComPublicProfileScreen
import com.telefam.company.app.ui.screens.TelefamComWelcomeScreen
import com.telefam.company.app.ui.theme.TelefamComTheme

class TelefamComActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = TelefamComDatabase.getInstance(applicationContext)
        val repository = TelefamComRepository(
            context = applicationContext,
            profileDao = database.profileDao()
        )
        val factory = TelefamComViewModel.provideFactory(
            context = applicationContext,
            repository = repository,
            chatDao = database.chatDao()
        )

        setContent {
            val viewModel: TelefamComViewModel = viewModel(factory = factory)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            TelefamComTheme(themeMode = uiState.themeMode) {
                TelefamRootAppContent(
                    activity = this@TelefamComActivity,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun TelefamRootAppContent(
    activity: ComponentActivity,
    viewModel: TelefamComViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = uiState.currentScreen,
        transitionSpec = {
            fadeIn(tween(240)) togetherWith fadeOut(tween(180))
        },
        modifier = modifier.fillMaxSize(),
        label = "telefam_root_navigation"
    ) { destination ->
        when (destination) {
            TelefamScreenDestination.WELCOME -> {
                TelefamComWelcomeScreen(
                    onGetStarted = { viewModel.onWelcomeFinished() }
                )
            }

            TelefamScreenDestination.AUTH_EMAIL,
            TelefamScreenDestination.AUTH_OTP -> {
                if (destination == TelefamScreenDestination.AUTH_OTP) {
                    BackHandler { viewModel.onChangeEmailClicked() }
                }
                TelefamComAuthScreen(
                    uiState = uiState,
                    onEmailChanged = viewModel::onEmailChanged,
                    onSendOtpClicked = viewModel::sendOtpCode,
                    onGoogleSignInClicked = { viewModel.initiateGoogleSignIn(activity) },
                    onOtpDigitsChanged = viewModel::onOtpDigitsChanged,
                    onVerifyOtpClicked = viewModel::verifyOtpCode,
                    onResendOtpClicked = viewModel::resendOtpCode,
                    onChangeEmailClicked = viewModel::onChangeEmailClicked,
                    onOpenEmailClientWithOtp = viewModel::openEmailAppWithOtp
                )
            }

            TelefamScreenDestination.PROFILE_SETUP -> {
                TelefamComProfileSetupScreen(
                    uiState = uiState,
                    onFullNameChanged = viewModel::onFullNameChanged,
                    onUsernameChanged = viewModel::onUsernameChanged,
                    onCountrySelected = viewModel::onCountrySelected,
                    onPhoneNumberChanged = viewModel::onPhoneNumberChanged,
                    onLocationChanged = viewModel::onLocationChanged,
                    onGenderSelected = viewModel::onGenderSelected,
                    onDateOfBirthSelected = viewModel::onDateOfBirthSelected,
                    onBioChanged = viewModel::onBioChanged,
                    onPhotoCropped = viewModel::saveCroppedProfileBitmap,
                    onContinueClicked = viewModel::submitProfileSetup,
                    onBackClicked = viewModel::onBackFromProfileSetup
                )
            }

            TelefamScreenDestination.HOME -> {
                TelefamComHomeScreen(
                    uiState = uiState,
                    conversations = uiState.conversations,
                    viewModel = viewModel,
                    onTabSelected = viewModel::onHomeTabSelected,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onOpenConversation = viewModel::openConversationById,
                    onStartNewChat = viewModel::startOrOpenEncryptedChat,
                    onThreeDotsClicked = viewModel::openPrivacySettingsFromThreeDots
                )
            }

            TelefamScreenDestination.PRIVACY_SETTINGS -> {
                TelefamComPrivacyScreen(
                    privacyState = uiState.privacySettings,
                    onToggleAudience = viewModel::togglePrivacyAudience,
                    onSelectChatTheme = viewModel::selectOfflineChatTheme,
                    onSelectBubbleColor = viewModel::selectMessageBubbleColor,
                    onBackClicked = viewModel::closePrivacySettingsBackToHome
                )
            }

            TelefamScreenDestination.INDIVIDUAL_CHAT -> {
                val activeConv = uiState.activeConversation
                if (activeConv != null) {
                    TelefamComIndividualChatScreen(
                        activity = activity,
                        conversation = activeConv,
                        messages = uiState.activeMessages,
                        privacySettings = uiState.privacySettings,
                        signalSession = uiState.activeSignalSession,
                        stickers = uiState.stickers,
                        stickerSearchQuery = uiState.stickerSearchQuery,
                        isLoadingStickers = uiState.isLoadingStickers,
                        isLoadingNextStickers = uiState.isLoadingNextStickers,
                        stickerError = uiState.stickerError,
                        onStickerSearchChanged = viewModel::onStickerSearchChanged,
                        onLoadMoreStickers = viewModel::loadMoreStickers,
                        onRetryStickers = viewModel::retryLoadStickers,
                        onSendTextMessage = viewModel::sendTextMessage,
                        onSendStickerMessage = viewModel::sendStickerMessage,
                        onStartVoiceRecording = viewModel::startVoiceNoteRecording,
                        onStopVoiceRecording = viewModel::stopVoiceNoteRecording,
                        onPlayAudioFile = viewModel::playVoiceNoteFile,
                        onStopAudioPlayback = viewModel::stopVoiceNotePlayback,
                        onSendVoiceNoteMessage = viewModel::sendVoiceNoteMessage,
                        onConsumeViewOnceVoiceNote = viewModel::consumeViewOnceVoiceNote,
                        onReactToMessage = viewModel::reactToMessage,
                        onTogglePinMessage = viewModel::togglePinMessage,
                        onToggleFavouriteMessage = viewModel::toggleFavouriteMessage,
                        onEditOwnMessage = viewModel::editOwnMessage,
                        onUnsendMessageForEveryone = viewModel::unsendMessageForEveryone,
                        onDeleteMessageForMe = viewModel::deleteMessageForMe,
                        onDeleteSelectedMessages = viewModel::deleteSelectedMessages,
                        onToggleBlockPeer = viewModel::toggleBlockActivePeer,
                        onSubmitReportPeer = viewModel::submitPeerReport,
                        onSetDisappearingTimer = viewModel::setDisappearingMessagesTimer,
                        onClearChatMessages = viewModel::clearActiveConversationMessages,
                        onToggleSignalVerified = viewModel::toggleSignalSessionVerified,
                        onBackClicked = viewModel::closeIndividualChatBackToHome
                    )
                } else {
                    viewModel.closeIndividualChatBackToHome()
                }
            }

            TelefamScreenDestination.PUBLIC_PROFILE -> {
                val email = uiState.publicProfileEmail
                if (email != null) {
                    TelefamComPublicProfileScreen(
                        userEmail = email,
                        viewModel = viewModel,
                        onBackClicked = viewModel::closePublicProfile
                    )
                } else {
                    viewModel.closePublicProfile()
                }
            }
        }
    }
}
