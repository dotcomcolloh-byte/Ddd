package com.telefam.company.app.ui.components

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.telefam.company.app.data.TelefamStickerItem
import com.telefam.company.app.ui.theme.LocalTelefamColors

@Composable
fun rememberStickerImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .build()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelefamComStickerBottomSheet(
    stickers: List<TelefamStickerItem>,
    searchQuery: String,
    isLoadingInitial: Boolean,
    isLoadingNextPage: Boolean,
    errorMessage: String?,
    onSearchQueryChanged: (String) -> Unit,
    onLoadNextPage: () -> Unit,
    onRetryLoad: () -> Unit,
    onStickerSendConfirmed: (TelefamStickerItem) -> Unit,
    onDismiss: () -> Unit
) {
    val telefamColors = LocalTelefamColors.current
    val gridState = rememberLazyGridState()
    val imageLoader = rememberStickerImageLoader()

    var previewingSticker by remember { mutableStateOf<TelefamStickerItem?>(null) }

    val shouldFetchMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 6
        }
    }

    LaunchedEffect(shouldFetchMore) {
        if (shouldFetchMore && !isLoadingInitial && !isLoadingNextPage && errorMessage == null) {
            onLoadNextPage()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = telefamColors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.76f)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TelefamComLucideIcons.StickerSmileIcon(
                        tint = telefamColors.brandRed,
                        size = 22.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Stickers",
                        color = telefamColors.textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Stickers",
                        tint = telefamColors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sticker Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                singleLine = true,
                placeholder = {
                    Text(
                        text = "Search stickers (happy, love, wave, cat…)",
                        color = telefamColors.textSecondary.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Stickers",
                        tint = telefamColors.brandRed,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = telefamColors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = telefamColors.inputBackground,
                    unfocusedContainerColor = telefamColors.inputBackground,
                    focusedBorderColor = telefamColors.brandRed,
                    unfocusedBorderColor = telefamColors.inputBorder,
                    focusedTextColor = telefamColors.textPrimary,
                    unfocusedTextColor = telefamColors.textPrimary,
                    cursorColor = telefamColors.brandRed
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sticker_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoadingInitial && stickers.isEmpty() -> {
                    // Skeleton Shimmer Grid during initial load
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(16) {
                            StickerSkeletonBox()
                        }
                    }
                }

                errorMessage != null && stickers.isEmpty() -> {
                    // Error state with Retry button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        TelefamComLucideIcons.StickerSmileIcon(
                            tint = telefamColors.brandRed,
                            size = 44.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage,
                            color = telefamColors.textSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRetryLoad,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = telefamColors.brandRed,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("sticker_retry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Retry", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("sticker_grid")
                    ) {
                        items(
                            items = stickers,
                            key = { it.id }
                        ) { sticker ->
                            Surface(
                                color = telefamColors.inputBackground,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(0.8.dp, telefamColors.inputBorder),
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { previewingSticker = sticker }
                                    .testTag("sticker_item_${sticker.id}")
                            ) {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(sticker.previewUrl)
                                        .crossfade(true)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .build(),
                                    imageLoader = imageLoader,
                                    contentDescription = sticker.title,
                                    contentScale = ContentScale.Fit,
                                    loading = { StickerSkeletonBox() },
                                    error = {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            TelefamComLucideIcons.StickerSmileIcon(
                                                tint = telefamColors.brandRed.copy(alpha = 0.6f),
                                                size = 24.dp
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                )
                            }
                        }

                        if (isLoadingNextPage) {
                            item(span = { GridItemSpan(4) }) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = telefamColors.brandRed,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Sticker Preview Modal before sending
    val selectedSticker = previewingSticker
    if (selectedSticker != null) {
        Dialog(onDismissRequest = { previewingSticker = null }) {
            Surface(
                color = telefamColors.cardBackground,
                shape = RoundedCornerShape(22.dp),
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(22.dp)
                ) {
                    Text(
                        text = "Sticker Preview",
                        color = telefamColors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedSticker.title,
                        color = telefamColors.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = telefamColors.inputBackground,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, telefamColors.inputBorder),
                        modifier = Modifier.size(168.dp)
                    ) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(selectedSticker.fullUrl)
                                .crossfade(true)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build(),
                            imageLoader = imageLoader,
                            contentDescription = selectedSticker.title,
                            contentScale = ContentScale.Fit,
                            loading = { StickerSkeletonBox() },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { previewingSticker = null },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                color = telefamColors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                onStickerSendConfirmed(selectedSticker)
                                previewingSticker = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = telefamColors.brandRed,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("confirm_send_sticker_button")
                        ) {
                            Text(
                                text = "Send Sticker",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StickerSkeletonBox(modifier: Modifier = Modifier) {
    val telefamColors = LocalTelefamColors.current
    val transition = rememberInfiniteTransition(label = "sticker_shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val baseColor = if (telefamColors.isDark) Color(0xFF262A34) else Color(0xFFECEEF2)
    val highlightColor = if (telefamColors.isDark) Color(0xFF383E4C) else Color(0xFFF9FAFB)

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(progress - 300f, progress - 300f),
        end = Offset(progress, progress)
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(brush)
    )
}
