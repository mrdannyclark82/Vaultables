package com.example.data.remote

import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches X-Firebase-AppCheck. Fail-soft: missing token still sends the
 * request so monitor-mode backend can log and Auth can still gate.
 */
class AppCheckHeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = appCheckTokenOrNull()
        val request = if (token.isNullOrBlank()) {
            original
        } else {
            original.newBuilder()
                .header(HEADER, token)
                .build()
        }
        return chain.proceed(request)
    }

    private fun appCheckTokenOrNull(): String? {
        return try {
            runBlocking {
                withTimeout(15_000) {
                    FirebaseAppCheck.getInstance().getAppCheckToken(false).await().token
                }
            }
        } catch (error: Exception) {
            Log.w(TAG, "App Check token unavailable: ${error.message}")
            null
        }
    }

    companion object {
        const val HEADER = "X-Firebase-AppCheck"
        private const val TAG = "AppCheckHeader"
    }
}
