package com.droidaio.gallery.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.*

/**
 * App-wide UI event bus. Compose collects events and shows
 * Snackbars / Progress UI. Added ShowItemProgress for per-media
 * progress updates.
 */
object AppEventBus {
    sealed class UiEvent {
        data class ShowSnackbar(val id : UUID = UUID.randomUUID(), val message : String, val actionLabel : String? = null, val durationMs : Long = 4000L) : UiEvent()
        data class ShowUndoableSnackbar(val id : UUID = UUID.randomUUID(), val message : String, val actionLabel : String = "Undo", val durationMs : Long = 5000L) : UiEvent()
        data class ShowProgress(val id : UUID = UUID.randomUUID(), val message : String) : UiEvent()
        data class HideProgress(val id : UUID) : UiEvent()

        /** Per-item progress update for operations like copy/move.
         * opId identifies the overall operation, mediaId and itemIndex
         * identify the specific item, and percent is the completion percentage
         * for that item.
         */
        data class ShowItemProgress(
            val opId : UUID,
            val mediaId : Long?,
            val itemIndex : Int,
            val percent : Int,
        ) : UiEvent()
    }

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 128)
    val events = _events.asSharedFlow()

    suspend fun post(event : UiEvent) {
        _events.emit(event)
    }

    fun tryPost(event : UiEvent) {
        _events.tryEmit(event)
    }
}

