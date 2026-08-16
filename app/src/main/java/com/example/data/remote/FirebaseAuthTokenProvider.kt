package com.example.data.remote

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

object FirebaseAuthTokenProvider {
    suspend fun authorizationHeader(): String {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Sign in with Google before using secure scan or payment services.")
        val token = user.getIdToken(false).await().token
            ?: throw IllegalStateException("Unable to refresh the Firebase authentication token.")
        return "Bearer $token"
    }
}
