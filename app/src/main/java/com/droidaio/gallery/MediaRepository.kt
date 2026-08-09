package com.droidaio.gallery

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.database.getStringOrNull
import com.droidaio.gallery.models.FolderInfo
import com.droidaio.gallery.models.MediaItem

/**
 * Repository class responsible for querying media items from the device's MediaStore. Provides a method to
 * retrieve all images and videos with their metadata. This class abstracts the data access layer and can be
 * extended in the future to support additional media sources or filtering logic. The queryAllMedia() method
 * performs a single query to the MediaStore, filtering for images and videos, and returns a list of MediaItem
 * objects containing relevant metadata such as URI, name, MIME type, date, and size. The implementation
 * handles differences in MediaStore columns based on Android version and ensures proper resource management
 * by using the cursor's use() function. This class can be easily tested by mocking the Context and ContentResolver
 * to verify that the query is constructed correctly and that the returned MediaItem list matches expected results
 * based on the cursor data. Future enhancements could include methods for querying media by folder, date range, or
 * other criteria, as well as support for pagination to handle large media libraries efficiently. Overall, this
 * MediaRepository class serves as a clean and maintainable way to access media data within the app, keeping the data
 * retrieval logic separate from the UI and business logic layers.
 *
 * Usage example:
 * val repository = MediaRepository(context)
 * val mediaItems = repository.queryAllMedia()
 * mediaItems.forEach {item -> println("Media item: ${item.name}, URI: ${item.uri}, MIME type: ${item.mimeType}, date: ${item.date}, size: ${item.size}") }
 */
class MediaRepository(private val context: Context) {

    fun queryAllMedia(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.DATE_TAKEN,
                MediaStore.MediaColumns.SIZE
            )
        } else {
            arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.SIZE
            )
        }

        val selection =
            (MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    + " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
        val queryUri = MediaStore.Files.getContentUri("external")
        val sort = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.MediaColumns.DATE_TAKEN} DESC"
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            } else {
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            }
        }
        val cursor: Cursor? =
            context.contentResolver.query(queryUri, projection, selection, null, sort)
        cursor?.use {
            val idIdx = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val mediaTypeIdx = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val nameIdx = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeIdx = it.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val dateIdx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            } else {
                it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            }
            val sizeIdx = it.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            while (it.moveToNext()) {
                val id = it.getLong(idIdx)
                val mediaType = it.getInt(mediaTypeIdx)
                val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val contentUri =
                    if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val uri = ContentUris.withAppendedId(contentUri, id)
                val name = it.getStringOrNull(nameIdx)
                val mime = it.getStringOrNull(mimeIdx)
                val date = if (!it.isNull(dateIdx)) it.getLong(dateIdx) else null
                val size = if (!it.isNull(sizeIdx)) it.getLong(sizeIdx) else null
                items.add(MediaItem(id, uri, name, mime, date, size, isVideo))
            }
        }
        return items
    }

    fun queryFolders(): List<FolderInfo> {
        val folders = mutableListOf<FolderInfo>()

        // Projection: bucket id and bucket display name (supported by MediaStore)
        val projection = arrayOf(
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns._ID
        )

        // Query both images and videos
        val selection =
            (MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    + " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)

        val queryUri = MediaStore.Files.getContentUri("external")
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        val cursor = context.contentResolver.query(queryUri, projection, selection, null, sortOrder)
        cursor?.use { c ->
            val bucketIdIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)

            // Use a map to aggregate counts and sample URIs per bucket
            val map = linkedMapOf<String, FolderInfoBuilder>()

            while (c.moveToNext()) {
                val bucketId = c.getStringOrNull(bucketIdIdx) ?: continue
                val bucketName = c.getStringOrNull(bucketNameIdx) ?: "Unknown"
                val sampleId = c.getLong(idIdx)
                val sampleUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, sampleId
                )

                val builder = map.getOrPut(bucketId) {
                    FolderInfoBuilder(bucketId, bucketName)
                }
                builder.incrementCount()
                builder.maybeSetSampleUri(sampleUri)
            }

            // Convert builders to FolderInfo list
            map.values.forEach { folders.add(it.build()) }
        }

        return folders
    }

    // Add inside MediaRepository (private helper)
    private class FolderInfoBuilder(val bucketId: String, val bucketName: String) {
        private var count = 0
        private var sampleUri: Uri? = null

        fun incrementCount() {
            count++
        }

        fun maybeSetSampleUri(uri: Uri) {
            if (sampleUri == null) sampleUri = uri
        }

        fun build(): FolderInfo {
            return FolderInfo(
                id = bucketId.hashCode().toLong(),
                bucketId = bucketId,
                bucketName = bucketName,
                sampleUri = sampleUri,
                itemCount = count
            )
        }
    }
}

