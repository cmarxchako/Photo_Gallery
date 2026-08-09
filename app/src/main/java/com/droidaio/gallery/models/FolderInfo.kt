package com.droidaio.gallery.models

import android.net.Uri

/**
 * Data class representing information about a media folder (bucket) in the device's MediaStore.
 * Contains the folder's unique ID, bucket ID, bucket name, an optional sample URI for thumbnail display,
 * and the count of items in the folder. This class is used to represent folders in the UI, allowing users
 * to see the folder name, a thumbnail image, and the number of media items contained within it.
 * The sampleUri can be used to load a representative image for the folder, while the itemCount provides context
 * on how many media items are present in that folder. This class can be easily extended in the future to include
 * additional metadata about the folder if needed.
 * Example usage:
 *
 * val folderInfo = FolderInfo(id = 123L, bucketId = "bucket_123", bucketName = "Camera", sampleUri = Uri.parse("content://media/external/images/media/123"), itemCount = 42)
 */
data class FolderInfo(
    val id: Long,
    val bucketId: String?,
    val bucketName: String?,
    val sampleUri: Uri? = null,
    val itemCount: Int = 0,
)

