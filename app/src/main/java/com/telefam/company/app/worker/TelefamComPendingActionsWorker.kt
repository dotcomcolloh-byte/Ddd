package com.telefam.company.app.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.telefam.company.app.backend.TelefamComKtorBackend
import com.telefam.company.app.data.TelefamComDatabase
import com.telefam.company.app.data.TelefamMessageStatus
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TelefamComPendingActionsWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        TelefamComKtorBackend.ensureStarted(applicationContext)
        val database = TelefamComDatabase.getInstance(applicationContext)
        val chatDao = database.chatDao()

        // Purge any expired disappearing messages first
        chatDao.purgeExpiredDisappearingMessages(System.currentTimeMillis())

        val pendingActions = chatDao.getAllPendingActions()
        if (pendingActions.isEmpty()) {
            return Result.success()
        }

        var anyFailed = false

        for (action in pendingActions) {
            val payload = runCatching { JSONObject(action.payloadJson) }.getOrNull()
            if (payload == null) {
                chatDao.deletePendingAction(action.actionId)
                continue
            }

            val succeeded = when (action.actionType) {
                "SEND_MESSAGE" -> {
                    val messageId = action.targetMessageId
                    chatDao.updateMessageStatus(messageId, TelefamMessageStatus.SENT.name)
                    updateConversationLastStatus(chatDao, action.conversationId, TelefamMessageStatus.SENT.name)

                    val delivered = TelefamComKtorBackend.postEncryptedMessageToKtor(payload)
                    if (delivered) {
                        delay(260)
                        chatDao.updateMessageStatus(messageId, TelefamMessageStatus.DELIVERED.name)
                        updateConversationLastStatus(chatDao, action.conversationId, TelefamMessageStatus.DELIVERED.name)

                        delay(380)
                        TelefamComKtorBackend.postReceiptToKtor(
                            messageId = messageId,
                            status = TelefamMessageStatus.SEEN.name,
                            conversationId = action.conversationId
                        )
                        chatDao.updateMessageStatus(messageId, TelefamMessageStatus.SEEN.name)
                        updateConversationLastStatus(chatDao, action.conversationId, TelefamMessageStatus.SEEN.name)
                        true
                    } else {
                        false
                    }
                }

                "EDIT_MESSAGE" -> {
                    TelefamComKtorBackend.postEditMessageToKtor(payload)
                }

                "UNSEND_MESSAGE" -> {
                    TelefamComKtorBackend.postUnsendMessageToKtor(payload)
                }

                "REACT_MESSAGE" -> {
                    TelefamComKtorBackend.postReactionToKtor(payload)
                }

                "PIN_MESSAGE" -> {
                    TelefamComKtorBackend.postPinToKtor(payload)
                }

                "FAV_MESSAGE" -> {
                    TelefamComKtorBackend.postFavouriteToKtor(payload)
                }

                "FOLLOW_USER",
                "UNFOLLOW_USER" -> {
                    val followerEmail = payload.optString("followerEmail")
                    val targetEmail = payload.optString("targetEmail")
                    val follow = payload.optBoolean("follow", action.actionType == "FOLLOW_USER")
                    val res = TelefamComKtorBackend.postFollowActionToKtor(
                        followerEmail = followerEmail,
                        targetEmail = targetEmail,
                        follow = follow
                    )
                    if (res.success) {
                        chatDao.updateFollowRelationship(
                            targetEmail = targetEmail,
                            isFollowing = res.isFollowing,
                            isFollowedBy = res.isFollowedBy,
                            followersCount = res.targetFollowersCount,
                            followingCount = res.targetFollowingCount
                        )
                    }
                    res.success || res.isRateLimited
                }

                "SYNC_CONTACT_HASHES" -> {
                    val ownerEmail = payload.optString("ownerEmail")
                    val hashesArr = payload.optJSONArray("hashes")
                    val hashSet = mutableSetOf<String>()
                    if (hashesArr != null) {
                        for (i in 0 until hashesArr.length()) {
                            hashSet.add(hashesArr.optString(i))
                        }
                    }
                    val merged = TelefamComKtorBackend.uploadContactHashesToKtor(ownerEmail, hashSet)
                    if (merged != null) {
                        chatDao.upsertDirectoryUsers(merged)
                        true
                    } else {
                        false
                    }
                }

                "BLOCK_USER",
                "REPORT_USER",
                "SET_DISAPPEARING",
                "CLEAR_CHAT" -> {
                    TelefamComKtorBackend.postModerationToKtor(payload)
                }

                else -> true
            }

            if (succeeded) {
                chatDao.deletePendingAction(action.actionId)
            } else {
                chatDao.incrementActionRetryCount(action.actionId)
                anyFailed = true
            }
        }

        return if (anyFailed) Result.retry() else Result.success()
    }

    private suspend fun updateConversationLastStatus(
        chatDao: com.telefam.company.app.data.TelefamChatDao,
        conversationId: String,
        status: String
    ) {
        val conv = chatDao.getConversationById(conversationId) ?: return
        chatDao.upsertConversation(conv.copy(lastMessageStatus = status))
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "telefam_pending_actions_sync_worker"

        fun enqueueSync(context: Context) {
            runCatching {
                val request = OneTimeWorkRequestBuilder<TelefamComPendingActionsWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                            .build()
                    )
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                    .build()

                WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    request
                )
            }
        }
    }
}
