@file:Suppress("DEPRECATION")

package com.droidaio.gallery

import android.app.Activity
import android.content.Context
import android.util.Log
import com.droidaio.gallery.models.MediaItem
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalUiRequiredException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object OneDriveManager {

    private const val TAG = "OneDriveManager"
    private val SCOPES = arrayOf("Files.ReadWrite.All", "offline_access", "User.Read")
    private var msalApp : ISingleAccountPublicClientApplication? = null

    /*
     * Ensure msalApp is initialized. Prefer the instance created in GalleryApp.
     * Try to reuse the application-level instance if available
     * Fallback: create a new instance reading res/raw/auth_config.json
     * If still null, attempt to create a PublicClientApplication using the JSON config resource if available.
     * Many projects initialize this centrally (GalleryApp). If you want to initialize here instead,
     * use PublicClientApplication.createSingleAccountPublicClientApplication(...) or createMultipleAccount... .
     * PublicClientApplication(context.applicationContext.
    */

    /**
     * Initialize msalApp (prefer the centralized instance created in GalleryApp).
     * We cast the GalleryApp-held instance into the IPublicClientApplication interface to use the public APIs.
     */
    fun init(context : Context) {
        if (msalApp == null) {
            msalApp = try {
                GalleryApp.msalApp
            } catch (ex : Exception) {
                Log.e(TAG, "Failed to initialize MSAL PublicClientApplication", ex)
                null
            }
        } else {
            Log.d(TAG, "MSAL PublicClientApplication already initialized")
        }
    }

    /**
     * Basic signin check (non-blocking). This is a conservative check: it returns true if the MSAL client
     * is available. A precise signed-in status requiring account lookup is asynchronous — see getAnyAccount().
     */
    fun isSignedIn() : Boolean = try {
        initIfNeeded()
        msalApp != null
    } catch (ex : Exception) {
        Log.e(TAG, "Error checking sign-in status", ex)
        false
    }

    /**
     * Start an interactive sign-in flow.
     * Caller must call this from an Activity.
     */
    fun signIn(activity : Activity, callback : AuthenticationCallback) {
        msalApp?.signIn(activity, null, SCOPES, callback) ?: callback.onError(
            MsalClientException("MSAL_NOT_INITIALIZED", "MSAL not initialized")
        )
    }

    /**
     * Obtain any available account (suspending). Handles both single-account and multi-account
     * MSAL apps. This wraps the MSAL callbacks and returns the first found IAccount or null
     * if none.
     */
    private suspend fun getAnyAccount() : IAccount? = suspendCancellableCoroutine { cont ->
        try {
            val app = msalApp
            if (app == null) {
                cont.resumeWithException(IllegalStateException("msalApp is not initialized"))
                return@suspendCancellableCoroutine
            }

            // If the app is a single-account application, use getCurrentAccountAsync
            app.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
                override fun onAccountLoaded(currentAccount : IAccount?) {
                    cont.resume(currentAccount)
                }

                override fun onAccountChanged(priorAccount : IAccount?, currentAccount : IAccount?) {
                    // treat as loaded with the currentAccount
                    cont.resume(currentAccount)
                }

                override fun onError(exception : MsalException) {
                    cont.resumeWithException(exception)
                }
            })
        } catch (ex : Exception) {
            cont.resumeWithException(ex)
        }
    }

    /**
     * Attempt to get a valid access token silently.
     * This suspends until the result or error.
     */
    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun getAccessTokenSilent() : String = suspendCancellableCoroutine { cont ->
        try {
            val app = msalApp
            if (app == null) {
                cont.resumeWithException(IllegalStateException("No MSAL account or client available"))
                return@suspendCancellableCoroutine
            }
            kotlin.concurrent.thread {
                // We call getAnyAccount() but cannot call suspend from here, so we use a helper approach below:
            }

            GlobalScope.launch(Dispatchers.Main) {
                try {
                    val account = getAnyAccount()
                    if (account == null) {
                        cont.resumeWithException(IllegalStateException("No MSAL account available"))
                        return@launch
                    }

                    val silentParams = AcquireTokenSilentParameters.Builder()
                        .withScopes(SCOPES.toList())
                        .forAccount(account)
                        .withCallback(object : SilentAuthenticationCallback {
                            override fun onSuccess(authenticationResult : IAuthenticationResult) {
                                cont.resume(authenticationResult.accessToken)
                            }

                            override fun onError(exception : MsalException) {
                                cont.resumeWithException(exception)
                            }
                        })
                        .build()

                    msalApp!!.acquireTokenSilent(silentParams)
                } catch (ex : Exception) {
                    cont.resumeWithException(ex)
                }
            }
        } catch (ex : Exception) {
            cont.resumeWithException(ex)
        }
    }

    private fun initIfNeeded() {
        if (msalApp == null) {
            throw IllegalStateException("OneDriveManager not initialized. Call init(context) first.")
        }
    }

    /**
     * Interactive sign-in helper (backwards compatibility). Caller provides an AuthenticationCallback
     * to handle success/error/cancel. We attempt to call the library convenience method if available.
     */
    fun acquireTokenInteractive(activity : Activity, callback : AuthenticationCallback) {
        init(activity)
        msalApp?.let { app ->
            try {
                try {
                    if (app is PublicClientApplication) {
                        app.acquireToken(activity, SCOPES, callback)
                        return
                    } else {
                        app.acquireToken(activity, SCOPES, callback)
                        return
                    }
                } catch (_ : Throwable) {
                    // If the above fails (e.g. method not found), we fall back to the builder approach below.
                    Log.w(TAG, "MSAL app acquireToken with Activity failed. Falling back to builder.")
                }

                val builder = AcquireTokenParameters.Builder()
                builder.startAuthorizationFromActivity(activity)
                    .withScopes(SCOPES.toList())
                    .withCallback(callback)
                val params = builder.build()
                app.acquireToken(params)
            } catch (ex : Exception) {
                Log.e(TAG, "Failed to acquire token interactively", ex)
                callback.onError(MsalClientException("acquire_interactive_failed", ex.localizedMessage ?: "Failed to acquire token interactively"))
            }
        } ?: run {
            Log.w(TAG, "MSAL app not initialized (call OneDriveManager.init(context) early).")
            callback.onError(MsalClientException("msal_not_initialized", "MSAL not initialized"))
        }
    }

    /**
     * Upload files using a silent token (acquireTokenSilent). If silent fails, caller
     * should trigger interactive sign-in.
     */
    suspend fun uploadFiles(context : Context, items : List<MediaItem>) {
        withContext(Dispatchers.IO) {
            val token = try {
                getAccessTokenSilent()
            } catch (ex : Exception) {
                // If a UI interaction is required, MSAL will throw MsalUiRequiredException — surface this so caller can act.
                if (ex is MsalUiRequiredException) {
                    throw ex
                }
                throw IllegalStateException("Failed to acquire OneDrive access token silently: ${ex.message}", ex)
            }

            val client = OkHttpClient()
            items.forEach { item ->
                try {
                    context.contentResolver.openInputStream(item.uri).use { input ->
                        if (input != null) {
                            val bytes = input.readBytes()
                            val fileName = item.displayName ?: "file_${item.id}"
                            val url = "https://graph.microsoft.com/v1.0/me/drive/root:/$fileName:/content"
                            val body = bytes.toRequestBody(item.mimeType?.toMediaTypeOrNull(), 0, bytes.size)
                            val request = Request.Builder()
                                .url(url)
                                .addHeader("Authorization", "Bearer $token")
                                .put(body)
                                .build()
                            client.newCall(request).execute().use { resp ->
                                if (!resp.isSuccessful) {
                                    Log.e(TAG, "OneDrive upload failed: ${resp.code} ${resp.message}")
                                    throw RuntimeException("OneDrive upload failed: ${resp.code} ${resp.message}")
                                }
                            }
                        } else {
                            Log.w(TAG, "Cannot open input stream for ${item.uri}")
                        }
                    }
                } catch (ex : Exception) {
                    Log.e(TAG, "Upload error", ex)
                    throw ex
                }
            }
        }
    }
}


/**
package com.droidaio.gallery

import android.app.Activity
import android.content.Context
import android.util.Log
import com.droidaio.gallery.models.MediaItem
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object OneDriveManager {

private const val TAG = "OneDriveManager"
private val SCOPES = arrayOf("Files.ReadWrite.All", "offline_access", "User.Read")
private var msalApp : PublicClientApplication? = null

/*
 * Ensure msalApp is initialized. Prefer the instance created in GalleryApp.
 * Try to reuse the application-level instance if available
 * Fallback: create a new instance reading res/raw/auth_config.json
 * If still null, attempt to create a PublicClientApplication using the JSON config resource if available.
 * Many projects initialize this centrally (GalleryApp). If you want to initialize here instead,
 * use PublicClientApplication.createSingleAccountPublicClientApplication(...) or createMultipleAccount... .
 * PublicClientApplication(context.applicationContext.
*/
fun init(context : Context) {
if (msalApp == null) {
try {
msalApp = GalleryApp.msalApp
} catch (ex : Exception) {
Log.e(TAG, "Failed to initialize MSAL PublicClientApplication", ex)
//null
}
} else {
Log.d(TAG, "MSAL PublicClientApplication already initialized")
}
}

fun isSignedIn() : Boolean = try {
initIfNeeded()
val accounts = msalApp?.accounts
accounts?.isNotEmpty() == true
} catch (ex : Exception) {
Log.e(TAG, "Error checking sign-in status", ex)
false
}

/**
 * Start an interactive sign-in flow.
 * Caller must call this from an Activity.
*/
fun signIn(activity : Activity, callback : AuthenticationCallback) {
msalApp?.let { app ->
try {
val builder = AcquireTokenParameters.Builder()
builder.startAuthorizationFromActivity(activity)
.withScopes(SCOPES.toList())
.withCallback(callback)
val params = builder.build()
app.acquireToken(params)
} catch (ex : Exception) {
Log.e(TAG, "signIn error", ex)
callback.onError(MsalException("signIn error: ${ex.localizedMessage}"))
}
} ?: run {
Log.w(TAG, "MSAL app not initialized (call OneDriveManager.init(context) early).")
}
}

/**
 * Attempt to get a valid access token silently.
 * This suspends until the result or error.
*/
private suspend fun getAccessTokenSilent() : String = suspendCancellableCoroutine { cont ->
try {
val accounts = msalApp?.accounts
val account = accounts?.firstOrNull()
if (msalApp == null || account == null) {
cont.resumeWithException(IllegalStateException("No MSAL account or client available"))
return@suspendCancellableCoroutine
}

val silentParams = AcquireTokenSilentParameters.Builder()
.withScopes(SCOPES.toList())
.forAccount(account)
.withCallback(object : SilentAuthenticationCallback {
override fun onSuccess(authenticationResult : IAuthenticationResult) {
cont.resume(authenticationResult.accessToken)
}

override fun onError(exception : MsalException) {
cont.resumeWithException(exception)
}
})
.build()

// call async silent API
msalApp!!.acquireTokenSilent(silentParams)
} catch (ex : Exception) {
cont.resumeWithException(ex)
}
}

private fun initIfNeeded() {
if (msalApp == null) {
throw IllegalStateException("OneDriveManager not initialized. Call init(context) first.")
}
}

/*
 * Interactive sign-in. Caller provides an AuthenticationCallback to handle success/error/cancel.
*/
fun acquireTokenInteractive(activity : Activity, callback : AuthenticationCallback) {
init(activity)
msalApp?.acquireToken(activity, SCOPES, callback)
?: callback.onError(MsalClientException("MSAL not initialized"))
}

/*
 * Upload files using a silent token (acquireTokenSilent). If silent fails, caller should trigger interactive sign-in.
*/
suspend fun uploadFiles(context : Context, items : List<MediaItem>) {
withContext(Dispatchers.IO) {
val token = try {
getAccessTokenSilent()
} catch (ex : Exception) {
throw IllegalStateException("Failed to acquire OneDrive access token silently: ${ex.message}", ex)
}

val client = OkHttpClient()
items.forEach { item ->
try {
context.contentResolver.openInputStream(item.uri).use { input ->
if (input != null) {
val bytes = input.readBytes()
val fileName = item.displayName ?: "file_${item.id}"
val url = "https://graph.microsoft.com/v1.0/me/drive/root:/$fileName:/content"
val body = bytes.toRequestBody(item.mimeType?.toMediaTypeOrNull(), 0, bytes.size)
val request = Request.Builder()
.url(url)
.addHeader("Authorization", "Bearer $token")
.put(body)
.build()
client.newCall(request).execute().use { resp ->
if (!resp.isSuccessful) {
Log.e(TAG, "OneDrive upload failed: ${resp.code} ${resp.message}")
throw RuntimeException("OneDrive upload failed: ${resp.code} ${resp.message}")
}
}
} else {
Log.w(TAG, "Cannot open input stream for ${item.uri}")
}
}
} catch (ex : MsalException) {
Log.e(TAG, "MSAL error", ex)
throw ex
}
}
}
}
}
 */

/**
@file:Suppress("DEPRECATION")

package com.droidaio.gallery

import android.app.Activity
import android.content.Context
import android.util.Log
import com.droidaio.gallery.models.MediaItem
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.*
import okhttp3.RequestBody.Companion.toRequestBody

object OneDriveManager {

private const val TAG = "OneDriveManager"
private val SCOPES = arrayOf("Files.ReadWrite.All", "offline_access", "User.Read")
private var msalApp : PublicClientApplication? = null

/*
 * Ensure msalApp is initialized. Prefer the instance created in GalleryApp.
*/
fun init(context : Context) {
if (msalApp == null) {
// Try to reuse the application-level instance if available
msalApp = GalleryApp.msalApp ?: try {
// Fallback: create a new instance reading res/raw/auth_config.json
PublicClientApplication(context.applicationContext)
} catch (ex : Exception) {
Log.e(TAG, "Failed to initialize MSAL PublicClientApplication", ex)
null
}
}
}

fun isSignedIn() : Boolean {
initIfNeeded()
return msalApp?.accounts?.isNotEmpty() == true
}

private fun initIfNeeded() {
if (msalApp == null) {
throw IllegalStateException("OneDriveManager not initialized. Call init(context) first.")
}
}

/*
 * Interactive sign-in. Caller provides an AuthenticationCallback to handle success/error/cancel.
*/
fun acquireTokenInteractive(activity : Activity, callback : AuthenticationCallback) {
init(activity)
msalApp?.acquireToken(activity, SCOPES, callback)
?: callback.onError(MsalClientException("MSAL not initialized"))
}

/*
 * Upload files using a silent token (acquireTokenSilent). If silent fails, caller should trigger interactive sign-in.
*/
suspend fun uploadFiles(context : Context, items : List<MediaItem>) {
withContext(Dispatchers.IO) {
init(context)
val accounts = msalApp?.accounts
val account = accounts?.firstOrNull()
if (account == null) {
throw IllegalStateException("No signed-in account. Call acquireTokenInteractive first.")
}
try {
// Acquire token silently
val result = msalApp!!.acquireTokenSilent(SCOPES, account, null)
val accessToken = result.accessToken
TokenStore.saveOneDriveToken(context, accessToken)

val client = OkHttpClient()
items.forEach { item ->
context.contentResolver.openInputStream(item.uri).use { input ->
if (input != null) {
val bytes = input.readBytes()
val fileName = item.displayName ?: "file_${item.id}"
val url = "https://graph.microsoft.com/v1.0/me/drive/root:/$fileName:/content"
val body = bytes.toRequestBody(item.mimeType?.toMediaTypeOrNull(), 0, content.size)
val request = Request.Builder()
.url(url)
.addHeader("Authorization", "Bearer $accessToken")
.put(body)
.build()
client.newCall(request).execute().use { resp ->
if (!resp.isSuccessful) {
Log.e(TAG, "OneDrive upload failed: ${resp.code} ${resp.message}")
throw RuntimeException("OneDrive upload failed: ${resp.code} ${resp.message}")
}
}
}
}
}
} catch (ex : MsalException) {
Log.e(TAG, "MSAL error", ex)
throw ex
}
}
}
}

 */

