package com.droidaio.gallery

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Simple model representing a file stored in the app vault.
 * `filePath` is the absolute path on internal storage
 * (or other storage the app controls).
 * - delete deletes this vault file using VaultManager helper.
 * - toFile convenient accessor for the underlying File.
 */
data class VaultFile(
    val name: String,
    val filePath: String,
    val uri: Uri? = null,
) {

    fun delete(context: Context) {
        VaultManager.deleteVaultFile(context, this)
    }

    fun toFile(): File = File(filePath)
}
