package com.droidaio.gallery.ui

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.droidaio.gallery.models.MediaItem
import com.droidaio.gallery.ui.theme.PhotoGalleryTheme

@Composable
fun SelectableMediaItem(
    item: MediaItem,
    selected: Boolean,
    progressPercent: Int?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
            val uri = item.uri
            AsyncImage(
                model = uri,
                contentDescription = item.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
                error = painterResource(id = android.R.drawable.ic_menu_report_image)
            )

            // selection indicator
            val icon =
                if (selected) android.R.drawable.checkbox_on_background else android.R.drawable.checkbox_off_background
            Icon(
                painter = painterResource(id = icon),
                contentDescription = if (selected) "Selected" else "Not selected",
                modifier = Modifier
                    .padding(6.dp)
                    .size(24.dp)
            )

            // per-item overlay (center) when progress is active
            if (progressPercent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = (progressPercent.coerceIn(
                            0,
                            100
                        ) / 100f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "${progressPercent.coerceIn(0, 100)}%",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // bottom thin progress bar (animated and density-safe)
            if (progressPercent != null) {
                val fraction = (progressPercent.coerceIn(0, 100) / 100f)
                val animatedFraction by animateFloatAsState(targetValue = fraction)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(Color.Black.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedFraction)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SelectableMediaItemPreview() {
    PhotoGalleryTheme {
        Row(modifier = Modifier.padding(8.dp)) {
            Box(modifier = Modifier.size(120.dp)) {
                SelectableMediaItem(
                    item = MediaItem(1L, Uri.EMPTY, "IMG_001.jpg", "image/jpeg", null, null, false),
                    selected = false,
                    progressPercent = null,
                    onClick = {},
                    onLongClick = {}
                )
            }
            Box(modifier = Modifier.size(120.dp)) {
                SelectableMediaItem(
                    item = MediaItem(2L, Uri.EMPTY, "IMG_002.jpg", "image/jpeg", null, null, false),
                    selected = true,
                    progressPercent = null,
                    onClick = {},
                    onLongClick = {}
                )
            }
            Box(modifier = Modifier.size(120.dp)) {
                SelectableMediaItem(
                    item = MediaItem(3L, Uri.EMPTY, "IMG_003.jpg", "image/jpeg", null, null, false),
                    selected = false,
                    progressPercent = 45,
                    onClick = {},
                    onLongClick = {}
                )
            }
        }
    }
}
