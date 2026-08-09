package com.droidaio.gallery.models

import android.net.Uri
import java.io.Serializable

/**
 * Represents a media item (photo or video) in the gallery.
 * - id: unique media ID (from MediaStore)
 * - uri: content URI to access the media
 * - displayName: original file name (may be null)
 * - mimeType: MIME type (e.g. "image/jpeg", "video/mp4")
 * - dateTaken: epoch millis when photo/video was taken (may be null)
 * - size: file size in bytes (may be null)
 * - isVideo: true if this item is a video, false if it's a photo
 *
 * This class is Serializable to allow passing in Intents or saving state.
 * Note: we use content URIs (e.g. content://media/external/images/media/123) to access media
 * via ContentResolver, which is more robust than file paths and works with scoped storage.
 * We may need to handle cases where displayName, mimeType, dateTaken, or size are null (e.g.
 * for certain media types or if permissions are limited), so callers should be prepared for
 * that. This class is immutable and only contains basic metadata; any additional info (e.g.
 * thumbnail, selection state) should be managed separately in the UI layer. We can extend this
 * class in the future if we need to store more metadata, but for now we keep it simple.
 * Note: when displaying media items in the gallery, we should load thumbnails efficiently (e.g.
 * using Glide or similar library) rather than trying to load full-size images from the URIs
 * directly.
 */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String?,
    val mimeType: String?,
    val dateTaken: Long?,
    val size: Long?,
    val isVideo: Boolean,
) : Serializable

