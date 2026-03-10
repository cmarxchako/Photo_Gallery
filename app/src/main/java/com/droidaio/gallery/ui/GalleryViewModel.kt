package com.droidaio.gallery.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.droidaio.gallery.MediaRepository
import com.droidaio.gallery.models.MediaItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds

    private val pendingDeleteJobs = mutableMapOf<Long, Job>()

    private val repository by lazy { MediaRepository(getApplication()) }

    companion object {
        private const val UNDO_DELAY_MS = 5000L
    }

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            try {
                val list = repository.queryAllMedia()
                _items.value = list
                val cur = _selectedIds.value
                if (cur.isNotEmpty()) {
                    val valid = cur.intersect(list.map { it.id }.toSet())
                    if (valid.size != cur.size) _selectedIds.value = valid
                }
            } catch (e: Exception) {
                _items.value = emptyList()
                e.printStackTrace()
            }
        }
    }

    fun toggleSelection(itemId: Long) {
        val cur = _selectedIds.value.toMutableSet()
        if (cur.contains(itemId)) cur.remove(itemId) else cur.add(itemId)
        _selectedIds.value = cur
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun selectAll() {
        _selectedIds.value = _items.value.map { it.id }.toSet()
    }

    fun invertSelection() {
        val all = _items.value.map { it.id }.toSet()
        val cur = _selectedIds.value
        _selectedIds.value = all.filter { !cur.contains(it) }.toSet()
    }

    fun getSelectedItems(): List<MediaItem> {
        val ids = _selectedIds.value
        return _items.value.filter { ids.contains(it.id) }
    }

    fun scheduleDeleteWithUndo(itemsToDelete: List<MediaItem>) {
        // remove from UI immediately
        _items.value = _items.value.filter { it.id !in itemsToDelete.map { it.id } }
        _selectedIds.value = emptySet()

        /** Schedule deletion jobs and post undoable snackbar
         * via AppEventBus
         */
        val opId = java.util.UUID.randomUUID()
        AppEventBus.tryPost(
            AppEventBus.UiEvent.ShowUndoableSnackbar(
                id = opId,
                message = "Deleted ${itemsToDelete.size} items"
            )
        )

        itemsToDelete.forEach { item ->
            val job = viewModelScope.launch {
                try {
                    delay(UNDO_DELAY_MS)
                    com.droidaio.gallery.FileOperations.deleteMedia(getApplication(), listOf(item))
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingDeleteJobs.remove(item.id)
                }
            }
            pendingDeleteJobs[item.id] = job
        }
    }

    fun cancelDelete(itemIds: List<Long>) {
        itemIds.forEach { id -> pendingDeleteJobs.remove(id)?.cancel() }
        loadMedia()
    }

    fun performImmediateDelete(itemsToDelete: List<MediaItem>) {
        viewModelScope.launch {
            try {
                com.droidaio.gallery.FileOperations.deleteMedia(getApplication(), itemsToDelete)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loadMedia()
            }
        }
    }
}

