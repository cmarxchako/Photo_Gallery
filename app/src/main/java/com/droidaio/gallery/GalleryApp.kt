package com.droidaio.gallery

import android.app.Application
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException

/**
 * The main Application class for the Gallery app. This class is responsible for initializing the MSAL
 * public client application instance that will be used for authentication throughout the app. The MSAL
 * instance is created asynchronously using the configuration specified in the auth_config.json file located
 * in the res/raw directory. The instance is stored in a companion object to allow easy access from other
 * parts of the app. If the initialization fails, the MSAL instance will be set to null, and any errors
 * will be printed to the console for debugging purposes.
 */
class GalleryApp : Application() {

    companion object {
        @Volatile
        var msalApp : ISingleAccountPublicClientApplication? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize MSAL using auth_config.json in res/raw via the recommended factory method.
        try {
            PublicClientApplication.createSingleAccountPublicClientApplication(
                this,
                R.raw.auth_config,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application : ISingleAccountPublicClientApplication) {
                        msalApp = application
                    }

                    override fun onError(exception : MsalException) {
                        exception.printStackTrace()
                        msalApp = null
                    }
                }
            )
        } catch (ex : Exception) {
            ex.printStackTrace()
            msalApp = null
        }
    }
}

