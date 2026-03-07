package com.droidaio.gallery

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.droidaio.gallery.models.MediaItem
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object GoogleDriveManager {

    // Your backend endpoint that exchanges serverAuthCode for refresh/access tokens and stores refresh token securely.
    // Replace this with your real server endpoint in strings.xml or build config.
    private const val TAG = "GoogleDriveManager"
    private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
    private const val GOOGLE_CLIENT_ID = "673284168708-qikna8dgit46607ujf932aqq3imie7e6.apps.googleusercontent.com"
    private const val SERVER_TOKEN_EXCHANGE_URL_KEY = "token_Xchange"

    /*
     * Build the Google Sign-In intent. We request a server auth code so the backend can exchange it for refresh tokens.
    */
    fun getSignInIntent(activity : Activity) : Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
            .requestEmail()
            .requestServerAuthCode(GOOGLE_CLIENT_ID, false)
            .build()
        val client = GoogleSignIn.getClient(activity, gso)
        return client.signInIntent
    }

    fun getSignedInAccount(context : Context) : GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    /*
     * Called after GoogleSignIn success on the client. This method:
     * 1) extracts the serverAuthCode from the GoogleSignInAccount
     * 2) sends it to your backend server (SERVER_TOKEN_EXCHANGE_URL) for secure exchange
     * 3) backend should return a success response (and optionally a short-lived access token)
     *
     * This method stores only a short-lived access token if returned by server; refresh tokens must be stored server-side.
    */
    suspend fun onSignInSuccessAndExchange(
        context : Context,
        account : GoogleSignInAccount,
        serverExchangeUrl : String,
    ) {
        withContext(Dispatchers.IO) {
            try {
                val serverAuthCode = account.serverAuthCode
                    ?: throw IllegalStateException("No server auth code returned by Google Sign-In")

                // Build JSON payload for your server
                val payload = JSONObject().apply {
                    put("serverAuthCode", serverAuthCode)
                    put("clientId", GOOGLE_CLIENT_ID)
                    put("accountEmail", account.email ?: "")
                    put("packageName", context.packageName)
                }

                val client = OkHttpClient()
                val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url(serverExchangeUrl)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        val msg = resp.body?.string() ?: "HTTP ${resp.code}"
                        Log.e(TAG, "Server exchange failed: $msg")
                        throw RuntimeException("Server exchange failed: $msg")
                    }

                    // Server should return JSON with optional short-lived access token for immediate uploads.
                    val respBody = resp.body?.string()
                    if (!respBody.isNullOrEmpty()) {
                        val json = JSONObject(respBody)
                        if (json.has("access_token")) {
                            val accessToken = json.getString("access_token")
                            TokenStore.saveGoogleToken(context, accessToken)
                        }

                        // Do not store refresh token on device. Server stores refresh token securely.
                    }
                }
            } catch (e : Exception) {
                Log.e(TAG, "onSignInSuccessAndExchange error", e)
                throw e
            }
        }
    }

    /*
     * Upload files to Drive using an access token previously obtained (either client-side or from server).
     * If no token is available, caller should prompt sign-in and server exchange.
    */
    suspend fun uploadFiles(context : Context, items : List<MediaItem>, accessToken : String) {
        withContext(Dispatchers.IO) {
            if (accessToken.isBlank()) {
                Log.e(TAG, "No Google access token provided for upload")
                throw IllegalStateException("No Google access token provided for upload. Please sign in.")
            }
            val client = OkHttpClient()
            items.forEach { item ->
                try {
                    context.contentResolver.openInputStream(item.uri).use { input ->
                        if (input != null) {
                            val metadataJson = """{"name":"${item.displayName ?: "file_${item.id}"}"}"""
                            val metadataPart = metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull())
                            val fileBytes = input.readBytes()
                            val filePart = fileBytes.toRequestBody(item.mimeType?.toMediaTypeOrNull(), 0, fileBytes.size)
                            val multipartBody = MultipartBody.Builder().setType(MultipartBody.MIXED)
                                .addPart(MultipartBody.Part.create(metadataPart))
                                .addPart(MultipartBody.Part.createFormData("file", item.displayName ?: "file", filePart))
                                .build()
                            val request = Request.Builder()
                                .url(DRIVE_UPLOAD_URL)
                                .addHeader("Authorization", "Bearer $accessToken")
                                .post(multipartBody)
                                .build()
                            client.newCall(request).execute().use { resp ->
                                if (!resp.isSuccessful) {
                                    Log.e(TAG, "Upload failed: ${resp.code} ${resp.message}")
                                    throw RuntimeException("Upload failed: ${resp.code} ${resp.message}")
                                }
                            }
                        } else {
                            Log.w(TAG, "Could not open inputStream for ${item.uri}")
                        }
                    }
                } catch (e : Exception) {
                    Log.e(TAG, "Upload exception", e)
                    throw e
                }
            }
        }
    }
}

/**
package com.droidaio.gallery

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.droidaio.gallery.models.MediaItem
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object GoogleDriveManager {

// Your backend endpoint that exchanges serverAuthCode for refresh/access tokens and stores refresh token securely.
// Replace this with your real server endpoint in strings.xml or build config.
private const val TAG = "GoogleDriveManager"
private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
private const val GOOGLE_CLIENT_ID = "673284168708-qikna8dgit46607ujf932aqq3imie7e6.apps.googleusercontent.com"
private const val SERVER_TOKEN_EXCHANGE_URL_KEY = "token_Xchange"

/*
 * Build the Google Sign-In intent. We request a server auth code so the backend can exchange it for refresh tokens.
*/
fun getSignInIntent(activity : Activity) : Intent {
val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
.requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
.requestEmail()
.requestServerAuthCode(GOOGLE_CLIENT_ID, false)
.build()
val client = GoogleSignIn.getClient(activity, gso)
return client.signInIntent
}

fun getSignedInAccount(context : Context) : GoogleSignInAccount? {
return GoogleSignIn.getLastSignedInAccount(context)
}

/*
 * Called after GoogleSignIn success on the client. This method:
 * 1) extracts the serverAuthCode from the GoogleSignInAccount
 * 2) sends it to your backend server (SERVER_TOKEN_EXCHANGE_URL) for secure exchange
 * 3) backend should return a success response (and optionally a short-lived access token)
 *
 * This method stores only a short-lived access token if returned by server; refresh tokens must be stored server-side.
*/
suspend fun onSignInSuccessAndExchange(
context : Context,
account : GoogleSignInAccount,
serverExchangeUrl : String,
) {
withContext(Dispatchers.IO) {
try {
val serverAuthCode = account.serverAuthCode
?: throw IllegalStateException("No server auth code returned by Google Sign-In")

// Build JSON payload for your server
val payload = JSONObject().apply {
put("serverAuthCode", serverAuthCode)
put("clientId", GOOGLE_CLIENT_ID)
put("accountEmail", account.email ?: "")
put("packageName", context.packageName)
}

val client = OkHttpClient()
val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
val request = Request.Builder()
.url(serverExchangeUrl)
.post(body)
.build()

client.newCall(request).execute().use { resp ->
if (!resp.isSuccessful) {
val msg = resp.body?.string() ?: "HTTP ${resp.code}"
Log.e(TAG, "Server exchange failed: $msg")
throw RuntimeException("Server exchange failed: $msg")
}
// Server should return JSON with optional short-lived access token for immediate uploads.
val respBody = resp.body?.string()
if (!respBody.isNullOrEmpty()) {
val json = JSONObject(respBody)
if (json.has("access_token")) {
val accessToken = json.getString("access_token")
TokenStore.saveGoogleToken(context, accessToken)
}
// Do not store refresh token on device. Server stores refresh token securely.
}
}
} catch (e : Exception) {
Log.e(TAG, "onSignInSuccessAndExchange error", e)
throw e
}
}
}

/*
 * Upload files to Drive using an access token previously obtained (either client-side or from server).
 * If no token is available, caller should prompt sign-in and server exchange.
*/
suspend fun uploadFiles(context : Context, items : List<MediaItem>) {
withContext(Dispatchers.IO) {
val token = TokenStore.getGoogleToken(context) ?: run {
Log.e(TAG, "No Google access token available")
throw IllegalStateException("No Google access token available. Please sign in.")
}
val client = OkHttpClient()
items.forEach { item ->
try {
context.contentResolver.openInputStream(item.uri).use { input ->
if (input != null) {
val metadataJson = """{"name":"${item.displayName ?: "file_${item.id}"}"}"""
val metadataPart = metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull())
val fileBytes = input.readBytes()
val filePart = fileBytes.toRequestBody(item.mimeType?.toMediaTypeOrNull(), 0, content.size)
val multipartBody = MultipartBody.Builder().setType(MultipartBody.MIXED)
.addPart(MultipartBody.Part.create(metadataPart))
.addPart(MultipartBody.Part.createFormData("file", item.displayName ?: "file", filePart))
.build()
val request = Request.Builder()
.url(DRIVE_UPLOAD_URL)
.addHeader("Authorization", "Bearer $token")
.post(multipartBody)
.build()
client.newCall(request).execute().use { resp ->
if (!resp.isSuccessful) {
Log.e(TAG, "Upload failed: ${resp.code} ${resp.message}")
throw RuntimeException("Upload failed: ${resp.code} ${resp.message}")
}
}
}
}
} catch (e : Exception) {
Log.e(TAG, "Upload exception", e)
throw e
}
}
}
}
}

 */
