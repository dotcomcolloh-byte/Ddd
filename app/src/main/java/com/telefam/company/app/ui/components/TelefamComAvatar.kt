package com.telefam.company.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.telefam.company.app.data.rememberOfflineProfileImageLoader

@Composable
fun TelefamComAvatar(photoPath: String, name: String, modifier: Modifier = Modifier) {
    val imageLoader = rememberOfflineProfileImageLoader()
    Surface(
        shape = CircleShape,
        modifier = modifier.size(40.dp)
    ) {
        if (photoPath.isNotBlank()) {
            AsyncImage(
                model = photoPath,
                contentDescription = name,
                imageLoader = imageLoader,
                modifier = Modifier.clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder
            Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    text = name.take(1).uppercase(),
                    modifier = Modifier.wrapContentSize(androidx.compose.ui.Alignment.Center)
                )
            }
        }
    }
}
