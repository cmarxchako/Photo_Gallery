package com.droidaio.gallery

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.droidaio.gallery.models.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

/**
 * Helper object for file operations (delete, copy, move) with progress tracking.
 * This object provides functions to perform delete, copy, and move operations on media items
 * with progress callbacks.
 * - deleteMedia function tries to delete each item via the content resolver first, then falls
 *   back to DocumentFile if needed.
 * - copyToUriWithProgress and moveToUriWithProgress functions perform copy/move operations to
 *   a destination tree URI, using OpenableColumns.SIZE when available for better progress tracking.
 * - copyStreamWithProgress function handles copying data from an input stream to an output stream
 *   while emitting progress updates based on the total size when known, or coarse updates when size
 *   is unknown.
 *
 * Note: the legacy functions (deleteMedia, copyToFolderWithPicker, moveToFolderWithPicker, copyToUri, moveToUri)
 * are still available for simpler use cases without progress tracking, while the new functions provide more
 * detailed progress updates for better user feedback during long operations.
 * This design allows us to have both simple and progress-aware file operations in the project, depending on
 * the needs of the caller. All functions are suspend functions that should be called from a coroutine context,
 * and they perform their work on the IO dispatcher to avoid blocking the main thread. Error handling is basic
 * (catching exceptions and printing stack traces), but could be enhanced to provide more detailed error
 * reporting via the progress callback or by throwing exceptions to the caller. The progress callback provides
 * the item index and percent complete for copy/move operations, but for delete operations it only provides the
 * item index since delete is usually fast and doesn't have byte-level progress.
 *
 *  - copyStreamWithProgress emits progress updates based on the total size when known, but if the size is unknown
 *   it emits coarse updates (e.g. every 256KB) to provide some feedback without being linear.
 * - querySizeForUri attempts to get the size of a URI using OpenableColumns.SIZE, and falls back to
 *   queryAssetFileDescriptorLength and if that fails, to provide better progress tracking for copy operations.
 * - queryAssetFileDescriptorLength tries to get the length of a URI by opening an asset file descriptor, which
 *   can work for certain types of URIs that support it, as a fallback when OpenableColumns.SIZE is not available.
 * - deleteMedia does not provide per-item percent progress since delete is usually fast, but it could be extended
 *   to do so if desired (e.g. by emitting 100% after each item is deleted).
 * - copyToFolderWithPicker and moveToFolderWithPicker functions simply launch a folder picker intent, and the
 *   actual copy/move logic (including progress updates) is expected to be handled in the caller's
 *   onActivityResult after obtaining the tree URI, to allow for better separation of concerns and flexibility in
 *   how the copy/move is performed.
 * - copyToUri and moveToUri functions are legacy implementations that perform simple copy/move operations without
 *   progress tracking, and they can still be used for simpler cases where progress updates are not needed.
 *
 * Overall, this FileOperations object provides a set of helper functions for performing file operations on media
 * items with varying levels of progress tracking, allowing the caller to choose the appropriate function based on
 * their needs for user feedback during long operations.
 */
object FileOperations {

    // Existing simple helpers remain available elsewhere in the project.
    // Below are progress-aware implementations that use OpenableColumns.SIZE
    // when available to provide more accurate per-item progress.

    suspend fun deleteMediaWithProgress(context : Context, items : List<MediaItem>, progress : (itemIndex : Int, percent : Int) -> Unit) {
        withContext(Dispatchers.IO) {
            items.forEachIndexed { idx, item ->
                try {
                    context.contentResolver.delete(item.uri, null, null)
                    progress(idx, 100)
                } catch (e : Exception) {
                    val doc = DocumentFile.fromSingleUri(context, item.uri)
                    doc?.delete()
                    progress(idx, 100)
                }
            }
        }
    }

    suspend fun copyToUriWithProgress(context : Context, treeUri : Uri, items : List<MediaItem>, progress : (itemIndex : Int, percent : Int) -> Unit) {
        withContext(Dispatchers.IO) {
            val docTree = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext
            items.forEachIndexed { idx, item ->
                try {
                    val name = item.displayName ?: "file"
                    val mime = item.mimeType ?: "application/octet-stream"
                    val dest = docTree.createFile(mime, name) ?: return@forEachIndexed

                    /* Determine size using OpenableColumns.SIZE if possible for better progress
                    * tracking, otherwise fall back to asset file descriptor length
                    */
                    val totalSize = querySizeForUri(context.contentResolver, item.uri).let { size ->
                        if (size > 0) size else queryAssetFileDescriptorLength(context.contentResolver, item.uri)
                    }

                    context.contentResolver.openInputStream(item.uri).use { input ->
                        context.contentResolver.openOutputStream(dest.uri).use { out ->
                            if (input != null && out != null) {
                                copyStreamWithProgress(input, out, totalSize) { percent -> progress(idx, percent) }
                            } else {
                                progress(idx, 100)
                            }
                        }
                    }
                } catch (e : Exception) {
                    e.printStackTrace()
                    progress(idx, 100)
                }
            }
        }
    }

    suspend fun moveToUriWithProgress(context : Context, treeUri : Uri, items : List<MediaItem>, progress : (itemIndex : Int, percent : Int) -> Unit) {
        withContext(Dispatchers.IO) {
            // Move = copy then delete
            copyToUriWithProgress(context, treeUri, items, progress)
            deleteMediaWithProgress(context, items) { idx, _ -> /* no per-item percent for delete */ }
        }
    }

    private fun querySizeForUri(resolver : ContentResolver, uri : Uri?) : Long {
        if (uri == null) return -1L
        var cursor : Cursor? = null
        return try {
            cursor = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0) {
                    val size = cursor.getLong(sizeIndex)
                    if (size > 0) size else -1L
                } else {
                    -1L
                }
            } else {
                -1L
            }
        } catch (_ : Exception) {
            -1L
        } finally {
            cursor?.close()
        }
    }

    private fun queryAssetFileDescriptorLength(resolver : ContentResolver, uri : Uri?) : Long {
        if (uri == null) return -1L
        return try {
            resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                val length = afd.length
                if (length > 0) length else -1L
            } ?: -1L
        } catch (_ : Exception) {
            -1L
        }
    }

    private fun copyStreamWithProgress(input : InputStream, out : OutputStream, totalSize : Long, progressCb : (percent : Int) -> Unit) {
        val buffer = ByteArray(8 * 1024)
        var total = 0L
        var read : Int
        var lastPercent = -1
        if (totalSize > 0) {
            while (true) {
                read = input.read(buffer)
                if (read <= 0) break
                out.write(buffer, 0, read)
                total += read
                val percent = ((total * 100) / totalSize).toInt().coerceIn(0, 100)
                if (percent != lastPercent) {
                    lastPercent = percent
                    progressCb(percent)
                }
            }
            progressCb(100)
        } else {
            // Unknown total size: copy and emit coarse progress updates
            var bytesSinceLast = 0L
            val threshold = 256 * 1024 // 256KB increments
            while (true) {
                read = input.read(buffer)
                if (read <= 0) break
                out.write(buffer, 0, read)
                bytesSinceLast += read
                if (bytesSinceLast >= threshold) {
                    // emit a pseudo-progress value (will be non-linear)
                    progressCb(50)
                    bytesSinceLast = 0
                }
            }
            progressCb(100)
        }
    }

    /**
     * Legacy delete that tries content resolver first, then falls back to DocumentFile if needed.
     * Helper to delete media items by URI. Tries content resolver delete first, then falls back to
     * DocumentFile delete if content resolver fails (e.g. for certain URIs). This provides a more
     * robust delete operation across different URI types. Progress is reported per item, but not
     * per byte since delete is usually fast and doesn't provide byte-level progress.
     * Note: this does not provide per-item progress since delete is usually fast,
     * but could be extended similarly to copy if desired.
     */
    suspend fun deleteMedia(context : Context, items : List<MediaItem>) {
        withContext(Dispatchers.IO) {
            items.forEach { item ->
                try {
                    context.contentResolver.delete(item.uri, null, null)
                } catch (e : Exception) {
                    // fallback: try to delete via DocumentFile if possible
                    val doc = DocumentFile.fromSingleUri(context, item.uri)
                    doc?.delete()
                }
            }
        }
    }

    /** Legacy copy/move that launches a folder picker, then expects the caller to handle the
     * result in onActivityResult and call copyToUri/moveToUri accordingly.
     * Helper to initiate copy to a folder by launching a folder picker. The actual copy will be
     * performed in the caller's onActivityResult after obtaining the tree URI. This separation allows
     * the caller to handle the copy logic (including progress updates) after folder selection.
     * Note: these do not provide progress updates since the actual copy/move happens in the
     * caller after folder selection.
     */
    suspend fun copyToFolderWithPicker(activity : Activity, items : List<MediaItem>) {
        withContext(Dispatchers.Main) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            activity.startActivityForResult(intent, COPYPICKERREQUEST)
            // The actual copy will be triggered in activity's onActivityResult by calling copyToUri
        }
    }

    /** Legacy move that launches a folder picker, then expects the caller to handle the
     * result in onActivityResult and call moveToUri accordingly.
     * Helper to initiate move to a folder by launching a folder picker. The actual move will be
     * performed in the caller's onActivityResult after obtaining the tree URI. This separation allows
     * the caller to handle the move logic (including progress updates) after folder selection.
     * Note: this does not provide progress updates since the actual move happens in the
     * caller after folder selection.
     */
    suspend fun moveToFolderWithPicker(activity : Activity, items : List<MediaItem>) {
        withContext(Dispatchers.Main) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            activity.startActivityForResult(intent, MOVEPICKERREQUEST)
            // The actual move will be triggered in activity's onActivityResult by calling moveToUri
        }
    }

    /** Legacy copy that expects the caller to have already obtained a tree URI
     * (e.g. via copyToFolderWithPicker).
     * Helper to perform copy to a DocumentFile tree URI by copying each item.
     * Note: this does not provide progress updates since it's a simple copy
     * without per-item tracking.
     */
    suspend fun copyToUri(context : Context, treeUri : Uri, items : List<MediaItem>) {
        withContext(Dispatchers.IO) {
            val docTree = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext
            items.forEach { item ->
                try {
                    val name = item.displayName ?: "file"
                    val mime = item.mimeType ?: "application/octet-stream"
                    val dest = docTree.createFile(mime, name) ?: return@forEach
                    context.contentResolver.openInputStream(item.uri).use { input ->
                        context.contentResolver.openOutputStream(dest.uri).use { out ->
                            if (input != null && out != null) {
                                input.copyTo(out)
                            }
                        }
                    }
                } catch (_ : Exception) {
                }
            }
        }
    }

    /** Legacy move that expects the caller to have already obtained a tree URI
     * (e.g. via moveToFolderWithPicker).
     * Helper to perform move to a DocumentFile tree URI by copying then deleting originals.
     * Note: this does not provide progress updates since it's a simple move
     * without per-item tracking.
     */
    suspend fun moveToUri(context : Context, treeUri : Uri, items : List<MediaItem>) {
        copyToUri(context, treeUri, items)
        deleteMedia(context, items)
    }

    const val COPYPICKERREQUEST = 2001
    const val MOVEPICKERREQUEST = 2002
}

