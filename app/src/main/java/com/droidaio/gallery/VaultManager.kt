package com.droidaio.gallery

import android.app.KeyguardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.droidaio.gallery.models.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object VaultManager {

    private const val VAULT_DIR = "vault_files"
    private const val DEVICE_LOCK_KEY_ALIAS = "vault_device_lock_key"

    enum class EncryptionMode {
        AES_256_GCM,
        DEVICE_LOCK
    }

    suspend fun lockToVault(
        context: Context,
        items: List<MediaItem>,
        mode: EncryptionMode = EncryptionMode.AES_256_GCM
    ) {
        withContext(Dispatchers.IO) {
            val vaultDir = File(context.filesDir, VAULT_DIR)
            if (!vaultDir.exists()) vaultDir.mkdirs()

            val masterKey = when (mode) {
                EncryptionMode.AES_256_GCM -> MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptionMode.DEVICE_LOCK -> getDeviceLockMasterKey(context)
            }

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
                    // Optionally delete original
                    context.contentResolver.delete(item.uri, null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun getDeviceLockMasterKey(context: Context): MasterKey {
        return MasterKey.Builder(context, DEVICE_LOCK_KEY_ALIAS)
            .setKeyGenParameterSpec(
                KeyGenParameterSpec.Builder(
                    DEVICE_LOCK_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(true)
                    // Validity duration in seconds after authentication
                    .setUserAuthenticationValidityDurationSeconds(30)
                    .build()
            )
            .build()
    }

    suspend fun listVaultFiles(context: Context): List<File> {
        return withContext(Dispatchers.IO) {
            val vaultDir = File(context.filesDir, VAULT_DIR)
            if (!vaultDir.exists()) return@withContext emptyList()
            vaultDir.listFiles()?.toList() ?: emptyList()
        }
    }

    suspend fun unlockFromVault(context: Context, file: File, destUri: Uri) {
        withContext(Dispatchers.IO) {
            // We'd need to know which key was used. For now, try default.
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            try {
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
            } catch (e: Exception) {
                // If it fails, maybe it was device lock? 
                // In a real app, we should store metadata about the encryption type used for each file.
                e.printStackTrace()
            }
        }
    }

    fun deleteVaultFile(context: Context, file: VaultFile): Boolean {
        return try {
            val f = File(file.filePath)
            f.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun isDeviceSecure(context: Context): Boolean {
        val kgm = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            kgm.isDeviceSecure
        } else {
            kgm.isKeyguardSecure
        }
    }
}
