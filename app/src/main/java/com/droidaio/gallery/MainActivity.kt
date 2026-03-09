package com.droidaio.gallery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.droidaio.gallery.models.MediaItem
import com.droidaio.gallery.ui.AppEventBus
import com.droidaio.gallery.ui.AppNavGraph
import com.droidaio.gallery.ui.theme.PhotoGalleryTheme
import com.google.android.material.color.DynamicColors
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * MainActivity - app entry.
 *  - Manages pending move/copy operations with undo support using WorkManager
 *    and a simple persistence layer.
 *  - Provides APIs for Compose UI to start folder picker and cancel pending ops.
 *  - Handles dynamic color theming with Material You support.
 *  - MSAL initialization is handled in GalleryApp (Application.onCreate) to ensure
 *    it's available app-wide without tight coupling to the Activity lifecycle.
 */
class MainActivity : AppCompatActivity() {

    enum class PendingOp { NONE, COPY, MOVE }

    private lateinit var requestPermissionsLauncher : ActivityResultLauncher<Array<String>>
    private lateinit var folderPickerLauncher : ActivityResultLauncher<Intent>

    // Pending items to move/copy (set by Compose UI via MainActivity)
    @Volatile
    private var pendingItemsForOp : List<MediaItem> = emptyList()

    // Persistence store
    private lateinit var opStore : PendingOpStore

    // WorkManager
    private val workManager by lazy { WorkManager.getInstance(applicationContext) }
    private val gson = Gson()

    // Coroutine scope for lifecycle-tied tasks
    private val mainScope = CoroutineScope(Dispatchers.Main)

    companion object {
        private const val OP_UNDO_DELAY_MS = 5000L
        private const val WORK_INPUT_KEY = "pending_op_json"
    }

    override fun onCreate(savedInstanceState : Bundle?) {
        // Apply dynamic colors using the Application instance (fixes type mismatch)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivitiesIfAvailable(application)
        }

        ThemeManager.applySavedTheme(applicationContext)
        val choice = ThemeManager.getSavedTheme(applicationContext)
        if (choice == ThemeManager.ThemeChoice.PURE_BLACK) {
            setTheme(R.style.Theme_PhotoGallery_PureBlack)
        } else {
            setTheme(R.style.Theme_PhotoGallery_Default)
        }

        super.onCreate(savedInstanceState)

        opStore = PendingOpStore(applicationContext)

        // MSAL initialization is handled in GalleryApp (Application.onCreate).
        // Rely on GalleryApp.msalApp where needed; do not call PublicClientApplication constructor here.

        requestPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

        folderPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (data != null && result.resultCode == RESULT_OK) {
                val treeUri : Uri? = data.data
                if (treeUri != null) {
                    contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    onFolderPicked(treeUri)
                }
            }
        }

        requestNecessaryPermissions()

        // Restore persisted pending ops and re-schedule any PENDING ones
        restorePendingOperations()

        setContent {
            val usePureBlack = (ThemeManager.getSavedTheme(applicationContext) == ThemeManager.ThemeChoice.PURE_BLACK)
            PhotoGalleryTheme(
                usePureBlack = usePureBlack,
                darkTheme = (ThemeManager.getSavedTheme(applicationContext) == ThemeManager.ThemeChoice.DARK || usePureBlack)
            ) {
                val navController = rememberNavController()
                AppNavGraph(navController)
            }
        }
    }

    private fun requestNecessaryPermissions() {
        val perms = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (perms.isNotEmpty()) requestPermissionsLauncher.launch(perms.toTypedArray())
    }

    /**
     * Called by Compose UI to start a folder picker for a move or copy operation.
     * Compose should set pendingItemsForOp and also set a transient opType via startFolderPickerForOperationWithType.
     */
    fun startFolderPickerForOperationWithType(opType : PendingOperation.Type, items : List<MediaItem>) {
        // store pending items temporarily until folder is picked
        pendingItemsForOp = items
        transientOpType = opType
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        folderPickerLauncher.launch(intent)
    }

    // transient op type set by Compose before launching folder picker
    @Volatile
    private var transientOpType : PendingOperation.Type? = null

    private fun onFolderPicked(treeUri : Uri) {
        val items = pendingItemsForOp
        val opType = transientOpType ?: PendingOperation.Type.COPY
        // reset transient state
        pendingItemsForOp = emptyList()
        transientOpType = null
        if (items.isEmpty()) return

        val itemUris = items.mapNotNull { it.uri.toString() }
        val itemIds = items.map { it.id }

        val op = PendingOperation(
            type = when (opType) {
                PendingOperation.Type.COPY -> PendingOperation.Type.COPY
                PendingOperation.Type.MOVE -> PendingOperation.Type.MOVE
                else -> PendingOperation.Type.COPY
            },
            itemIds = itemIds,
            itemUris = itemUris,
            targetTreeUri = treeUri.toString(),
            createdAt = System.currentTimeMillis(),
            status = PendingOperation.Status.PENDING
        )

        // persist op
        opStore.add(op)

        // prepare WorkRequest with initial delay = undo window
        val json = gson.toJson(op)
        val input = Data.Builder().putString(WORK_INPUT_KEY, json).build()

        val work = OneTimeWorkRequestBuilder<OperationWorker>()
            .setInputData(input)
            .setInitialDelay(OP_UNDO_DELAY_MS, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10_000, TimeUnit.MILLISECONDS)
            .build()

        // enqueue unique work so we can cancel by op id
        val uniqueName = "op_${op.id}"
        workManager.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, work)

        // notify Compose UI
        AppEventBus.tryPost(AppEventBus.UiEvent.ShowUndoableSnackbar(id = op.id, message = "${op.type.name} scheduled"))
        AppEventBus.tryPost(AppEventBus.UiEvent.ShowProgress(id = op.id, message = "${op.type.name} scheduled"))
    }

    /**
     * Cancel scheduled op (called by Compose when user taps Undo).
     */
    fun cancelScheduledOp(opId : UUID) {
        val uniqueName = "op_$opId"
        workManager.cancelUniqueWork(uniqueName)
        // update persisted op status
        val list = opStore.loadAll()
        val idx = list.indexOfFirst { it.id == opId }
        if (idx >= 0) {
            val op = list[idx]
            op.status = PendingOperation.Status.CANCELLED
            opStore.update(op)
        }
        AppEventBus.tryPost(AppEventBus.UiEvent.HideProgress(id = opId))
        AppEventBus.tryPost(AppEventBus.UiEvent.ShowSnackbar(id = opId, message = "Operation cancelled"))
    }

    /**
     * Return persisted operations for the history UI.
     */
    fun getPersistedOperations() : List<PendingOperation> = opStore.loadAll().sortedByDescending { it.createdAt }

    /**
     * Restore persisted PENDING operations on startup and re-enqueue them with WorkManager.
     */
    private fun restorePendingOperations() {
        val list = opStore.loadAll()
        list.filter { it.status == PendingOperation.Status.PENDING || it.status == PendingOperation.Status.SCHEDULED }.forEach { op ->
            // re-enqueue work with remaining attempts info
            val json = gson.toJson(op)
            val input = Data.Builder().putString(WORK_INPUT_KEY, json).build()
            val work = OneTimeWorkRequestBuilder<OperationWorker>()
                .setInputData(input)
                .setInitialDelay(OP_UNDO_DELAY_MS, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10_000, TimeUnit.MILLISECONDS)
                .build()
            val uniqueName = "op_${op.id}"
            workManager.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.KEEP, work)
            // mark as scheduled
            op.status = PendingOperation.Status.SCHEDULED
            opStore.update(op)
            AppEventBus.tryPost(AppEventBus.UiEvent.ShowProgress(id = op.id, message = "${op.type.name} resuming"))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }
}

