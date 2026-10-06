package com.telefam.company.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "telefam_user_profiles")
data class TelefamUserProfileEntity(
    @PrimaryKey val email: String,
    val uid: String,
    val fullName: String,
    val username: String,
    val countryIso: String,
    val countryDialCode: String,
    val phoneNumber: String,
    val location: String,
    val gender: String,
    val dateOfBirth: String,
    val dateOfBirthMillis: Long,
    val bio: String,
    val photoPath: String,
    val isProfileCompleted: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)

enum class TelefamMessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    SEEN
}

enum class TelefamMessageType {
    TEXT,
    STICKER,
    VOICE_NOTE
}

@Entity(tableName = "telefam_conversations")
data class TelefamConversationEntity(
    @PrimaryKey val conversationId: String,
    val peerEmail: String,
    val peerFullName: String,
    val peerUsername: String,
    val peerAvatarUrl: String,
    val lastMessagePreview: String,
    val lastMessageType: String, // TEXT, STICKER, VOICE_NOTE
    val lastMessageStatus: String, // SENDING, SENT, DELIVERED, SEEN
    val lastMessageSenderEmail: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int = 0,
    val isPeerOnline: Boolean = false,
    val isPeerTyping: Boolean = false,
    val isBlocked: Boolean = false,
    val disappearingTimerMillis: Long = 0L, // 0 = OFF, 86400000 = 24h, 604800000 = 7d, 2592000000 = 30d
    val peerIdentityPublicKeyBase64: String = "",
    val peerSignedPreKeyBase64: String = "",
    val peerRegistrationId: Int = 0
)

@Entity(tableName = "telefam_messages")
data class TelefamMessageEntity(
    @PrimaryKey val messageId: String,
    val conversationId: String,
    val senderEmail: String,
    val receiverEmail: String,
    val messageType: String, // TEXT, STICKER, VOICE_NOTE
    val plaintextContent: String,
    val cipherTextBase64: String,
    val ephemeralPublicKeyBase64: String,
    val ratchetStepIndex: Int,
    val mediaUrl: String = "",
    val localAudioPath: String = "",
    val audioDurationMs: Long = 0L,
    val isViewOnce: Boolean = false,
    val isViewOnceConsumed: Boolean = false,
    val status: String, // SENDING, SENT, DELIVERED, SEEN
    val replyToMessageId: String? = null,
    val replyToSenderName: String? = null,
    val replyToPreviewText: String? = null,
    val replyToMessageType: String? = null,
    val reactionEmoji: String? = null,
    val isPinned: Boolean = false,
    val isFavourite: Boolean = false,
    val isEdited: Boolean = false,
    val editedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null
)

@Entity(tableName = "telefam_pending_actions")
data class TelefamPendingActionEntity(
    @PrimaryKey(autoGenerate = true) val actionId: Long = 0,
    val actionType: String, // SEND_MESSAGE, EDIT_MESSAGE, UNSEND_MESSAGE, REACT_MESSAGE, PIN_MESSAGE, FAV_MESSAGE, DELETE_LOCAL, BLOCK_USER, REPORT_USER, SET_DISAPPEARING, CLEAR_CHAT
    val conversationId: String,
    val targetMessageId: String = "",
    val payloadJson: String,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "telefam_signal_sessions")
data class TelefamSignalSessionEntity(
    @PrimaryKey val peerEmail: String,
    val localIdentityPublicKeyBase64: String,
    val localIdentityPrivateKeyBase64: String,
    val peerIdentityPublicKeyBase64: String,
    val rootKeyBase64: String,
    val sendingChainKeyBase64: String,
    val receivingChainKeyBase64: String,
    val sendRatchetCounter: Int = 0,
    val receiveRatchetCounter: Int = 0,
    val safetyNumberNumeric: String,
    val isVerified: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "telefam_cached_stickers")
data class TelefamCachedStickerEntity(
    @PrimaryKey val stickerId: String,
    val queryKey: String,
    val title: String,
    val previewUrl: String,
    val fullUrl: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "telefam_user_reports")
data class TelefamUserReportEntity(
    @PrimaryKey(autoGenerate = true) val reportId: Long = 0,
    val conversationId: String,
    val reportedPeerEmail: String,
    val reporterEmail: String,
    val reasonAnswer1: String,
    val severityAnswer2: String,
    val actionTakenAnswer3: String,
    val additionalNotes: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "telefam_directory_users")
data class TelefamDirectoryUserEntity(
    @PrimaryKey val email: String,
    val uid: String,
    val fullName: String,
    val username: String,
    val countryIso: String = "",
    val countryName: String = "",
    val countryDialCode: String = "",
    val phoneHashSha256: String = "",
    val emailHashSha256: String = "",
    val location: String = "",
    val gender: String = "",
    val bio: String = "",
    val photoPath: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val likesCount: Int = 0,
    val mutualConnectionsCount: Int = 0,
    val isFollowing: Boolean = false,
    val isFollowedBy: Boolean = false,
    val isMatchedFromContacts: Boolean = false,
    val isBlocked: Boolean = false,
    val suggestionDayKey: String = "",
    val suggestionScore: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isFriend: Boolean
        get() = isFollowing && isFollowedBy
}

@Dao
interface TelefamProfileDao {
    @Query("SELECT * FROM telefam_user_profiles WHERE email = :email LIMIT 1")
    fun observeProfileByEmail(email: String): Flow<TelefamUserProfileEntity?>

    @Query("SELECT * FROM telefam_user_profiles WHERE email = :email LIMIT 1")
    suspend fun getProfileByEmail(email: String): TelefamUserProfileEntity?

    @Query("SELECT * FROM telefam_user_profiles ORDER BY updatedAt DESC")
    suspend fun getAllSavedProfiles(): List<TelefamUserProfileEntity>

    @Query("SELECT * FROM telefam_user_profiles ORDER BY updatedAt DESC LIMIT 1")
    fun observeActiveProfile(): Flow<TelefamUserProfileEntity?>

    @Query("SELECT * FROM telefam_user_profiles ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestProfile(): TelefamUserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: TelefamUserProfileEntity)

    @Query("DELETE FROM telefam_user_profiles WHERE email = :email")
    suspend fun deleteProfile(email: String)

    @Query("DELETE FROM telefam_user_profiles")
    suspend fun clearAllProfiles()
}

@Dao
interface TelefamChatDao {
    @Query("SELECT * FROM telefam_conversations ORDER BY lastMessageTimestamp DESC")
    fun observeAllConversations(): Flow<List<TelefamConversationEntity>>

    @Query("SELECT * FROM telefam_conversations WHERE conversationId = :conversationId LIMIT 1")
    fun observeConversationById(conversationId: String): Flow<TelefamConversationEntity?>

    @Query("SELECT * FROM telefam_conversations WHERE conversationId = :conversationId LIMIT 1")
    suspend fun getConversationById(conversationId: String): TelefamConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conversation: TelefamConversationEntity)

    @Query("UPDATE telefam_conversations SET unreadCount = 0 WHERE conversationId = :conversationId")
    suspend fun clearUnreadCount(conversationId: String)

    @Query("UPDATE telefam_conversations SET isPeerTyping = :isTyping, isPeerOnline = :isOnline WHERE conversationId = :conversationId")
    suspend fun updatePeerPresence(conversationId: String, isOnline: Boolean, isTyping: Boolean)

    @Query("UPDATE telefam_conversations SET isBlocked = :isBlocked WHERE conversationId = :conversationId")
    suspend fun updateBlockedState(conversationId: String, isBlocked: Boolean)

    @Query("UPDATE telefam_conversations SET disappearingTimerMillis = :timerMillis WHERE conversationId = :conversationId")
    suspend fun updateDisappearingTimer(conversationId: String, timerMillis: Long)

    @Query(
        """
        SELECT * FROM telefam_messages 
        WHERE conversationId = :conversationId 
          AND (expiresAt IS NULL OR expiresAt > :nowMillis)
        ORDER BY createdAt ASC
        """
    )
    fun observeMessagesForConversation(conversationId: String, nowMillis: Long): Flow<List<TelefamMessageEntity>>

    @Query("SELECT * FROM telefam_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getMessagesForConversation(conversationId: String): List<TelefamMessageEntity>

    @Query("SELECT * FROM telefam_messages WHERE conversationId = :conversationId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestMessageForConversation(conversationId: String): TelefamMessageEntity?

    @Query("SELECT * FROM telefam_messages WHERE messageId = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): TelefamMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: TelefamMessageEntity)

    @Query("UPDATE telefam_messages SET status = :newStatus WHERE messageId = :messageId")
    suspend fun updateMessageStatus(messageId: String, newStatus: String)

    @Query("UPDATE telefam_messages SET status = 'SEEN' WHERE conversationId = :conversationId AND status != 'SEEN'")
    suspend fun markAllConversationMessagesSeen(conversationId: String)

    @Query("UPDATE telefam_messages SET isViewOnceConsumed = 1, localAudioPath = '' WHERE messageId = :messageId")
    suspend fun markViewOnceVoiceNoteConsumed(messageId: String)

    @Query("UPDATE telefam_messages SET reactionEmoji = :emoji WHERE messageId = :messageId")
    suspend fun updateMessageReaction(messageId: String, emoji: String?)

    @Query("UPDATE telefam_messages SET isPinned = :pinned WHERE messageId = :messageId")
    suspend fun updateMessagePinned(messageId: String, pinned: Boolean)

    @Query("UPDATE telefam_messages SET isFavourite = :favourite WHERE messageId = :messageId")
    suspend fun updateMessageFavourite(messageId: String, favourite: Boolean)

    @Query(
        """
        UPDATE telefam_messages 
        SET plaintextContent = :newPlaintext, 
            cipherTextBase64 = :newCipherTextBase64, 
            ephemeralPublicKeyBase64 = :newEphemeralKeyBase64,
            ratchetStepIndex = :newRatchetStep,
            isEdited = 1, 
            editedAt = :editedAt 
        WHERE messageId = :messageId
        """
    )
    suspend fun updateEditedMessage(
        messageId: String,
        newPlaintext: String,
        newCipherTextBase64: String,
        newEphemeralKeyBase64: String,
        newRatchetStep: Int,
        editedAt: Long
    )

    @Query(
        """
        UPDATE telefam_messages
        SET replyToPreviewText = :newReplyPreview
        WHERE replyToMessageId = :targetMessageId
        """
    )
    suspend fun updateRepliesPointingToEditedMessage(targetMessageId: String, newReplyPreview: String)

    @Query(
        """
        UPDATE telefam_messages
        SET replyToMessageId = NULL,
            replyToSenderName = NULL,
            replyToPreviewText = NULL,
            replyToMessageType = NULL
        WHERE replyToMessageId = :deletedMessageId
        """
    )
    suspend fun clearReplyReferencesToDeletedMessage(deletedMessageId: String)

    @Query("DELETE FROM telefam_messages WHERE messageId = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM telefam_messages WHERE messageId IN (:messageIds)")
    suspend fun deleteMessagesByIds(messageIds: List<String>)

    @Query("DELETE FROM telefam_messages WHERE conversationId = :conversationId")
    suspend fun deleteAllMessagesInConversation(conversationId: String)

    @Query("DELETE FROM telefam_messages WHERE expiresAt IS NOT NULL AND expiresAt <= :nowMillis")
    suspend fun purgeExpiredDisappearingMessages(nowMillis: Long)

    // Pending actions queue for offline-first Worker retry
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueuePendingAction(action: TelefamPendingActionEntity): Long

    @Query("SELECT * FROM telefam_pending_actions ORDER BY createdAt ASC")
    suspend fun getAllPendingActions(): List<TelefamPendingActionEntity>

    @Query("DELETE FROM telefam_pending_actions WHERE actionId = :actionId")
    suspend fun deletePendingAction(actionId: Long)

    @Query("DELETE FROM telefam_pending_actions WHERE targetMessageId = :targetMessageId")
    suspend fun deletePendingActionsForMessage(targetMessageId: String)

    @Query("UPDATE telefam_pending_actions SET retryCount = retryCount + 1 WHERE actionId = :actionId")
    suspend fun incrementActionRetryCount(actionId: Long)

    // Signal E2EE Session Persistence
    @Query("SELECT * FROM telefam_signal_sessions WHERE peerEmail = :peerEmail LIMIT 1")
    fun observeSignalSession(peerEmail: String): Flow<TelefamSignalSessionEntity?>

    @Query("SELECT * FROM telefam_signal_sessions WHERE peerEmail = :peerEmail LIMIT 1")
    suspend fun getSignalSession(peerEmail: String): TelefamSignalSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSignalSession(session: TelefamSignalSessionEntity)

    @Query("UPDATE telefam_signal_sessions SET isVerified = :verified WHERE peerEmail = :peerEmail")
    suspend fun updateSignalSessionVerified(peerEmail: String, verified: Boolean)

    // Stickers Offline Cache
    @Query("SELECT * FROM telefam_cached_stickers WHERE queryKey = :queryKey ORDER BY cachedAt ASC")
    suspend fun getCachedStickersByQuery(queryKey: String): List<TelefamCachedStickerEntity>

    @Query("SELECT * FROM telefam_cached_stickers ORDER BY cachedAt DESC LIMIT 48")
    suspend fun getAllCachedStickers(): List<TelefamCachedStickerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCachedStickers(stickers: List<TelefamCachedStickerEntity>)

    // Reports
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserReport(report: TelefamUserReportEntity): Long

    // Contacts & Registered Users Directory Cache
    @Query("SELECT * FROM telefam_directory_users WHERE email != :excludeEmail ORDER BY suggestionScore DESC, mutualConnectionsCount DESC, fullName ASC")
    fun observeAllDirectoryUsers(excludeEmail: String): Flow<List<TelefamDirectoryUserEntity>>

    @Query("SELECT * FROM telefam_directory_users WHERE email = :email LIMIT 1")
    fun observeDirectoryUserByEmail(email: String): Flow<TelefamDirectoryUserEntity?>

    @Query("SELECT * FROM telefam_directory_users WHERE email = :email LIMIT 1")
    suspend fun getDirectoryUserByEmail(email: String): TelefamDirectoryUserEntity?

    @Query("SELECT * FROM telefam_directory_users WHERE email != :excludeEmail ORDER BY suggestionScore DESC, fullName ASC")
    suspend fun getAllDirectoryUsers(excludeEmail: String): List<TelefamDirectoryUserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDirectoryUser(user: TelefamDirectoryUserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDirectoryUsers(users: List<TelefamDirectoryUserEntity>)

    @Query(
        """
        UPDATE telefam_directory_users
        SET isFollowing = :isFollowing,
            isFollowedBy = :isFollowedBy,
            followersCount = :followersCount,
            followingCount = :followingCount,
            updatedAt = :updatedAt
        WHERE email = :targetEmail
        """
    )
    suspend fun updateFollowRelationship(
        targetEmail: String,
        isFollowing: Boolean,
        isFollowedBy: Boolean,
        followersCount: Int,
        followingCount: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE telefam_directory_users SET isBlocked = :isBlocked WHERE email = :targetEmail")
    suspend fun updateDirectoryUserBlocked(targetEmail: String, isBlocked: Boolean)
}

@Database(
    entities = [
        TelefamUserProfileEntity::class,
        TelefamConversationEntity::class,
        TelefamMessageEntity::class,
        TelefamPendingActionEntity::class,
        TelefamSignalSessionEntity::class,
        TelefamCachedStickerEntity::class,
        TelefamUserReportEntity::class,
        TelefamDirectoryUserEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class TelefamComDatabase : RoomDatabase() {
    abstract fun profileDao(): TelefamProfileDao
    abstract fun chatDao(): TelefamChatDao

    companion object {
        @Volatile
        private var INSTANCE: TelefamComDatabase? = null

        fun getInstance(context: Context): TelefamComDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TelefamComDatabase::class.java,
                    "telefam_com_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
