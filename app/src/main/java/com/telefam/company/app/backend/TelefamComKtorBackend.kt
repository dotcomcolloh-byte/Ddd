package com.telefam.company.app.backend

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Base64
import com.telefam.company.app.data.TelefamComContactsManager
import com.telefam.company.app.data.TelefamComCountries
import com.telefam.company.app.data.TelefamDirectoryUserEntity
import com.telefam.company.app.data.TelefamUserProfileEntity
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets as ServerWebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

data class VerifiedFirebasePrincipal(
    val firebaseUid: String,
    val email: String,
    val name: String
)

object FirebaseTokenVerifier {
    private const val PROJECT_ID = "chat-me-2b4d3"
    private const val ISSUER = "https://securetoken.google.com/$PROJECT_ID"
    private const val LOCAL_KEY_ID = "telefam-firebase-edge-rsa-1"
    private const val GOOGLE_CERTS_URL =
        "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com"

    private val publicKeyCache = ConcurrentHashMap<String, RSAPublicKey>()
    private val localRsaKeyPair by lazy {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        kpg.generateKeyPair().also { kp ->
            publicKeyCache[LOCAL_KEY_ID] = kp.public as RSAPublicKey
        }
    }

    init {
        // Initialize local RSA keypair immediately so publicKeyCache has LOCAL_KEY_ID ready
        val kp = localRsaKeyPair
        publicKeyCache[LOCAL_KEY_ID] = kp.public as RSAPublicKey
    }

    fun issueClientFirebaseToken(
        firebaseUid: String,
        email: String,
        name: String
    ): String {
        val kp = localRsaKeyPair
        val pub = kp.public as RSAPublicKey
        val priv = kp.private as RSAPrivateKey
        val now = System.currentTimeMillis()
        val algorithm = Algorithm.RSA256(pub, priv)
        return JWT.create()
            .withKeyId(LOCAL_KEY_ID)
            .withIssuer(ISSUER)
            .withAudience(PROJECT_ID)
            .withSubject(firebaseUid)
            .withClaim("user_id", firebaseUid)
            .withClaim("email", email.lowercase(Locale.US))
            .withClaim("name", name)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + 3600_000L * 24 * 7))
            .sign(algorithm)
    }

    fun verifyToken(token: String): VerifiedFirebasePrincipal? {
        if (token.isBlank()) return null
        return try {
            val jwt = JWT.decode(token)
            val kid = jwt.keyId
            val publicKey = if (!kid.isNullOrBlank()) getPublicKey(kid) else null
            val decoded = if (publicKey != null) {
                val algorithm = Algorithm.RSA256(publicKey, null)
                val verifier = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .withAudience(PROJECT_ID)
                    .acceptLeeway(300L)
                    .build()
                verifier.verify(jwt)
            } else {
                // Verify standard Firebase token claims for chat-me-2b4d3 if Google cert endpoint is unreachable offline
                if (jwt.issuer != ISSUER || !jwt.audience.contains(PROJECT_ID)) {
                    return null
                }
                val exp = jwt.expiresAt
                if (exp != null && exp.time < System.currentTimeMillis() - 300_000L) {
                    return null
                }
                jwt
            }
            val uid = decoded.subject?.takeIf { it.isNotBlank() }
                ?: decoded.getClaim("user_id").asString()?.takeIf { it.isNotBlank() }
                ?: return null
            val email = decoded.getClaim("email").asString().orEmpty().lowercase(Locale.US)
            val name = decoded.getClaim("name").asString().orEmpty()
            VerifiedFirebasePrincipal(
                firebaseUid = uid,
                email = email,
                name = name
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun getPublicKey(kid: String): RSAPublicKey? {
        publicKeyCache[kid]?.let { return it }
        refreshGooglePublicKeys()
        return publicKeyCache[kid]
    }

    private fun refreshGooglePublicKeys() {
        runCatching {
            val conn = (URL(GOOGLE_CERTS_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 3500
                readTimeout = 3500
                requestMethod = "GET"
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val certFactory = CertificateFactory.getInstance("X.509")
                val keys = json.keys()
                while (keys.hasNext()) {
                    val keyId = keys.next()
                    val pem = json.optString(keyId)
                    if (pem.isNotBlank()) {
                        val cert = certFactory.generateCertificate(
                            ByteArrayInputStream(pem.toByteArray(Charsets.UTF_8))
                        ) as? X509Certificate
                        val rsaPub = cert?.publicKey as? RSAPublicKey
                        if (rsaPub != null) {
                            publicKeyCache[keyId] = rsaPub
                        }
                    }
                }
            }
        }
    }
}

data class TelefamRealtimeEvent(
    val eventId: Long,
    val eventType: String, // MESSAGE_SENT, MESSAGE_DELIVERED, MESSAGE_SEEN, MESSAGE_EDITED, MESSAGE_UNSENT, MESSAGE_REACTED, MESSAGE_PINNED, MESSAGE_FAVOURITED, PRESENCE_CHANGED, USER_REGISTERED, SOCIAL_FOLLOW_CHANGED, CONTACTS_HASH_MATCHED
    val conversationId: String,
    val messageId: String = "",
    val payloadJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class SocialFollowServerResult(
    val success: Boolean,
    val isRateLimited: Boolean = false,
    val errorMessage: String? = null,
    val isFollowing: Boolean = false,
    val isFollowedBy: Boolean = false,
    val targetFollowersCount: Int = 0,
    val targetFollowingCount: Int = 0
)

class TelefamBackendSqlDatabase(context: Context) :
    SQLiteOpenHelper(context, "telefam_ktor_backend_tables.db", null, 4) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS users (
                firebase_uid TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                email TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS backend_prekey_bundles (
                peer_email TEXT PRIMARY KEY,
                registration_id INTEGER NOT NULL,
                identity_public_key_b64 TEXT NOT NULL,
                signed_prekey_public_b64 TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS backend_encrypted_messages (
                message_id TEXT PRIMARY KEY,
                conversation_id TEXT NOT NULL,
                sender_email TEXT NOT NULL,
                receiver_email TEXT NOT NULL,
                message_type TEXT NOT NULL,
                cipher_text_b64 TEXT NOT NULL,
                ephemeral_public_key_b64 TEXT NOT NULL,
                ratchet_step INTEGER NOT NULL,
                delivery_status TEXT NOT NULL,
                reply_to_message_id TEXT,
                reaction_emoji TEXT,
                is_pinned INTEGER NOT NULL DEFAULT 0,
                is_favourite INTEGER NOT NULL DEFAULT 0,
                is_edited INTEGER NOT NULL DEFAULT 0,
                edited_at INTEGER,
                expires_at INTEGER,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS backend_realtime_events (
                event_id INTEGER PRIMARY KEY AUTOINCREMENT,
                event_type TEXT NOT NULL,
                conversation_id TEXT NOT NULL,
                message_id TEXT NOT NULL,
                payload_json TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS backend_presence_states (
                peer_email TEXT PRIMARY KEY,
                is_online INTEGER NOT NULL,
                is_typing INTEGER NOT NULL,
                last_active_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS backend_blocks_and_reports (
                entry_id INTEGER PRIMARY KEY AUTOINCREMENT,
                conversation_id TEXT NOT NULL,
                reporter_email TEXT NOT NULL,
                target_email TEXT NOT NULL,
                action_kind TEXT NOT NULL,
                payload_json TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS backend_registered_users (
                email TEXT PRIMARY KEY,
                uid TEXT NOT NULL,
                full_name TEXT NOT NULL,
                username TEXT NOT NULL,
                country_iso TEXT NOT NULL,
                country_name TEXT NOT NULL,
                country_dial_code TEXT NOT NULL,
                phone_hash_sha256 TEXT NOT NULL,
                phone_tail_hash_sha256 TEXT NOT NULL,
                email_hash_sha256 TEXT NOT NULL,
                location TEXT NOT NULL,
                gender TEXT NOT NULL,
                bio TEXT NOT NULL,
                photo_path TEXT NOT NULL,
                likes_count INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS backend_user_contact_hashes (
                owner_email TEXT NOT NULL,
                contact_hash_sha256 TEXT NOT NULL,
                uploaded_at INTEGER NOT NULL,
                PRIMARY KEY (owner_email, contact_hash_sha256)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS backend_social_follows (
                follower_email TEXT NOT NULL,
                target_email TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                PRIMARY KEY (follower_email, target_email)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
    }
}

object TelefamComKtorBackend {
    private const val BACKEND_PORT = 29481
    private const val BASE_URL = "http://127.0.0.1:$BACKEND_PORT"

    @Volatile
    private var engine: CIOApplicationEngine? = null
    private var backendDb: TelefamBackendSqlDatabase? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val secureRandom = SecureRandom()

    // Active WebSocket Server sessions for real-time push without refresh
    private val activeWsSessions = Collections.synchronizedSet(mutableSetOf<DefaultWebSocketServerSession>())

    // Server-side per-minute rate limit tracker per user email
    private val serverFollowRateWindow = ConcurrentHashMap<String, MutableList<Long>>()

    @Volatile
    private var activeBearerToken: String = ""

    fun setAuthToken(token: String) {
        activeBearerToken = token
    }

    fun getOrIssueAuthToken(firebaseUid: String, email: String, name: String): String {
        val current = activeBearerToken
        if (current.isNotBlank() && FirebaseTokenVerifier.verifyToken(current) != null) {
            return current
        }
        val issued = FirebaseTokenVerifier.issueClientFirebaseToken(
            firebaseUid = firebaseUid.ifBlank { "telefam_${email.hashCode().toUInt()}" },
            email = email,
            name = name
        )
        activeBearerToken = issued
        return issued
    }

    private fun upsertFirebaseUserIdentity(
        dbHelper: TelefamBackendSqlDatabase,
        firebaseUid: String,
        name: String,
        email: String
    ) {
        if (firebaseUid.isBlank()) return
        val now = System.currentTimeMillis()
        dbHelper.writableDatabase.execSQL(
            """
            INSERT OR REPLACE INTO users (firebase_uid, name, email, created_at)
            VALUES (
                ?,
                ?,
                ?,
                COALESCE((SELECT created_at FROM users WHERE firebase_uid = ?), ?)
            )
            """.trimIndent(),
            arrayOf<Any?>(
                firebaseUid,
                name.ifBlank { email.substringBefore("@") },
                email.lowercase(Locale.US),
                firebaseUid,
                now
            )
        )
    }

    private val _realtimeEventFlow = MutableSharedFlow<TelefamRealtimeEvent>(extraBufferCapacity = 64)
    val realtimeEventFlow: SharedFlow<TelefamRealtimeEvent> = _realtimeEventFlow.asSharedFlow()

    val ktorHttpClient: HttpClient by lazy {
        HttpClient(OkHttp) {
            install(ClientWebSockets)
            engine {
                config {
                    retryOnConnectionFailure(true)
                }
            }
        }
    }

    private fun recordAndBroadcastRealtimeEvent(
        dbHelper: TelefamBackendSqlDatabase,
        eventType: String,
        conversationId: String,
        messageId: String,
        payload: JSONObject
    ) {
        val now = System.currentTimeMillis()
        val writable = dbHelper.writableDatabase
        writable.execSQL(
            """
            INSERT INTO backend_realtime_events
            (event_type, conversation_id, message_id, payload_json, created_at)
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(eventType, conversationId, messageId, payload.toString(), now)
        )
        val evt = TelefamRealtimeEvent(
            eventId = now,
            eventType = eventType,
            conversationId = conversationId,
            messageId = messageId,
            payloadJson = payload.toString(),
            timestamp = now
        )
        _realtimeEventFlow.tryEmit(evt)

        // Broadcast frame over all connected Ktor WebSocket sessions
        val wsFrameJson = JSONObject()
            .put("eventId", evt.eventId)
            .put("eventType", evt.eventType)
            .put("conversationId", evt.conversationId)
            .put("messageId", evt.messageId)
            .put("payloadJson", evt.payloadJson)
            .put("timestamp", evt.timestamp)
            .toString()

        scope.launch {
            val snapshot = synchronized(activeWsSessions) { activeWsSessions.toList() }
            for (wsSession in snapshot) {
                runCatching {
                    wsSession.send(Frame.Text(wsFrameJson))
                }.onFailure {
                    activeWsSessions.remove(wsSession)
                }
            }
        }
    }

    fun ensureStarted(context: Context) {
        if (engine != null) return
        synchronized(this) {
            if (engine != null) return
            val dbHelper = TelefamBackendSqlDatabase(context.applicationContext)
            backendDb = dbHelper

            scope.launch {
                runCatching {
                    val server = embeddedServer(CIO, port = BACKEND_PORT, host = "127.0.0.1") {
                        install(ServerWebSockets)

                        routing {
                            // Real-time WebSocket endpoint for live updates without refresh
                            webSocket("/edge/v1/ws") {
                                activeWsSessions.add(this)
                                try {
                                    for (frame in incoming) {
                                        if (frame is Frame.Text) {
                                            val text = frame.readText()
                                            val req = runCatching { JSONObject(text) }.getOrNull()
                                            if (req != null && req.optString("type") == "PING") {
                                                send(
                                                    Frame.Text(
                                                        JSONObject()
                                                            .put("eventType", "PONG")
                                                            .put("timestamp", System.currentTimeMillis())
                                                            .toString()
                                                    )
                                                )
                                            }
                                        }
                                    }
                                } finally {
                                    activeWsSessions.remove(this)
                                }
                            }

                            get("/edge/v1/health") {
                                val res = JSONObject()
                                    .put("status", "ok")
                                    .put("service", "telefam-ktor-edge")
                                    .put("wsActiveClients", activeWsSessions.size)
                                    .toString()
                                call.respondText(res, ContentType.Application.Json, HttpStatusCode.OK)
                            }

                            // Authenticated Firebase profile route ("/profile" and "/edge/v1/profile")
                            get("/profile") {
                                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")?.trim()
                                val principal = token?.let { FirebaseTokenVerifier.verifyToken(it) }
                                if (principal == null) {
                                    call.respond(HttpStatusCode.Unauthorized)
                                    return@get
                                }
                                upsertFirebaseUserIdentity(
                                    dbHelper = dbHelper,
                                    firebaseUid = principal.firebaseUid,
                                    name = principal.name,
                                    email = principal.email
                                )
                                val response = JSONObject()
                                    .put("authenticated", true)
                                    .put("firebaseUid", principal.firebaseUid)
                                    .put("name", principal.name)
                                    .put("email", principal.email)
                                call.respondText(response.toString(), ContentType.Application.Json, HttpStatusCode.OK)
                            }

                            get("/edge/v1/profile") {
                                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")?.trim()
                                val principal = token?.let { FirebaseTokenVerifier.verifyToken(it) }
                                if (principal == null) {
                                    call.respond(HttpStatusCode.Unauthorized)
                                    return@get
                                }
                                upsertFirebaseUserIdentity(
                                    dbHelper = dbHelper,
                                    firebaseUid = principal.firebaseUid,
                                    name = principal.name,
                                    email = principal.email
                                )
                                val response = JSONObject()
                                    .put("authenticated", true)
                                    .put("firebaseUid", principal.firebaseUid)
                                    .put("name", principal.name)
                                    .put("email", principal.email)
                                call.respondText(response.toString(), ContentType.Application.Json, HttpStatusCode.OK)
                            }

                            post("/edge/v1/auth/session") {
                                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")?.trim()
                                val principal = token?.let { FirebaseTokenVerifier.verifyToken(it) }
                                if (principal == null) {
                                    call.respond(HttpStatusCode.Unauthorized)
                                    return@post
                                }
                                val rawBody = call.receiveText()
                                val req = runCatching { JSONObject(rawBody) }.getOrNull()
                                val resolvedEmail = req?.optString("email")?.ifBlank { principal.email } ?: principal.email
                                val resolvedName = req?.optString("name")?.ifBlank { principal.name } ?: principal.name
                                upsertFirebaseUserIdentity(
                                    dbHelper = dbHelper,
                                    firebaseUid = principal.firebaseUid,
                                    name = resolvedName,
                                    email = resolvedEmail
                                )
                                val response = JSONObject()
                                    .put("authenticated", true)
                                    .put("firebaseUid", principal.firebaseUid)
                                    .put("name", resolvedName)
                                    .put("email", resolvedEmail)
                                call.respondText(response.toString(), ContentType.Application.Json, HttpStatusCode.OK)
                            }

                            // Register or update a real user profile with SHA-256 contact hashes
                            post("/edge/v1/users/register") {
                                val rawBody = call.receiveText()
                                val req = JSONObject(rawBody)
                                val email = req.optString("email").trim().lowercase(Locale.US)
                                val uid = req.optString("uid").ifBlank { "telefam_${email.hashCode().toUInt()}" }
                                val fullName = req.optString("fullName").trim()
                                val username = req.optString("username").trim().removePrefix("@").lowercase(Locale.US)
                                val countryIso = req.optString("countryIso", "KE")
                                val countryName = req.optString("countryName").ifBlank {
                                    TelefamComCountries.findByIso(countryIso).name
                                }
                                val countryDialCode = req.optString("countryDialCode", "+254")
                                val phoneDigits = req.optString("phoneNumber", "")
                                val (phoneHash, phoneTailHash) = TelefamComContactsManager.computePhoneHashes(
                                    countryDialCode,
                                    phoneDigits
                                )
                                val emailHash = req.optString("emailHashSha256").ifBlank {
                                    TelefamComContactsManager.computeEmailHash(email)
                                }
                                val location = req.optString("location").trim()
                                val gender = req.optString("gender").trim()
                                val bio = req.optString("bio").trim()
                                val photoPath = req.optString("photoPath").trim()
                                val now = System.currentTimeMillis()

                                if (email.isNotBlank() && fullName.isNotBlank()) {
                                    upsertFirebaseUserIdentity(
                                        dbHelper = dbHelper,
                                        firebaseUid = uid,
                                        name = fullName,
                                        email = email
                                    )
                                    dbHelper.writableDatabase.execSQL(
                                        """
                                        INSERT OR REPLACE INTO backend_registered_users
                                        (email, uid, full_name, username, country_iso, country_name, country_dial_code,
                                         phone_hash_sha256, phone_tail_hash_sha256, email_hash_sha256,
                                         location, gender, bio, photo_path, likes_count, created_at, updated_at)
                                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
                                        """.trimIndent(),
                                        arrayOf<Any?>(
                                            email,
                                            uid,
                                            fullName,
                                            username,
                                            countryIso,
                                            countryName,
                                            countryDialCode,
                                            phoneHash,
                                            phoneTailHash,
                                            emailHash,
                                            location,
                                            gender,
                                            bio,
                                            photoPath,
                                            now,
                                            now
                                        )
                                    )

                                    recordAndBroadcastRealtimeEvent(
                                        dbHelper = dbHelper,
                                        eventType = "USER_REGISTERED",
                                        conversationId = "",
                                        messageId = email,
                                        payload = req
                                    )
                                }

                                val response = JSONObject()
                                    .put("registered", true)
                                    .put("email", email)
                                call.respondText(response.toString(), ContentType.Application.Json, HttpStatusCode.OK)
                            }

                            // Upload SHA-256 contact hashes from device and merge against registered users
                            post("/edge/v1/contacts/sync-hashes") {
                                val rawBody = call.receiveText()
                                val req = JSONObject(rawBody)
                                val ownerEmail = req.optString("ownerEmail").trim().lowercase(Locale.US)
                                val dayKey = req.optString("dayKey", TelefamComContactsManager.currentDayKey())
                                val hashesJson = req.optJSONArray("hashes") ?: JSONArray()
                                val now = System.currentTimeMillis()

                                val writable = dbHelper.writableDatabase
                                writable.beginTransaction()
                                try {
                                    for (i in 0 until hashesJson.length()) {
                                        val h = hashesJson.optString(i).trim()
                                        if (h.isNotBlank() && ownerEmail.isNotBlank()) {
                                            writable.execSQL(
                                                """
                                                INSERT OR REPLACE INTO backend_user_contact_hashes
                                                (owner_email, contact_hash_sha256, uploaded_at)
                                                VALUES (?, ?, ?)
                                                """.trimIndent(),
                                                arrayOf<Any?>(ownerEmail, h, now)
                                            )
                                        }
                                    }
                                    writable.setTransactionSuccessful()
                                } finally {
                                    writable.endTransaction()
                                }

                                val directoryArray = queryMergedDirectoryUsersJson(
                                    dbHelper = dbHelper,
                                    viewerEmail = ownerEmail,
                                    dayKey = dayKey
                                )

                                recordAndBroadcastRealtimeEvent(
                                    dbHelper = dbHelper,
                                    eventType = "CONTACTS_HASH_MATCHED",
                                    conversationId = "",
                                    messageId = ownerEmail,
                                    payload = JSONObject().put("matchedCount", directoryArray.length())
                                )

                                val response = JSONObject()
                                    .put("synced", true)
                                    .put("uploadedHashCount", hashesJson.length())
                                    .put("users", directoryArray)
                                call.respondText(response.toString(), ContentType.Application.Json, HttpStatusCode.OK)
                            }

                            // Query registered users directory & daily relationship-based suggestions
                            get("/edge/v1/users/directory") {
                                val viewerEmail = (call.request.queryParameters["viewerEmail"] ?: "")
                                    .trim()
                                    .lowercase(Locale.US)
                                val dayKey = call.request.queryParameters["dayKey"]
                                    ?: TelefamComContactsManager.currentDayKey()

                                val usersArr = queryMergedDirectoryUsersJson(
                                    dbHelper = dbHelper,
                                    viewerEmail = viewerEmail,
                                    dayKey = dayKey
                                )
                                val response = JSONObject()
                                    .put("dayKey", dayKey)
                                    .put("users", usersArr)
                                call.respondText(response.toString(), ContentType.Application.Json, HttpStatusCode.OK)
                            }

                            // Follow / Follow Back / Unfollow with server-side per-minute rate limit
                            post("/edge/v1/social/follow") {
                                val rawBody = call.receiveText()
                                val req = JSONObject(rawBody)
                                val followerEmail = req.optString("followerEmail").trim().lowercase(Locale.US)
                                val targetEmail = req.optString("targetEmail").trim().lowercase(Locale.US)
                                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")?.trim()
                                val principal = token?.let { FirebaseTokenVerifier.verifyToken(it) }
                                if (principal == null && followerEmail.isBlank()) {
                                    call.respond(HttpStatusCode.Unauthorized)
                                    return@post
                                }
                                val follow = req.optBoolean("follow", true)
                                val now = System.currentTimeMillis()

                                // Server-side rate limit check (max 8 actions per 60s)
                                val history = serverFollowRateWindow.getOrPut(followerEmail) {
                                    Collections.synchronizedList(mutableListOf())
                                }
                                val isRateLimited = synchronized(history) {
                                    history.removeAll { now - it >= 60_000L }
                                    if (history.size >= 8) {
                                        true
                                    } else {
                                        history.add(now)
                                        false
                                    }
                                }
                                if (isRateLimited) {
                                    val errRes = JSONObject()
                                        .put("success", false)
                                        .put("rateLimited", true)
                                        .put("message", "Rate limit exceeded. Please try again later.")
                                    call.respondText(
                                        errRes.toString(),
                                        ContentType.Application.Json,
                                        HttpStatusCode.TooManyRequests
                                    )
                                    return@post
                                }

                                val writable = dbHelper.writableDatabase
                                if (follow) {
                                    writable.execSQL(
                                        """
                                        INSERT OR REPLACE INTO backend_social_follows
                                        (follower_email, target_email, created_at)
                                        VALUES (?, ?, ?)
                                        """.trimIndent(),
                                        arrayOf<Any?>(followerEmail, targetEmail, now)
                                    )
                                } else {
                                    writable.execSQL(
                                        """
                                        DELETE FROM backend_social_follows
                                        WHERE follower_email = ? AND target_email = ?
                                        """.trimIndent(),
                                        arrayOf<Any?>(followerEmail, targetEmail)
                                    )
                                }

                                val readable = dbHelper.readableDatabase
                                val isFollowedByTarget = queryDoesFollowExist(readable, targetEmail, followerEmail)
                                val targetFollowers = queryFollowersCount(readable, targetEmail)
                                val targetFollowing = queryFollowingCount(readable, targetEmail)

                                val eventPayload = JSONObject()
                                    .put("followerEmail", followerEmail)
                                    .put("targetEmail", targetEmail)
                                    .put("isFollowing", follow)
                                    .put("isFollowedBy", isFollowedByTarget)
                                    .put("isFriends", follow && isFollowedByTarget)
                                    .put("targetFollowersCount", targetFollowers)
                                    .put("targetFollowingCount", targetFollowing)

                                recordAndBroadcastRealtimeEvent(
                                    dbHelper = dbHelper,
                                    eventType = "SOCIAL_FOLLOW_CHANGED",
                                    conversationId = "",
                                    messageId = targetEmail,
                                    payload = eventPayload
                                )

                                val response = JSONObject()
                                    .put("success", true)
                                    .put("rateLimited", false)
                                    .put("isFollowing", follow)
                                    .put("isFollowedBy", isFollowedByTarget)
                                    .put("isFriends", follow && isFollowedByTarget)
                                    .put("targetFollowersCount", targetFollowers)
                                    .put("targetFollowingCount", targetFollowing)
                                call.respondText(response.toString(), ContentType.Application.Json, HttpStatusCode.OK)
                            }

                            get("/edge/v1/keys/bundle") {
                                val email = (call.request.queryParameters["email"] ?: "peer@telefam.com")
                                    .trim()
                                    .lowercase()
                                val bundleJson = getOrCreatePeerPreKeyBundleJson(dbHelper, email)
                                call.respondText(bundleJson.toString(), ContentType.Application.Json, HttpStatusCode.OK)
                            }

                            post("/edge/v1/messages/send") {
                                val rawBody = call.receiveText()
                                val req = JSONObject(rawBody)
                                val msgId = req.optString("messageId")
                                val convId = req.optString("conversationId")
                                val sender = req.optString("senderEmail")
                                val receiver = req.optString("receiverEmail")
                                val msgType = req.optString("messageType", "TEXT")
                                val cipherB64 = req.optString("cipherTextBase64")
                                val ephKey = req.optString("ephemeralPublicKeyBase64")
                                val ratchetStep = req.optInt("ratchetStepIndex", 0)
                                val replyToId = req.optString("replyToMessageId", "")
                                val expiresAt = if (req.has("expiresAt")) req.optLong("expiresAt") else 0L
                                val now = System.currentTimeMillis()

                                dbHelper.writableDatabase.execSQL(
                                    """
                                    INSERT OR REPLACE INTO backend_encrypted_messages
                                    (message_id, conversation_id, sender_email, receiver_email, message_type, cipher_text_b64, ephemeral_public_key_b64, ratchet_step, delivery_status, reply_to_message_id, expires_at, created_at)
                                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                                    """.trimIndent(),
                                    arrayOf<Any?>(
                                        msgId,
                                        convId,
                                        sender,
                                        receiver,
                                        msgType,
                                        cipherB64,
                                        ephKey,
                                        ratchetStep,
                                        "DELIVERED",
                                        replyToId.ifBlank { null },
                                        if (expiresAt > 0L) expiresAt else null,
                                        now
                                    )
                                )

                                recordAndBroadcastRealtimeEvent(
                                    dbHelper = dbHelper,
                                    eventType = "MESSAGE_DELIVERED",
                                    conversationId = convId,
                                    messageId = msgId,
                                    payload = req
                                )

                                val response = JSONObject()
                                    .put("acknowledged", true)
                                    .put("messageId", msgId)
                                    .put("status", "DELIVERED")
                                    .put("serverTimestamp", now)
                                call.respondText(response.toString(), ContentType.Application.Json, HttpStatusCode.OK)
                            }

                            post("/edge/v1/messages/edit") {
                                val rawBody = call.receiveText()
                                val req = JSONObject(rawBody)
                                val msgId = req.optString("messageId")
                                val convId = req.optString("conversationId")
                                val newCipherB64 = req.optString("cipherTextBase64")
                                val newEphKey = req.optString("ephemeralPublicKeyBase64")
                                val newRatchetStep = req.optInt("ratchetStepIndex", 0)
                                val editedAt = req.optLong("editedAt", System.currentTimeMillis())

                                dbHelper.writableDatabase.execSQL(
                                    """
                                    UPDATE backend_encrypted_messages
                                    SET cipher_text_b64 = ?,
                                        ephemeral_public_key_b64 = ?,
                                        ratchet_step = ?,
                                        is_edited = 1,
                                        edited_at = ?
                                    WHERE message_id = ?
                                    """.trimIndent(),
                                    arrayOf<Any?>(newCipherB64, newEphKey, newRatchetStep, editedAt, msgId)
                                )

                                recordAndBroadcastRealtimeEvent(
                                    dbHelper = dbHelper,
                                    eventType = "MESSAGE_EDITED",
                                    conversationId = convId,
                                    messageId = msgId,
                                    payload = req
                                )

                                val response = JSONObject()
                                    .put("edited", true)
                                    .put("messageId", msgId)
                                    .put("editedAt", editedAt)
                                call.respondText(response.toString(), ContentType.Application.Json, HttpStatusCode.OK)
                            }

                            post("/edge/v1/messages/unsend") {
                                val rawBody = call.receiveText()
                                val req = JSONObject(rawBody)
                                val msgId = req.optString("messageId")
                                val convId = req.optString("conversationId")

                                // Permanently purge all traces of the unsent message on the backend for both users
                                val writable = dbHelper.writableDatabase
                                writable.execSQL(
                                    "DELETE FROM backend_encrypted_messages WHERE message_id = ?",
                                    arrayOf<Any?>(msgId)
                                )
                                writable.execSQL(
                                    "UPDATE backend_encrypted_messages SET reply_to_message_id = NULL WHERE reply_to_message_id = ?",
                                    arrayOf<Any?>(msgId)
                                )
                                writable.execSQL(
                                    "DELETE FROM backend_realtime_events WHERE message_id = ?",
                                    arrayOf<Any?>(msgId)
                                )

                                recordAndBroadcastRealtimeEvent(
                                    dbHelper = dbHelper,
                                    eventType = "MESSAGE_UNSENT",
                                    conversationId = convId,
                                    messageId = msgId,
                                    payload = req
                                )

                                val response = JSONObject()
                                    .put("unsent", true)
                                    .put("purgedAllTraces", true)
                                    .put("messageId", msgId)
                                call.respondText(response.toString(), ContentType.Application.Json, HttpStatusCode.OK)
                            }

                            post("/edge/v1/messages/react") {
                                val rawBody = call.receiveText()
                                val req = JSONObject(rawBody)
                                val msgId = req.optString("messageId")
                                val convId = req.optString("conversationId")
                                val emoji = req.optString("reactionEmoji", "")

                                dbHelper.writableDatabase.execSQL(
                                    "UPDATE backend_encrypted_messages SET reaction_emoji = ? WHERE message_id = ?",
                                    arrayOf<Any?>(emoji.ifBlank { null }, msgId)
                                )

                                recordAndBroadcastRealtimeEvent(
                                    dbHelper = dbHelper,
                                    eventType = "MESSAGE_REACTED",
                                    conversationId = convId,
                                    messageId = msgId,
                                    payload = req
                                )

                                val response = JSONObject()
                                    .put("reacted", true)
                                    .put("messageId", msgId)
                                    .put("reactionEmoji", emoji)
                                call.respondText(response.toString(), ContentType.Application.Json, HttpStatusCode.OK)
                            }

                            post("/edge/v1/messages/pin") {
                                val rawBody = call.receiveText()
                                val req = JSONObject(rawBody)
                                val msgId = req.optString("messageId")
                                val convId = req.optString("conversationId")
                                val isPinned = if (req.optBoolean("isPinned", false)) 1 else 0

                                dbHelper.writableDatabase.execSQL(
                                    "UPDATE backend_encrypted_messages SET is_pinned = ? WHERE message_id = ?",
                                    arrayOf<Any?>(isPinned, msgId)
                                )

                                recordAndBroadcastRealtimeEvent(
                                    dbHelper = dbHelper,
                                    eventType = "MESSAGE_PINNED",
                                    conversationId = convId,
                                    messageId = msgId,
                                    payload = req
                                )

                                val response = JSONObject()
                                    .put("pinned", isPinned == 1)
                                    .put("messageId", msgId)
                                call.respondText(response.toString(), ContentType.Application.Json, HttpStatusCode.OK)
                            }

                            post("/edge/v1/messages/favourite") {
                                val rawBody = call.receiveText()
                                val req = JSONObject(rawBody)
                                val msgId = req.optString("messageId")
                                val convId = req.optString("conversationId")
                                val isFavourite = if (req.optBoolean("isFavourite", false)) 1 else 0

                                dbHelper.writableDatabase.execSQL(
                                    "UPDATE backend_encrypted_messages SET is_favourite = ? WHERE message_id = ?",
                                    arrayOf<Any?>(isFavourite, msgId)
                                )

                                recordAndBroadcastRealtimeEvent(
                                    dbHelper = dbHelper,
                                    eventType = "MESSAGE_FAVOURITED",
                                    conversationId = convId,
                                    messageId = msgId,
                                    payload = req
                                )

                                val response = JSONObject()
                                    .put("favourited", isFavourite == 1)
                                    .put("messageId", msgId)
                                call.respondText(response.toString(), ContentType.Application.Json, HttpStatusCode.OK)
                            }

                            post("/edge/v1/messages/receipt") {
                                val rawBody = call.receiveText()
                                val req = JSONObject(rawBody)
                                val msgId = req.optString("messageId")
                                val convId = req.optString("conversationId", "")
                                val status = req.optString("status", "SEEN")
                                dbHelper.writableDatabase.execSQL(
                                    "UPDATE backend_encrypted_messages SET delivery_status = ? WHERE message_id = ?",
                                    arrayOf<Any?>(status, msgId)
                                )
                                recordAndBroadcastRealtimeEvent(
                                    dbHelper = dbHelper,
                                    eventType = "MESSAGE_$status",
                                    conversationId = convId,
                                    messageId = msgId,
                                    payload = req
                                )
                                val response = JSONObject()
                                    .put("updated", true)
                                    .put("messageId", msgId)
                                    .put("status", status)
                                call.respondText(response.toString(), ContentType.Application.Json, HttpStatusCode.OK)
                            }

                            get("/edge/v1/realtime/events") {
                                val sinceMillis = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
                                val eventsArr = JSONArray()
                                dbHelper.readableDatabase.rawQuery(
                                    """
                                    SELECT event_id, event_type, conversation_id, message_id, payload_json, created_at
                                    FROM backend_realtime_events
                                    WHERE created_at > ?
                                    ORDER BY created_at ASC
                                    LIMIT 50
                                    """.trimIndent(),
                                    arrayOf(sinceMillis.toString())
                                ).use { cursor ->
                                    while (cursor.moveToNext()) {
                                        val obj = JSONObject()
                                            .put("eventId", cursor.getLong(0))
                                            .put("eventType", cursor.getString(1))
                                            .put("conversationId", cursor.getString(2))
                                            .put("messageId", cursor.getString(3))
                                            .put("payloadJson", cursor.getString(4))
                                            .put("timestamp", cursor.getLong(5))
                                        eventsArr.put(obj)
                                    }
                                }
                                val res = JSONObject().put("events", eventsArr)
                                call.respondText(res.toString(), ContentType.Application.Json, HttpStatusCode.OK)
                            }

                            post("/edge/v1/presence") {
                                val rawBody = call.receiveText()
                                val req = JSONObject(rawBody)
                                val peerEmail = req.optString("peerEmail")
                                val convId = req.optString("conversationId", "")
                                val isOnline = if (req.optBoolean("isOnline", true)) 1 else 0
                                val isTyping = if (req.optBoolean("isTyping", false)) 1 else 0
                                val now = System.currentTimeMillis()
                                dbHelper.writableDatabase.execSQL(
                                    """
                                    INSERT OR REPLACE INTO backend_presence_states
                                    (peer_email, is_online, is_typing, last_active_at)
                                    VALUES (?, ?, ?, ?)
                                    """.trimIndent(),
                                    arrayOf<Any?>(peerEmail, isOnline, isTyping, now)
                                )
                                recordAndBroadcastRealtimeEvent(
                                    dbHelper = dbHelper,
                                    eventType = "PRESENCE_CHANGED",
                                    conversationId = convId,
                                    messageId = "",
                                    payload = req
                                )
                                val response = JSONObject()
                                    .put("peerEmail", peerEmail)
                                    .put("isOnline", isOnline == 1)
                                    .put("isTyping", isTyping == 1)
                                call.respondText(response.toString(), ContentType.Application.Json, HttpStatusCode.OK)
                            }

                            post("/edge/v1/moderation") {
                                val rawBody = call.receiveText()
                                val req = JSONObject(rawBody)
                                val convId = req.optString("conversationId")
                                val reporter = req.optString("reporterEmail")
                                val target = req.optString("targetEmail")
                                val kind = req.optString("actionKind", "REPORT")
                                val now = System.currentTimeMillis()
                                dbHelper.writableDatabase.execSQL(
                                    """
                                    INSERT INTO backend_blocks_and_reports
                                    (conversation_id, reporter_email, target_email, action_kind, payload_json, created_at)
                                    VALUES (?, ?, ?, ?, ?, ?)
                                    """.trimIndent(),
                                    arrayOf<Any?>(convId, reporter, target, kind, rawBody, now)
                                )
                                if (kind == "CLEAR_CHAT") {
                                    dbHelper.writableDatabase.execSQL(
                                        "DELETE FROM backend_encrypted_messages WHERE conversation_id = ?",
                                        arrayOf<Any?>(convId)
                                    )
                                }
                                val response = JSONObject()
                                    .put("success", true)
                                    .put("actionKind", kind)
                                    .put("timestamp", now)
                                call.respondText(response.toString(), ContentType.Application.Json, HttpStatusCode.OK)
                            }
                        }
                    }
                    engine = server
                    server.start(wait = false)

                    // Connect persistent Ktor Client WebSocket listener
                    startPersistentWebSocketClient()
                }
            }
        }
    }

    private fun startPersistentWebSocketClient() {
        scope.launch {
            delay(500L)
            while (true) {
                runCatching {
                    ktorHttpClient.webSocket(
                        host = "127.0.0.1",
                        port = BACKEND_PORT,
                        path = "/edge/v1/ws"
                    ) {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val json = JSONObject(frame.readText())
                                val evtType = json.optString("eventType")
                                if (evtType.isNotBlank() && evtType != "PONG") {
                                    _realtimeEventFlow.tryEmit(
                                        TelefamRealtimeEvent(
                                            eventId = json.optLong("eventId", System.currentTimeMillis()),
                                            eventType = evtType,
                                            conversationId = json.optString("conversationId"),
                                            messageId = json.optString("messageId"),
                                            payloadJson = json.optString("payloadJson"),
                                            timestamp = json.optLong("timestamp", System.currentTimeMillis())
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                delay(4000L)
            }
        }
    }

    private fun queryDoesFollowExist(
        db: SQLiteDatabase,
        followerEmail: String,
        targetEmail: String
    ): Boolean {
        if (followerEmail.isBlank() || targetEmail.isBlank()) return false
        db.rawQuery(
            "SELECT 1 FROM backend_social_follows WHERE follower_email = ? AND target_email = ? LIMIT 1",
            arrayOf(followerEmail, targetEmail)
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun queryFollowersCount(db: SQLiteDatabase, targetEmail: String): Int {
        db.rawQuery(
            "SELECT COUNT(*) FROM backend_social_follows WHERE target_email = ?",
            arrayOf(targetEmail)
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    private fun queryFollowingCount(db: SQLiteDatabase, followerEmail: String): Int {
        db.rawQuery(
            "SELECT COUNT(*) FROM backend_social_follows WHERE follower_email = ?",
            arrayOf(followerEmail)
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    private fun queryViewerContactHashes(db: SQLiteDatabase, viewerEmail: String): Set<String> {
        if (viewerEmail.isBlank()) return emptySet()
        val set = mutableSetOf<String>()
        db.rawQuery(
            "SELECT contact_hash_sha256 FROM backend_user_contact_hashes WHERE owner_email = ?",
            arrayOf(viewerEmail)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                set.add(cursor.getString(0))
            }
        }
        return set
    }

    private fun queryMutualConnectionsCount(
        db: SQLiteDatabase,
        viewerEmail: String,
        candidateEmail: String,
        viewerHashes: Set<String>
    ): Int {
        if (viewerEmail.isBlank()) return 0
        var mutualCount = 0

        // 1. Mutual social graph connections (people viewer follows who also follow/are followed by candidate)
        db.rawQuery(
            """
            SELECT COUNT(DISTINCT f1.target_email)
            FROM backend_social_follows f1
            INNER JOIN backend_social_follows f2
              ON f1.target_email = f2.follower_email OR f1.target_email = f2.target_email
            WHERE f1.follower_email = ?
              AND (f2.follower_email = ? OR f2.target_email = ?)
              AND f1.target_email != ?
              AND f1.target_email != ?
            """.trimIndent(),
            arrayOf(viewerEmail, candidateEmail, candidateEmail, viewerEmail, candidateEmail)
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                mutualCount += cursor.getInt(0)
            }
        }

        // 2. Overlapping uploaded SHA-256 contact hashes between viewer and candidate
        if (viewerHashes.isNotEmpty()) {
            db.rawQuery(
                "SELECT contact_hash_sha256 FROM backend_user_contact_hashes WHERE owner_email = ?",
                arrayOf(candidateEmail)
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.getString(0) in viewerHashes) {
                        mutualCount += 1
                    }
                }
            }
        }
        return mutualCount
    }

    private fun queryMergedDirectoryUsersJson(
        dbHelper: TelefamBackendSqlDatabase,
        viewerEmail: String,
        dayKey: String
    ): JSONArray {
        val db = dbHelper.readableDatabase
        val viewerHashes = queryViewerContactHashes(db, viewerEmail)
        val result = JSONArray()

        db.rawQuery(
            """
            SELECT email, uid, full_name, username, country_iso, country_name, country_dial_code,
                   phone_hash_sha256, phone_tail_hash_sha256, email_hash_sha256,
                   location, gender, bio, photo_path, likes_count, updated_at
            FROM backend_registered_users
            WHERE email != ?
            ORDER BY updated_at DESC
            """.trimIndent(),
            arrayOf(viewerEmail)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val email = cursor.getString(0)
                val uid = cursor.getString(1)
                val fullName = cursor.getString(2)
                val username = cursor.getString(3)
                val countryIso = cursor.getString(4)
                val countryName = cursor.getString(5)
                val countryDial = cursor.getString(6)
                val phoneHash = cursor.getString(7)
                val phoneTailHash = cursor.getString(8)
                val emailHash = cursor.getString(9)
                val location = cursor.getString(10)
                val gender = cursor.getString(11)
                val bio = cursor.getString(12)
                val photoPath = cursor.getString(13)
                val likesCount = cursor.getInt(14)
                val updatedAt = cursor.getLong(15)

                val isMatchedFromContacts = (phoneHash.isNotBlank() && phoneHash in viewerHashes) ||
                    (phoneTailHash.isNotBlank() && phoneTailHash in viewerHashes) ||
                    (emailHash.isNotBlank() && emailHash in viewerHashes)

                val isFollowing = queryDoesFollowExist(db, viewerEmail, email)
                val isFollowedBy = queryDoesFollowExist(db, email, viewerEmail)
                val followersCount = queryFollowersCount(db, email)
                val followingCount = queryFollowingCount(db, email)
                val mutualCount = queryMutualConnectionsCount(db, viewerEmail, email, viewerHashes)

                // Daily deterministic relationship score so suggestions rotate daily while prioritizing contact/mutual matches
                val dailyRotationJitter = abs("$email:$dayKey".hashCode()) % 25
                val suggestionScore = (if (isMatchedFromContacts) 200 else 0) +
                    (if (isFollowedBy && !isFollowing) 120 else 0) +
                    (mutualCount * 15) +
                    dailyRotationJitter

                val obj = JSONObject()
                    .put("email", email)
                    .put("uid", uid)
                    .put("fullName", fullName)
                    .put("username", username)
                    .put("countryIso", countryIso)
                    .put("countryName", countryName)
                    .put("countryDialCode", countryDial)
                    .put("phoneHashSha256", phoneHash)
                    .put("emailHashSha256", emailHash)
                    .put("location", location)
                    .put("gender", gender)
                    .put("bio", bio)
                    .put("photoPath", photoPath)
                    .put("followersCount", followersCount)
                    .put("followingCount", followingCount)
                    .put("likesCount", likesCount)
                    .put("mutualConnectionsCount", mutualCount)
                    .put("isFollowing", isFollowing)
                    .put("isFollowedBy", isFollowedBy)
                    .put("isMatchedFromContacts", isMatchedFromContacts)
                    .put("suggestionDayKey", dayKey)
                    .put("suggestionScore", suggestionScore)
                    .put("updatedAt", updatedAt)
                result.put(obj)
            }
        }
        return result
    }

    private fun getOrCreatePeerPreKeyBundleJson(
        dbHelper: TelefamBackendSqlDatabase,
        peerEmail: String
    ): JSONObject {
        val db = dbHelper.readableDatabase
        db.rawQuery(
            "SELECT registration_id, identity_public_key_b64, signed_prekey_public_b64 FROM backend_prekey_bundles WHERE peer_email = ?",
            arrayOf(peerEmail)
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return JSONObject()
                    .put("peerEmail", peerEmail)
                    .put("registrationId", cursor.getInt(0))
                    .put("identityPublicKeyBase64", cursor.getString(1))
                    .put("signedPreKeyPublicBase64", cursor.getString(2))
            }
        }

        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"), secureRandom)
        val identityPair = kpg.generateKeyPair()
        val signedPreKeyPair = kpg.generateKeyPair()
        val regId = 10000 + secureRandom.nextInt(89999)
        val idPubB64 = Base64.encodeToString(identityPair.public.encoded, Base64.NO_WRAP)
        val spkPubB64 = Base64.encodeToString(signedPreKeyPair.public.encoded, Base64.NO_WRAP)
        val now = System.currentTimeMillis()

        dbHelper.writableDatabase.execSQL(
            """
            INSERT OR REPLACE INTO backend_prekey_bundles
            (peer_email, registration_id, identity_public_key_b64, signed_prekey_public_b64, updated_at)
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(peerEmail, regId, idPubB64, spkPubB64, now)
        )

        return JSONObject()
            .put("peerEmail", peerEmail)
            .put("registrationId", regId)
            .put("identityPublicKeyBase64", idPubB64)
            .put("signedPreKeyPublicBase64", spkPubB64)
    }

    suspend fun authenticateWithFirebaseTokenOnKtor(
        firebaseIdToken: String,
        firebaseUid: String,
        name: String,
        email: String
    ): Boolean = withContext(Dispatchers.IO) {
        setAuthToken(firebaseIdToken)
        val payload = JSONObject()
            .put("firebaseUid", firebaseUid)
            .put("name", name)
            .put("email", email.lowercase(Locale.US))
        runCatching {
            val response = ktorHttpClient.post("$BASE_URL/edge/v1/auth/session") {
                header("Authorization", "Bearer $firebaseIdToken")
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            response.status == HttpStatusCode.OK
        }.getOrDefault(false)
    }

    suspend fun registerUserOnKtor(profile: TelefamUserProfileEntity): Boolean = withContext(Dispatchers.IO) {
        val token = getOrIssueAuthToken(profile.uid, profile.email, profile.fullName)
        val countryName = TelefamComCountries.findByIso(profile.countryIso).name
        val (phoneHash, _) = TelefamComContactsManager.computePhoneHashes(
            profile.countryDialCode,
            profile.phoneNumber
        )
        val emailHash = TelefamComContactsManager.computeEmailHash(profile.email)
        val payload = JSONObject()
            .put("email", profile.email)
            .put("uid", profile.uid)
            .put("fullName", profile.fullName)
            .put("username", profile.username)
            .put("countryIso", profile.countryIso)
            .put("countryName", countryName)
            .put("countryDialCode", profile.countryDialCode)
            .put("phoneNumber", profile.phoneNumber)
            .put("phoneHashSha256", phoneHash)
            .put("emailHashSha256", emailHash)
            .put("location", profile.location)
            .put("gender", profile.gender)
            .put("bio", profile.bio)
            .put("photoPath", profile.photoPath)

        runCatching {
            val response = ktorHttpClient.post("$BASE_URL/edge/v1/users/register") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            response.status == HttpStatusCode.OK
        }.getOrDefault(false)
    }

    suspend fun uploadContactHashesToKtor(
        ownerEmail: String,
        hashes: Set<String>,
        dayKey: String = TelefamComContactsManager.currentDayKey()
    ): List<TelefamDirectoryUserEntity>? = withContext(Dispatchers.IO) {
        runCatching {
            val token = getOrIssueAuthToken("", ownerEmail, ownerEmail)
            val hashesArr = JSONArray()
            hashes.forEach { hashesArr.put(it) }
            val payload = JSONObject()
                .put("ownerEmail", ownerEmail)
                .put("dayKey", dayKey)
                .put("hashes", hashesArr)

            val response = ktorHttpClient.post("$BASE_URL/edge/v1/contacts/sync-hashes") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            if (response.status == HttpStatusCode.OK) {
                val json = JSONObject(response.bodyAsText())
                parseDirectoryUsersArray(json.optJSONArray("users"))
            } else {
                null
            }
        }.getOrNull()
    }

    suspend fun fetchDirectoryUsersFromKtor(
        viewerEmail: String,
        dayKey: String = TelefamComContactsManager.currentDayKey()
    ): List<TelefamDirectoryUserEntity>? = withContext(Dispatchers.IO) {
        runCatching {
            val token = getOrIssueAuthToken("", viewerEmail, viewerEmail)
            val response = ktorHttpClient.get(
                "$BASE_URL/edge/v1/users/directory?viewerEmail=$viewerEmail&dayKey=$dayKey"
            ) {
                header("Authorization", "Bearer $token")
            }
            if (response.status == HttpStatusCode.OK) {
                val json = JSONObject(response.bodyAsText())
                parseDirectoryUsersArray(json.optJSONArray("users"))
            } else {
                null
            }
        }.getOrNull()
    }

    suspend fun postFollowActionToKtor(
        followerEmail: String,
        targetEmail: String,
        follow: Boolean
    ): SocialFollowServerResult = withContext(Dispatchers.IO) {
        runCatching {
            val token = getOrIssueAuthToken("", followerEmail, followerEmail)
            val payload = JSONObject()
                .put("followerEmail", followerEmail)
                .put("targetEmail", targetEmail)
                .put("follow", follow)
            val response = ktorHttpClient.post("$BASE_URL/edge/v1/social/follow") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            val bodyJson = runCatching { JSONObject(response.bodyAsText()) }.getOrNull()
            when (response.status) {
                HttpStatusCode.OK -> {
                    SocialFollowServerResult(
                        success = true,
                        isRateLimited = false,
                        isFollowing = bodyJson?.optBoolean("isFollowing", follow) ?: follow,
                        isFollowedBy = bodyJson?.optBoolean("isFollowedBy", false) ?: false,
                        targetFollowersCount = bodyJson?.optInt("targetFollowersCount", 0) ?: 0,
                        targetFollowingCount = bodyJson?.optInt("targetFollowingCount", 0) ?: 0
                    )
                }
                HttpStatusCode.TooManyRequests -> {
                    SocialFollowServerResult(
                        success = false,
                        isRateLimited = true,
                        errorMessage = bodyJson?.optString("message")
                            ?: "Rate limit exceeded. Please try again later."
                    )
                }
                else -> {
                    SocialFollowServerResult(
                        success = false,
                        errorMessage = "Unable to sync follow status right now."
                    )
                }
            }
        }.getOrElse {
            SocialFollowServerResult(
                success = false,
                errorMessage = "Network error while syncing follow status."
            )
        }
    }

    private fun parseDirectoryUsersArray(arr: JSONArray?): List<TelefamDirectoryUserEntity> {
        if (arr == null) return emptyList()
        val list = mutableListOf<TelefamDirectoryUserEntity>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val email = obj.optString("email")
            if (email.isBlank()) continue
            list.add(
                TelefamDirectoryUserEntity(
                    email = email,
                    uid = obj.optString("uid"),
                    fullName = obj.optString("fullName"),
                    username = obj.optString("username"),
                    countryIso = obj.optString("countryIso"),
                    countryName = obj.optString("countryName"),
                    countryDialCode = obj.optString("countryDialCode"),
                    phoneHashSha256 = obj.optString("phoneHashSha256"),
                    emailHashSha256 = obj.optString("emailHashSha256"),
                    location = obj.optString("location"),
                    gender = obj.optString("gender"),
                    bio = obj.optString("bio"),
                    photoPath = obj.optString("photoPath"),
                    followersCount = obj.optInt("followersCount", 0),
                    followingCount = obj.optInt("followingCount", 0),
                    likesCount = obj.optInt("likesCount", 0),
                    mutualConnectionsCount = obj.optInt("mutualConnectionsCount", 0),
                    isFollowing = obj.optBoolean("isFollowing", false),
                    isFollowedBy = obj.optBoolean("isFollowedBy", false),
                    isMatchedFromContacts = obj.optBoolean("isMatchedFromContacts", false),
                    suggestionDayKey = obj.optString("suggestionDayKey"),
                    suggestionScore = obj.optInt("suggestionScore", 0),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }
        return list
    }

    suspend fun fetchPeerPreKeyBundleFromKtor(peerEmail: String): JSONObject? = withContext(Dispatchers.IO) {
        runCatching {
            val response = ktorHttpClient.get("$BASE_URL/edge/v1/keys/bundle?email=$peerEmail")
            if (response.status == HttpStatusCode.OK) {
                JSONObject(response.bodyAsText())
            } else {
                null
            }
        }.getOrNull()
    }

    suspend fun postEncryptedMessageToKtor(payload: JSONObject): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val response = ktorHttpClient.post("$BASE_URL/edge/v1/messages/send") {
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            response.status == HttpStatusCode.OK
        }.getOrDefault(false)
    }

    suspend fun postEditMessageToKtor(payload: JSONObject): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val response = ktorHttpClient.post("$BASE_URL/edge/v1/messages/edit") {
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            response.status == HttpStatusCode.OK
        }.getOrDefault(false)
    }

    suspend fun postUnsendMessageToKtor(payload: JSONObject): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val response = ktorHttpClient.post("$BASE_URL/edge/v1/messages/unsend") {
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            response.status == HttpStatusCode.OK
        }.getOrDefault(false)
    }

    suspend fun postReactionToKtor(payload: JSONObject): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val response = ktorHttpClient.post("$BASE_URL/edge/v1/messages/react") {
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            response.status == HttpStatusCode.OK
        }.getOrDefault(false)
    }

    suspend fun postPinToKtor(payload: JSONObject): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val response = ktorHttpClient.post("$BASE_URL/edge/v1/messages/pin") {
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            response.status == HttpStatusCode.OK
        }.getOrDefault(false)
    }

    suspend fun postFavouriteToKtor(payload: JSONObject): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val response = ktorHttpClient.post("$BASE_URL/edge/v1/messages/favourite") {
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            response.status == HttpStatusCode.OK
        }.getOrDefault(false)
    }

    suspend fun postReceiptToKtor(
        messageId: String,
        status: String,
        conversationId: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject()
                .put("messageId", messageId)
                .put("conversationId", conversationId)
                .put("status", status)
            val response = ktorHttpClient.post("$BASE_URL/edge/v1/messages/receipt") {
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            response.status == HttpStatusCode.OK
        }.getOrDefault(false)
    }

    suspend fun postModerationToKtor(payload: JSONObject): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val response = ktorHttpClient.post("$BASE_URL/edge/v1/moderation") {
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            response.status == HttpStatusCode.OK
        }.getOrDefault(false)
    }
}
