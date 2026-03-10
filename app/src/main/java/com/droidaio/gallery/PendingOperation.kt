package com.droidaio.gallery

import java.io.Serializable
import java.util.UUID

/**
 * Represents a persisted pending operation (copy/move/delete).
 * - id: unique operation id (UUID)
 * - type: COPY, MOVE, DELETE
 * - itemIds: list of media item ids involved
 * - itemUris: list of item URIs (string) to allow resuming after restart
 * - targetTreeUri: destination tree URI (string) for copy/move; null for delete
 * - createdAt: epoch millis when scheduled
 * - status: PENDING, COMPLETED, CANCELLED, FAILED
 * - message: optional status message (e.g. error details)
 * - attempts: number of execution attempts made
 * - maxAttempts: max retry attempts before giving up (default 3)
 */
data class PendingOperation(
    val id: UUID = UUID.randomUUID(),
    val type: Type,
    val itemIds: List<Long> = emptyList(),
    val itemUris: List<String> = emptyList(),
    val itemNames: List<String> = emptyList(),
    val targetTreeUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    var status: Status = Status.PENDING,
    var message: String? = null,
    var attempts: Int = 0,
    val maxAttempts: Int = 3,
) : Serializable {
    enum class Type { COPY, MOVE, DELETE }
    enum class Status { PENDING, SCHEDULED, COMPLETED, CANCELLED, FAILED }
}
