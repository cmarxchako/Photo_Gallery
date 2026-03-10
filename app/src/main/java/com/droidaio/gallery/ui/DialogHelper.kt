package com.droidaio.gallery.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context

/** * Helper for showing dialogs. Provides common patterns like
 * retry/cancel and info dialogs. If context is an Activity,
 * ensures dialog is shown on UI thread.
 */
object DialogHelper {

    /**
     * Show an error dialog with Retry and Cancel.
     * If user chooses Retry, call onRetry.
     */
    fun showRetryDialog(context: Context, title: String, message: String, onRetry: () -> Unit) {
        val act = context as? Activity
        val builder = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(true)
            .setPositiveButton("Retry") { _, _ -> onRetry() }
            .setNegativeButton("Cancel", null)
        act?.runOnUiThread { builder.show() } ?: builder.show()
    }

    /**
     * Show a simple informational dialog.
     * If context is an Activity, show on UI thread.
     * Otherwise, just show it.
     */
    fun showInfo(context: Context, title: String, message: String) {
        val act = context as? Activity
        val builder = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
        act?.runOnUiThread { builder.show() } ?: builder.show()
    }
}

