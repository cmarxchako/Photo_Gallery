package com.droidaio.gallery.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.droidaio.gallery.models.MediaItem

/**
 * Composable representing a single media item in the grid. For simplicity,
 * this example uses a placeholder image. In a real implementation, you would load
 * the actual media thumbnail using an image loading library like Coil or Glide.
 * The onClick callback is invoked when the user taps on the item, allowing you to
 * navigate to a detail view or perform other actions.
 * @param item The MediaItem data to display.
 * @param onClick Callback invoked when the item is clicked.
 */
@Composable
fun MediaItemComposable(item : MediaItem, onClick : () -> Unit) {
    Card(modifier = Modifier
        .padding(4.dp)
        .aspectRatio(1f)
        .clickable { onClick() }) {
        Image(painter = painterResource(id = android.R.drawable.ic_menu_gallery), contentDescription = item.displayName, modifier = Modifier)
    }
}

