package com.telefam.company.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.ContactsContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TelefamContactsFilter(val label: String) {
    ALL("People you may know"),
    FOLLOWERS("Followers"),
    FOLLOWING("Following"),
    FRIENDS("Friends")
}

data class FollowRateLimitCheck(
    val allowed: Boolean,
    val retryAfterSeconds: Int = 0,
    val errorMessage: String? = null
)

class TelefamComContactsManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("telefam_contacts_social_prefs", Context.MODE_PRIVATE)

    fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isContactsSyncEnabledByUser(): Boolean {
        return prefs.getBoolean("contacts_sync_allowed", false) && hasContactsPermission()
    }

    fun markContactsSyncAllowed(allowed: Boolean) {
        prefs.edit().putBoolean("contacts_sync_allowed", allowed).apply()
    }

    fun getLastUploadedContactHashes(): Set<String> {
        return prefs.getStringSet("uploaded_contact_hashes", emptySet()) ?: emptySet()
    }

    fun saveUploadedContactHashes(hashes: Set<String>) {
        prefs.edit().putStringSet("uploaded_contact_hashes", hashes).apply()
    }

    fun isNetworkAvailable(): Boolean {
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.getOrDefault(true)
    }

    /**
     * Sliding 60-second window rate limiter for Follow / Unfollow actions.
     * Max 8 follow/unfollow actions per 60 seconds.
     */
    fun checkAndRecordFollowActionRateLimit(): FollowRateLimitCheck {
        val now = System.currentTimeMillis()
        val windowMs = 60_000L
        val maxActionsPerMinute = 8

        val rawHistory = prefs.getString("follow_rate_timestamps", "").orEmpty()
        val validTimestamps = rawHistory
            .split(",")
            .mapNotNull { it.toLongOrNull() }
            .filter { now - it < windowMs }
            .toMutableList()

        if (validTimestamps.size >= maxActionsPerMinute) {
            val oldest = validTimestamps.minOrNull() ?: now
            val waitSeconds = (((windowMs - (now - oldest)) / 1000L).toInt()).coerceAtLeast(1)
            return FollowRateLimitCheck(
                allowed = false,
                retryAfterSeconds = waitSeconds,
                errorMessage = "Rate limit reached. Please try again later (${waitSeconds}s)."
            )
        }

        validTimestamps.add(now)
        prefs.edit()
            .putString("follow_rate_timestamps", validTimestamps.joinToString(","))
            .apply()

        return FollowRateLimitCheck(allowed = true)
    }

    /**
     * Reads phone numbers and email addresses from the Android ContactsProvider
     * and returns only their irreversible SHA-256 hex hashes for privacy-preserving
     * server contact matching.
     */
    suspend fun readAndHashDeviceContacts(): Set<String> = withContext(Dispatchers.IO) {
        if (!hasContactsPermission()) return@withContext getLastUploadedContactHashes()

        val hashes = mutableSetOf<String>()
        val resolver = context.contentResolver

        // 1. Read and hash phone numbers
        runCatching {
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null,
                null,
                null
            )?.use { cursor ->
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    if (numberIndex >= 0) {
                        val rawPhone = cursor.getString(numberIndex).orEmpty()
                        val digits = rawPhone.filter { it.isDigit() }
                        if (digits.length >= 7) {
                            hashes.add(sha256Hex("phone:$digits"))
                            val tail9 = digits.takeLast(9)
                            hashes.add(sha256Hex("phone_tail:$tail9"))
                        }
                    }
                }
            }
        }

        // 2. Read and hash contact emails
        runCatching {
            resolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                null,
                null,
                null
            )?.use { cursor ->
                val emailIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                while (cursor.moveToNext()) {
                    if (emailIndex >= 0) {
                        val rawEmail = cursor.getString(emailIndex).orEmpty().trim().lowercase(Locale.US)
                        if (rawEmail.contains("@")) {
                            hashes.add(sha256Hex("email:$rawEmail"))
                        }
                    }
                }
            }
        }

        if (hashes.isNotEmpty()) {
            saveUploadedContactHashes(hashes)
        }
        hashes
    }

    companion object {
        fun currentDayKey(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }

        fun sha256Hex(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(input.trim().lowercase(Locale.US).toByteArray(Charsets.UTF_8))
            val sb = StringBuilder(bytes.size * 2)
            for (b in bytes) {
                sb.append(String.format(Locale.US, "%02x", b))
            }
            return sb.toString()
        }

        fun computePhoneHashes(dialCode: String, phoneDigitsRaw: String): Pair<String, String> {
            val cleanDial = dialCode.filter { it.isDigit() }
            val cleanPhone = phoneDigitsRaw.filter { it.isDigit() }.trimStart('0')
            if (cleanPhone.isEmpty()) return "" to ""
            val fullDigits = "$cleanDial$cleanPhone"
            val tail9 = fullDigits.takeLast(9)
            return sha256Hex("phone:$fullDigits") to sha256Hex("phone_tail:$tail9")
        }

        fun computeEmailHash(email: String): String {
            val normalized = email.trim().lowercase(Locale.US)
            if (normalized.isEmpty()) return ""
            return sha256Hex("email:$normalized")
        }
    }
}

@Composable
fun rememberOfflineProfileImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember(context) {
        ImageLoader.Builder(context)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, "telefam_profile_photos_offline_cache"))
                    .maxSizeBytes(80L * 1024L * 1024L) // 80 MB persistent offline avatar cache
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
