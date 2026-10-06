package com.telefam.company.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.telefam.company.app.data.TelefamComCountries
import com.telefam.company.app.data.TelefamComDatabase
import com.telefam.company.app.data.TelefamComRepository
import com.telefam.company.app.security.TelefamComSignalProtocolEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TelefamComRobolectricTest {

    @Test
    fun verifyAppNameAndEmailAutofillAndCountryValidation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Telefam", context.getString(R.string.app_name))

        val username = TelefamComRepository.deriveUsernameFromEmail("alex.morgan99@telefam.com")
        val fullName = TelefamComRepository.deriveFullNameFromEmail("alex.morgan99@telefam.com")
        assertEquals("alex.morgan99", username)
        assertEquals("Alex Morgan", fullName)

        val kenya = TelefamComCountries.defaultCountry
        assertEquals("KE", kenya.isoCode)
        assertEquals("+254", kenya.dialCode)
        assertNull(TelefamComCountries.validatePhoneDigits(kenya, "712345678"))
        assertNotNull(TelefamComCountries.validatePhoneDigits(kenya, "123"))
        assertTrue(TelefamComCountries.allCountries.size > 100)
    }

    @Test
    fun verifySignalProtocolEncryptionAndSafetyNumber() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = TelefamComDatabase.getInstance(context)
        val signalEngine = TelefamComSignalProtocolEngine(context, db.chatDao())

        val bundle = signalEngine.getLocalPublicKeyBundle()
        assertTrue(bundle.identityPublicKeyBase64.isNotBlank())

        val envelope1 = signalEngine.encryptOutgoingMessage(
            localUserEmail = "alice@telefam.com",
            peerEmail = "bob@telefam.com",
            plaintext = "Hello Bob, Signal E2EE test!"
        )
        val envelope2 = signalEngine.encryptOutgoingMessage(
            localUserEmail = "alice@telefam.com",
            peerEmail = "bob@telefam.com",
            plaintext = "Second ratchet message"
        )

        assertTrue(envelope1.cipherTextBase64.isNotBlank())
        assertNotEquals(envelope1.cipherTextBase64, envelope2.cipherTextBase64)
        assertEquals(0, envelope1.ratchetStepIndex)
        assertEquals(1, envelope2.ratchetStepIndex)
        // Safety number has 12 groups of 5 digits separated by spaces (60 digits total)
        val groups = envelope1.safetyNumberNumeric.split(" ")
        assertEquals(12, groups.size)
        assertTrue(groups.all { it.length == 5 && it.all(Char::isDigit) })
    }

    @Test
    fun verifyMessageReplyReactPinFavouriteEditAndUnsendClearTraces() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = TelefamComDatabase.getInstance(context)
        val chatDao = db.chatDao()

        val msg1 = com.telefam.company.app.data.TelefamMessageEntity(
            messageId = "msg_test_1",
            conversationId = "conv_test",
            senderEmail = "alice@telefam.com",
            receiverEmail = "bob@telefam.com",
            messageType = "TEXT",
            plaintextContent = "Original message",
            cipherTextBase64 = "cipher1",
            ephemeralPublicKeyBase64 = "eph1",
            ratchetStepIndex = 0,
            status = "SENT"
        )
        val msg2Reply = com.telefam.company.app.data.TelefamMessageEntity(
            messageId = "msg_test_2",
            conversationId = "conv_test",
            senderEmail = "bob@telefam.com",
            receiverEmail = "alice@telefam.com",
            messageType = "TEXT",
            plaintextContent = "Reply message",
            cipherTextBase64 = "cipher2",
            ephemeralPublicKeyBase64 = "eph2",
            ratchetStepIndex = 0,
            status = "SEEN",
            replyToMessageId = "msg_test_1",
            replyToSenderName = "Alice",
            replyToPreviewText = "Original message",
            replyToMessageType = "TEXT"
        )
        chatDao.insertMessage(msg1)
        chatDao.insertMessage(msg2Reply)

        // React, Pin, Favourite
        chatDao.updateMessageReaction("msg_test_1", "🔥")
        chatDao.updateMessagePinned("msg_test_1", true)
        chatDao.updateMessageFavourite("msg_test_1", true)

        val afterMeta = chatDao.getMessageById("msg_test_1")!!
        assertEquals("🔥", afterMeta.reactionEmoji)
        assertTrue(afterMeta.isPinned)
        assertTrue(afterMeta.isFavourite)

        // Edit message and verify reply preview updates
        chatDao.updateEditedMessage("msg_test_1", "Updated message", "cipher1_edit", "eph1_edit", 1, 123456L)
        chatDao.updateRepliesPointingToEditedMessage("msg_test_1", "Updated message")
        val editedMsg1 = chatDao.getMessageById("msg_test_1")!!
        val updatedReply2 = chatDao.getMessageById("msg_test_2")!!
        assertEquals("Updated message", editedMsg1.plaintextContent)
        assertTrue(editedMsg1.isEdited)
        assertEquals("Updated message", updatedReply2.replyToPreviewText)

        // Unsend message 1 and verify all traces & reply references are cleared
        chatDao.clearReplyReferencesToDeletedMessage("msg_test_1")
        chatDao.deleteMessageById("msg_test_1")
        assertNull(chatDao.getMessageById("msg_test_1"))
        val replyAfterUnsend = chatDao.getMessageById("msg_test_2")!!
        assertNull(replyAfterUnsend.replyToMessageId)
        assertNull(replyAfterUnsend.replyToPreviewText)
    }
}
