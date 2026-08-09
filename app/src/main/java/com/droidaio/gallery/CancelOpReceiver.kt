package com.droidaio.gallery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.droidaio.gallery.OperationWorker.Companion.EXTRA_OP_ID
import com.droidaio.gallery.ui.AppEventBus
import java.util.UUID

/**
 * BroadcastReceiver invoked by the notification Cancel action.
 * Cancels the WorkManager unique work for the given op id and updates persisted state.
 */
class CancelOpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val opIdStr = intent.getStringExtra(EXTRA_OP_ID) ?: return
        val opId = try {
            UUID.fromString(opIdStr)
        } catch (_: Exception) {
            return
        }
        val uniqueName = "op_$opId"
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName)

        // update persisted op status
        val store = PendingOpStore(context)
        val list = store.loadAll()
        val idx = list.indexOfFirst { it.id == opId }
        if (idx >= 0) {
            val op = list[idx]
            op.status = PendingOperation.Status.CANCELLED
            store.update(op)
        }

        // Post a UI event via AppEventBus (best-effort)
        AppEventBus.tryPost(AppEventBus.UiEvent.HideProgress(id = opId))
        AppEventBus.tryPost(
            AppEventBus.UiEvent.ShowSnackbar(
                id = opId,
                message = "Operation cancelled"
            )
        )
    }
}

