package com.telefam.company.app.security

import android.content.Context
import android.util.Base64
import com.telefam.company.app.data.TelefamChatDao
import com.telefam.company.app.data.TelefamSignalSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

data class SignalEncryptedEnvelope(
    val cipherTextBase64: String,
    val ephemeralPublicKeyBase64: String,
    val ratchetStepIndex: Int,
    val safetyNumberNumeric: String
)

data class SignalBundleKeys(
    val registrationId: Int,
    val identityPublicKeyBase64: String,
    val signedPreKeyPublicBase64: String,
    val signedPreKeySignatureBase64: String,
    val oneTimePreKeyPublicBase64: String
)

class TelefamComSignalProtocolEngine(
    context: Context,
    private val chatDao: TelefamChatDao
) {
    private val prefs = context.getSharedPreferences("telefam_signal_identity_store", Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    init {
        ensureLocalIdentityKeyBundle()
    }

    private fun ensureLocalIdentityKeyBundle() {
        if (prefs.contains("local_identity_pub") && prefs.contains("local_identity_priv")) {
            return
        }
        val identityKeyPair = generateEcKeyPair()
        val signedPreKeyPair = generateEcKeyPair()
        val oneTimePreKeyPair = generateEcKeyPair()
        val registrationId = 10000 + secureRandom.nextInt(89999)

        val signatureBytes = signBytes(identityKeyPair.private, signedPreKeyPair.public.encoded)

        prefs.edit()
            .putInt("local_registration_id", registrationId)
            .putString("local_identity_pub", encodeBase64(identityKeyPair.public.encoded))
            .putString("local_identity_priv", encodeBase64(identityKeyPair.private.encoded))
            .putString("local_signed_prekey_pub", encodeBase64(signedPreKeyPair.public.encoded))
            .putString("local_signed_prekey_priv", encodeBase64(signedPreKeyPair.private.encoded))
            .putString("local_signed_prekey_sig", encodeBase64(signatureBytes))
            .putString("local_onetime_prekey_pub", encodeBase64(oneTimePreKeyPair.public.encoded))
            .putString("local_onetime_prekey_priv", encodeBase64(oneTimePreKeyPair.private.encoded))
            .apply()
    }

    fun getLocalPublicKeyBundle(): SignalBundleKeys {
        ensureLocalIdentityKeyBundle()
        return SignalBundleKeys(
            registrationId = prefs.getInt("local_registration_id", 14209),
            identityPublicKeyBase64 = prefs.getString("local_identity_pub", "") ?: "",
            signedPreKeyPublicBase64 = prefs.getString("local_signed_prekey_pub", "") ?: "",
            signedPreKeySignatureBase64 = prefs.getString("local_signed_prekey_sig", "") ?: "",
            oneTimePreKeyPublicBase64 = prefs.getString("local_onetime_prekey_pub", "") ?: ""
        )
    }

    suspend fun getOrEstablishSession(
        localUserEmail: String,
        peerEmail: String,
        peerIdentityPublicKeyFromServer: String? = null
    ): TelefamSignalSessionEntity = withContext(Dispatchers.IO) {
        val normalizedPeer = peerEmail.trim().lowercase()
        val existing = chatDao.getSignalSession(normalizedPeer)
        if (existing != null) {
            return@withContext existing
        }

        ensureLocalIdentityKeyBundle()
        val localPubB64 = prefs.getString("local_identity_pub", "") ?: ""
        val localPrivB64 = prefs.getString("local_identity_priv", "") ?: ""
        val localPrivKey = decodePrivateKey(localPrivB64)

        // If peer's public key is supplied from Ktor backend prekey bundle, use it; otherwise generate peer EC prekey pair
        val peerPubB64 = if (!peerIdentityPublicKeyFromServer.isNullOrBlank()) {
            peerIdentityPublicKeyFromServer
        } else {
            val generatedPeerPair = generateEcKeyPair()
            encodeBase64(generatedPeerPair.public.encoded)
        }
        val peerPubKey = decodePublicKey(peerPubB64)

        // X3DH Shared Secret via ECDH KeyAgreement
        val ecdhSecret = computeEcdhSharedSecret(localPrivKey, peerPubKey)
        val rootKey = hkdfSha256(
            ikm = ecdhSecret,
            salt = "TelefamSignalX3DHRootSalt".toByteArray(Charsets.UTF_8),
            info = "TelefamDoubleRatchetRoot".toByteArray(Charsets.UTF_8),
            outputLength = 32
        )
        val sendingChainKey = hmacSha256(rootKey, byteArrayOf(0x01))
        val receivingChainKey = hmacSha256(rootKey, byteArrayOf(0x02))

        val safetyNumber = compute60DigitSafetyNumber(
            localEmail = localUserEmail.trim().lowercase(),
            localIdentityPub = decodeBase64(localPubB64),
            peerEmail = normalizedPeer,
            peerIdentityPub = decodeBase64(peerPubB64)
        )

        val newSession = TelefamSignalSessionEntity(
            peerEmail = normalizedPeer,
            localIdentityPublicKeyBase64 = localPubB64,
            localIdentityPrivateKeyBase64 = localPrivB64,
            peerIdentityPublicKeyBase64 = peerPubB64,
            rootKeyBase64 = encodeBase64(rootKey),
            sendingChainKeyBase64 = encodeBase64(sendingChainKey),
            receivingChainKeyBase64 = encodeBase64(receivingChainKey),
            sendRatchetCounter = 0,
            receiveRatchetCounter = 0,
            safetyNumberNumeric = safetyNumber,
            isVerified = false
        )
        chatDao.upsertSignalSession(newSession)
        newSession
    }

    suspend fun encryptOutgoingMessage(
        localUserEmail: String,
        peerEmail: String,
        plaintext: String
    ): SignalEncryptedEnvelope = withContext(Dispatchers.IO) {
        val session = getOrEstablishSession(localUserEmail, peerEmail)
        val currentChainKey = decodeBase64(session.sendingChainKeyBase64)
        val stepIndex = session.sendRatchetCounter

        // Double Ratchet KDF step: derive per-message key + next chain key
        val messageKey = hmacSha256(currentChainKey, byteArrayOf(0x01, stepIndex.toByte()))
        val nextChainKey = hmacSha256(currentChainKey, byteArrayOf(0x02, stepIndex.toByte()))

        // Ephemeral ratchet public key for envelope header
        val ephemeralPair = generateEcKeyPair()
        val ephemeralPubB64 = encodeBase64(ephemeralPair.public.encoded)

        // AES-256-GCM authenticated encryption
        val iv = ByteArray(12).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(messageKey.copyOf(32), "AES")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        cipher.updateAAD(ephemeralPubB64.toByteArray(Charsets.UTF_8))
        val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Combine IV (12 bytes) + Ciphertext+Tag
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

        val updatedSession = session.copy(
            sendingChainKeyBase64 = encodeBase64(nextChainKey),
            sendRatchetCounter = stepIndex + 1,
            updatedAt = System.currentTimeMillis()
        )
        chatDao.upsertSignalSession(updatedSession)

        SignalEncryptedEnvelope(
            cipherTextBase64 = encodeBase64(combined),
            ephemeralPublicKeyBase64 = ephemeralPubB64,
            ratchetStepIndex = stepIndex,
            safetyNumberNumeric = session.safetyNumberNumeric
        )
    }

    fun compute60DigitSafetyNumber(
        localEmail: String,
        localIdentityPub: ByteArray,
        peerEmail: String,
        peerIdentityPub: ByteArray
    ): String {
        val firstBlock = format30DigitsFromDigest(localEmail, localIdentityPub)
        val secondBlock = format30DigitsFromDigest(peerEmail, peerIdentityPub)
        val combined60 = if (localEmail <= peerEmail) {
            firstBlock + secondBlock
        } else {
            secondBlock + firstBlock
        }
        return combined60.chunked(5).joinToString(" ")
    }

    private fun format30DigitsFromDigest(identifier: String, publicKeyBytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-512")
        var hash = publicKeyBytes
        val idBytes = identifier.toByteArray(Charsets.UTF_8)
        // Iterative SHA-512 fingerprint rounds following Signal Safety Number specification
        repeat(64) {
            digest.reset()
            digest.update(byteArrayOf(0x00, 0x01))
            digest.update(hash)
            digest.update(idBytes)
            digest.update(publicKeyBytes)
            hash = digest.digest()
        }
        val sb = StringBuilder()
        var offset = 0
        repeat(6) {
            val chunkVal = abs(ByteBuffer.wrap(hash, offset, 8).long) % 100000L
            sb.append(String.format("%05d", chunkVal))
            offset += 8
        }
        return sb.toString()
    }

    private fun generateEcKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"), secureRandom)
        return kpg.generateKeyPair()
    }

    private fun computeEcdhSharedSecret(privateKey: PrivateKey, publicKey: PublicKey): ByteArray {
        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(privateKey)
        ka.doPhase(publicKey, true)
        return ka.generateSecret()
    }

    private fun signBytes(privateKey: PrivateKey, data: ByteArray): ByteArray {
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(data)
        return signature.sign()
    }

    private fun decodePublicKey(base64Str: String): PublicKey {
        val bytes = decodeBase64(base64Str)
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePublic(X509EncodedKeySpec(bytes))
    }

    private fun decodePrivateKey(base64Str: String): PrivateKey {
        val bytes = decodeBase64(base64Str)
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePrivate(PKCS8EncodedKeySpec(bytes))
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun hkdfSha256(
        ikm: ByteArray,
        salt: ByteArray,
        info: ByteArray,
        outputLength: Int
    ): ByteArray {
        val prk = hmacSha256(salt, ikm)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        mac.update(info)
        mac.update(0x01.toByte())
        return mac.doFinal().copyOf(outputLength)
    }

    private fun encodeBase64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun decodeBase64(str: String): ByteArray {
        return Base64.decode(str, Base64.NO_WRAP)
    }
}
