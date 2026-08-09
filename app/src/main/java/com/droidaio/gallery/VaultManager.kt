package com.droidaio.gallery

import android.content.Context
import android.net.Uri
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.droidaio.gallery.models.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Minimal vault manager that stores files under app filesDir/vault.
 * - listVaultFiles returns VaultFile list
 * - unlockFromVault copies the vault file to the destination Uri (SAF)
 * - deleteVaultFile removes the file from disk
 *
 * This is intentionally small and synchronous for clarity; adapt to your
 * app's needs.
 */
object VaultManager {

    private const val VAULT_DIR = "vault_files"

    suspend fun lockToVault(context : Context, items : List<MediaItem>) {
        withContext(Dispatchers.IO) {
            val vaultDir = File(context.filesDir, VAULT_DIR)
            if (!vaultDir.exists()) vaultDir.mkdirs()
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            items.forEach { item ->
                try {
                    val name = item.displayName ?: "file_${item.id}"
                    val destFile = File(vaultDir, name)
                    context.contentResolver.openInputStream(item.uri).use { input ->
                        if (input != null) {
                            val encryptedFile = EncryptedFile.Builder(
                                context,
                                destFile,
                                masterKey,
                                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                            ).build()
                            encryptedFile.openFileOutput().use { out ->
                                input.copyTo(out)
                            }
                        }
                    }
                    context.contentResolver.delete(item.uri, null, null)
                } catch (_ : Exception) {
                }
            }
        }
    }

    suspend fun listVaultFiles(context : Context) : List<File> {
        return withContext(Dispatchers.IO) {
            val vaultDir = File(context.filesDir, VAULT_DIR)
            if (!vaultDir.exists()) return@withContext emptyList()
            vaultDir.listFiles()?.toList() ?: emptyList()
        }
    }

    suspend fun unlockFromVault(context : Context, file : File, destUri : Uri) {
        withContext(Dispatchers.IO) {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val encryptedFile = EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            encryptedFile.openFileInput().use { input ->
                context.contentResolver.openOutputStream(destUri).use { out ->
                    if (out != null) {
                        input.copyTo(out)
                    }
                }
            }
            file.delete()
        }
    }

    fun deleteVaultFile(context : Context, file : VaultFile) : Boolean {
        return try {
            val f = File(file.filePath)
            f.delete()
        } catch (e : Exception) {
            e.printStackTrace()
            false
        }
    }
}

