package com.droidaio.gallery

import android.app.Application
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException

class GalleryApp : Application() {

    companion object {
        @Volatile
        var msalApp : ISingleAccountPublicClientApplication? = null
            private set

        fun getMsalApp() : ISingleAccountPublicClientApplication? = msalApp
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize MSAL using auth_config.json in res/raw via the recommended factory method.
        try {
            PublicClientApplication.createSingleAccountPublicClientApplication(
                this,
                R.raw.auth_config,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    /*fun onCreated(application : PublicClientApplication) {
                        msalApp = application
                    }*/

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

