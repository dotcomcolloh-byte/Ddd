package com.telefam.company.app.data

import com.telefam.company.app.backend.TelefamComKtorBackend
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder

data class TelefamStickerItem(
    val id: String,
    val title: String,
    val previewUrl: String,
    val fullUrl: String
)

data class StickerPageResult(
    val items: List<TelefamStickerItem>,
    val nextOffset: Int?,
    val isFromOfflineCache: Boolean = false,
    val errorMessage: String? = null
)

class TelefamComStickerService(
    private val chatDao: TelefamChatDao
) {
    companion object {
        // Publicly available free Tenor v1 sticker API key
        private const val PUBLIC_TENOR_KEY = "LIVDSRZULELA"
        private const val PAGE_LIMIT = 24
    }

    suspend fun fetchStickers(
        rawQuery: String,
        offset: Int = 0
    ): StickerPageResult = withContext(Dispatchers.IO) {
        val cleanQuery = rawQuery.trim().ifBlank { "featured" }
        val encodedQuery = URLEncoder.encode(
            if (cleanQuery == "featured") "cute sticker" else "$cleanQuery sticker",
            "UTF-8"
        )

        val apiUrl = "https://g.tenor.com/v1/search?q=$encodedQuery&key=$PUBLIC_TENOR_KEY&limit=$PAGE_LIMIT&pos=$offset&media_filter=minimal"

        try {
            val response = TelefamComKtorBackend.ktorHttpClient.get(apiUrl)
            if (response.status == HttpStatusCode.OK) {
                val bodyText = response.bodyAsText()
                val root = JSONObject(bodyText)
                val resultsArray = root.optJSONArray("results")
                val parsedItems = mutableListOf<TelefamStickerItem>()

                if (resultsArray != null) {
                    for (i in 0 until resultsArray.length()) {
                        val itemObj = resultsArray.optJSONObject(i) ?: continue
                        val id = itemObj.optString("id").ifBlank { "${cleanQuery}_${offset}_$i" }
                        val title = itemObj.optString("content_description")
                            .ifBlank { itemObj.optString("title", "Sticker") }

                        val mediaArr = itemObj.optJSONArray("media")
                        val firstMedia = mediaArr?.optJSONObject(0)
                        val tinyGifUrl = firstMedia?.optJSONObject("tinygif")?.optString("url").orEmpty()
                        val gifUrl = firstMedia?.optJSONObject("gif")?.optString("url").orEmpty()
                        val preview = tinyGifUrl.ifBlank { gifUrl }
                        val full = gifUrl.ifBlank { tinyGifUrl }

                        if (preview.isNotBlank()) {
                            parsedItems.add(
                                TelefamStickerItem(
                                    id = id,
                                    title = title,
                                    previewUrl = preview,
                                    fullUrl = full
                                )
                            )
                        }
                    }
                }

                if (parsedItems.isNotEmpty()) {
                    // Aggressively cache in Room for offline use
                    val entities = parsedItems.map {
                        TelefamCachedStickerEntity(
                            stickerId = it.id,
                            queryKey = cleanQuery.lowercase(),
                            title = it.title,
                            previewUrl = it.previewUrl,
                            fullUrl = it.fullUrl
                        )
                    }
                    chatDao.upsertCachedStickers(entities)

                    val nextPosStr = root.optString("next", "")
                    val nextOffset = nextPosStr.toIntOrNull() ?: (offset + PAGE_LIMIT)

                    return@withContext StickerPageResult(
                        items = parsedItems,
                        nextOffset = nextOffset,
                        isFromOfflineCache = false
                    )
                }
            }
        } catch (_: Exception) {
            // Network unavailable or API unreachable — fall back to Room cache silently
        }

        // Load from Room offline cache
        val cachedForQuery = chatDao.getCachedStickersByQuery(cleanQuery.lowercase())
        val fallbackList = if (cachedForQuery.isNotEmpty()) {
            cachedForQuery
        } else {
            chatDao.getAllCachedStickers()
        }

        if (fallbackList.isNotEmpty()) {
            return@withContext StickerPageResult(
                items = fallbackList.map {
                    TelefamStickerItem(
                        id = it.stickerId,
                        title = it.title,
                        previewUrl = it.previewUrl,
                        fullUrl = it.fullUrl
                    )
                },
                nextOffset = null,
                isFromOfflineCache = true
            )
        }

        StickerPageResult(
            items = emptyList(),
            nextOffset = null,
            isFromOfflineCache = true,
            errorMessage = "Could not load stickers right now. Tap Retry to try again."
        )
    }
}
