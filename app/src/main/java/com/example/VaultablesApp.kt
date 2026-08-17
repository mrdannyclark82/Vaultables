package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class VaultablesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            installAppCheck()
        } catch (error: Exception) {
            Log.w(TAG, "Firebase / App Check init failed: ${error.message}")
        }
    }

    private fun installAppCheck() {
        val appCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            installDebugAppCheck(appCheck)
        } else {
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance(),
            )
        }
    }

    private fun installDebugAppCheck(appCheck: FirebaseAppCheck) {
        try {
            val factoryClass = Class.forName(
                "com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory",
            )
            val factory = factoryClass.getMethod("getInstance").invoke(null)
            val install = appCheck.javaClass.getMethod(
                "installAppCheckProviderFactory",
                Class.forName("com.google.firebase.appcheck.AppCheckProviderFactory"),
            )
            install.invoke(appCheck, factory)
            Log.i(
                TAG,
                "App Check debug provider installed. Register the debug token from logcat in Firebase Console → App Check.",
            )
        } catch (error: Exception) {
            Log.w(TAG, "Debug App Check provider missing; falling back to Play Integrity: ${error.message}")
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance(),
            )
        }
    }

    companion object {
        private const val TAG = "VaultablesApp"
    }
}
