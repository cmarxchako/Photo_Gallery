package com.droidaio.gallery

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.droidaio.gallery.PendingOperation.Status
import com.droidaio.gallery.models.MediaItem
import com.droidaio.gallery.ui.AppEventBus
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OperationWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    companion object {
        const val INPUT_KEY = "pending_op_json"
        const val CHANNEL_ID = "photo_gallery_ops"
        const val CHANNEL_NAME = "Photo Gallery Operations"
        const val NOTIF_ID_BASE = 0x1000
        const val ACTION_CANCEL = "com.droidaio.gallery.ACTION_CANCEL_OP"
        const val EXTRA_OP_ID = "extra_op_id"
    }

    private val gson = Gson()
    private val store = PendingOpStore(applicationContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val json = inputData.getString(INPUT_KEY) ?: return@withContext Result.failure()
        val op = try {
            gson.fromJson(json, PendingOperation::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure()
        }

        if (op.status == Status.CANCELLED) {
            store.update(op)
            AppEventBus.tryPost(
                AppEventBus.UiEvent.ShowSnackbar(
                    id = op.id,
                    message = "${op.type.name} cancelled"
                )
            )
            return@withContext Result.success()
        }

        // Set foreground with notification that includes a Cancel action
        setForeground(createForegroundInfo(op))

        op.attempts = op.attempts + 1
        op.status = Status.SCHEDULED
        store.update(op)
        AppEventBus.tryPost(
            AppEventBus.UiEvent.ShowProgress(
                id = op.id,
                message = "${op.type.name} running (attempt ${op.attempts})"
            )
        )

        try {
            val items = op.itemIds.mapIndexed { idx, id ->
                val uriStr = op.itemUris.getOrNull(idx)
                try {
                    uriStr?.let { Uri.parse(it) }
                } catch (_: Exception) {
                    null
                }!!.let {
                    MediaItem(
                        id = id,
                        uri = it,
                        displayName = op.itemNames.getOrNull(idx),
                        mimeType = null,
                        dateTaken = null,
                        size = null,
                        isVideo = false
                    )
                }
            }

            val treeUri = op.targetTreeUri?.let { Uri.parse(it) }

            when (op.type) {
                PendingOperation.Type.COPY -> {
                    if (treeUri == null) throw IllegalStateException("Missing target URI for copy")
                    FileOperations.copyToUriWithProgress(
                        applicationContext,
                        treeUri,
                        items
                    ) { itemIndex, percent ->
                        // emit per-item progress with media id if available
                        val mediaId = items.getOrNull(itemIndex)?.id
                        AppEventBus.tryPost(
                            AppEventBus.UiEvent.ShowItemProgress(
                                opId = op.id,
                                mediaId = mediaId,
                                itemIndex = itemIndex,
                                percent = percent
                            )
                        )
                        AppEventBus.tryPost(
                            AppEventBus.UiEvent.ShowProgress(
                                id = op.id,
                                message = "Copying ${itemIndex + 1}/${items.size}: $percent%"
                            )
                        )
                    }
                }

                PendingOperation.Type.MOVE -> {
                    if (treeUri == null) throw IllegalStateException("Missing target URI for move")
                    FileOperations.moveToUriWithProgress(
                        applicationContext,
                        treeUri,
                        items
                    ) { itemIndex, percent ->
                        val mediaId = items.getOrNull(itemIndex)?.id
                        AppEventBus.tryPost(
                            AppEventBus.UiEvent.ShowItemProgress(
                                opId = op.id,
                                mediaId = mediaId,
                                itemIndex = itemIndex,
                                percent = percent
                            )
                        )
                        AppEventBus.tryPost(
                            AppEventBus.UiEvent.ShowProgress(
                                id = op.id,
                                message = "Moving ${itemIndex + 1}/${items.size}: $percent%"
                            )
                        )
                    }
                }

                PendingOperation.Type.DELETE -> {
                    FileOperations.deleteMediaWithProgress(
                        applicationContext,
                        items
                    ) { itemIndex, _ ->
                        val mediaId = items.getOrNull(itemIndex)?.id
                        AppEventBus.tryPost(
                            AppEventBus.UiEvent.ShowItemProgress(
                                opId = op.id,
                                mediaId = mediaId,
                                itemIndex = itemIndex,
                                percent = 100
                            )
                        )
                        AppEventBus.tryPost(
                            AppEventBus.UiEvent.ShowProgress(
                                id = op.id,
                                message = "Deleting ${itemIndex + 1}/${items.size}"
                            )
                        )
                    }
                }
            }

            op.status = Status.COMPLETED
            op.message = "Completed"
            store.update(op)
            AppEventBus.tryPost(AppEventBus.UiEvent.HideProgress(id = op.id))
            AppEventBus.tryPost(
                AppEventBus.UiEvent.ShowSnackbar(
                    id = op.id,
                    message = "${op.type.name} completed for ${items.size} items"
                )
            )
            return@withContext Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            op.message = e.localizedMessage
            if (op.attempts >= op.maxAttempts) {
                op.status = Status.FAILED
                store.update(op)
                AppEventBus.tryPost(AppEventBus.UiEvent.HideProgress(id = op.id))
                AppEventBus.tryPost(
                    AppEventBus.UiEvent.ShowSnackbar(
                        id = op.id,
                        message = "${op.type.name} failed: ${e.localizedMessage}"
                    )
                )
                return@withContext Result.failure()
            } else {
                store.update(op)
                AppEventBus.tryPost(
                    AppEventBus.UiEvent.ShowSnackbar(
                        id = op.id,
                        message = "${op.type.name} failed, will retry (attempt ${op.attempts})"
                    )
                )
                return@withContext Result.retry()
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createForegroundInfo(op: PendingOperation): ForegroundInfo {
        val notifymgr =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notifychannel =
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            notifymgr.createNotificationChannel(notifychannel)
        }

        val cancelIntent = Intent(applicationContext, CancelOpReceiver::class.java).apply {
            action = ACTION_CANCEL
            putExtra(EXTRA_OP_ID, op.id.toString())
        }
        val cancelPending = PendingIntent.getBroadcast(
            applicationContext,
            op.id.hashCode(),
            cancelIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notify: Notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("${op.type.name} in progress")
            .setContentText("Preparing ${op.type.name.lowercase()} for ${op.itemIds.size} items")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPending)
            .build()

        return ForegroundInfo(NOTIF_ID_BASE + (op.id.hashCode() and 0xFFF), notify)
    }
}

/**
package com.droidaio.gallery

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.droidaio.gallery.PendingOperation.Status
import com.droidaio.gallery.models.MediaItem
import com.droidaio.gallery.ui.AppEventBus
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OperationWorker(appContext : Context, params : WorkerParameters) : CoroutineWorker(appContext, params) {

companion object {
const val INPUT_KEY = "pending_op_json"
const val CHANNEL_ID = "photo_gallery_ops"
const val CHANNEL_NAME = "Photo Gallery Operations"
const val NOTIF_ID_BASE = 0x1000
const val ACTION_CANCEL = "com.droidaio.gallery.ACTION_CANCEL_OP"
const val EXTRA_OP_ID = "extra_op_id"
}

private val gson = Gson()
private val store = PendingOpStore(applicationContext)

override suspend fun doWork() : Result = withContext(Dispatchers.IO) {
val json = inputData.getString(INPUT_KEY) ?: return@withContext Result.failure()
val op = try {
gson.fromJson(json, PendingOperation::class.java)
} catch (e : Exception) {
e.printStackTrace()
return@withContext Result.failure()
}

if (op.status == Status.CANCELLED) {
store.update(op)
AppEventBus.tryPost(AppEventBus.UiEvent.ShowSnackbar(id = op.id, message = "${op.type.name} cancelled"))
return@withContext Result.success()
}

// Set foreground with notification that includes a Cancel action
setForeground(createForegroundInfo(op))

op.attempts = op.attempts + 1
op.status = Status.SCHEDULED
store.update(op)
AppEventBus.tryPost(AppEventBus.UiEvent.ShowProgress(id = op.id, message = "${op.type.name} running (attempt ${op.attempts})"))

try {
val items = op.itemIds.mapIndexed { idx, id ->
val uriStr = op.itemUris.getOrNull(idx)
val uri = try {
uriStr?.let { Uri.parse(it) }
} catch (_ : Exception) {
null
}
if (uri != null) {
MediaItem(id = id, displayName = null, uri = uri, mimeType = null, dateTaken = null, size = null, isVideo = false)
}
}

val treeUri = op.targetTreeUri?.let { Uri.parse(it) }

when (op.type) {
PendingOperation.Type.COPY -> {
if (treeUri == null) throw IllegalStateException("Missing target URI for copy")
FileOperations.copyToUriWithProgress(applicationContext, treeUri, items) { itemIndex, percent ->
// emit per-item progress with media id if available
val mediaId = items.getOrNull(itemIndex)?.id
AppEventBus.tryPost(AppEventBus.UiEvent.ShowItemProgress(opId = op.id, mediaId = mediaId, itemIndex = itemIndex, percent = percent))
AppEventBus.tryPost(AppEventBus.UiEvent.ShowProgress(id = op.id, message = "Copying ${itemIndex + 1}/${items.size}: $percent%"))
}
}
PendingOperation.Type.MOVE -> {
if (treeUri == null) throw IllegalStateException("Missing target URI for move")
FileOperations.moveToUriWithProgress(applicationContext, treeUri, items) { itemIndex, percent ->
val mediaId = items.getOrNull(itemIndex)?.id
AppEventBus.tryPost(AppEventBus.UiEvent.ShowItemProgress(opId = op.id, mediaId = mediaId, itemIndex = itemIndex, percent = percent))
AppEventBus.tryPost(AppEventBus.UiEvent.ShowProgress(id = op.id, message = "Moving ${itemIndex + 1}/${items.size}: $percent%"))
}
}
PendingOperation.Type.DELETE -> {
FileOperations.deleteMediaWithProgress(applicationContext, items) { itemIndex, _ ->
val mediaId = items.getOrNull(itemIndex)?.id
AppEventBus.tryPost(AppEventBus.UiEvent.ShowItemProgress(opId = op.id, mediaId = mediaId, itemIndex = itemIndex, percent = 100))
AppEventBus.tryPost(AppEventBus.UiEvent.ShowProgress(id = op.id, message = "Deleting ${itemIndex + 1}/${items.size}"))
}
}
}

op.status = Status.COMPLETED
op.message = "Completed"
store.update(op)
AppEventBus.tryPost(AppEventBus.UiEvent.HideProgress(id = op.id))
AppEventBus.tryPost(AppEventBus.UiEvent.ShowSnackbar(id = op.id, message = "${op.type.name} completed for ${items.size} items"))
return@withContext Result.success()
} catch (e : Exception) {
e.printStackTrace()
op.message = e.localizedMessage
if (op.attempts >= op.maxAttempts) {
op.status = Status.FAILED
store.update(op)
AppEventBus.tryPost(AppEventBus.UiEvent.HideProgress(id = op.id))
AppEventBus.tryPost(AppEventBus.UiEvent.ShowSnackbar(id = op.id, message = "${op.type.name} failed: ${e.localizedMessage}"))
return@withContext Result.failure()
} else {
store.update(op)
AppEventBus.tryPost(AppEventBus.UiEvent.ShowSnackbar(id = op.id, message = "${op.type.name} failed, will retry (attempt ${op.attempts})"))
return@withContext Result.retry()
}
}
}

@SuppressLint("ObsoleteSdkInt")
private fun createForegroundInfo(op : PendingOperation) : ForegroundInfo {
val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
nm.createNotificationChannel(channel)
}

val cancelIntent = Intent(applicationContext, CancelOpReceiver::class.java).apply {
action = ACTION_CANCEL
putExtra(EXTRA_OP_ID, op.id.toString())
}
val cancelPending = PendingIntent.getBroadcast(
applicationContext,
op.id.hashCode(),
cancelIntent,
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
)

val notif : Notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
.setContentTitle("${op.type.name} in progress")
.setContentText("Preparing ${op.type.name.lowercase()} for ${op.itemIds.size} items")
.setSmallIcon(android.R.drawable.stat_sys_upload)
.setOngoing(true)
.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPending)
.build()

return ForegroundInfo(NOTIF_ID_BASE + (op.id.hashCode() and 0xFFF), notif)
}
}
 */
