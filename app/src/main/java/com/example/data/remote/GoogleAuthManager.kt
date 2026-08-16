package com.example.data.remote

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserAccount(
    val uid: String = "",
    val displayName: String = "Guest User",
    val email: String = "",
    val photoUrl: String? = null,
    val isSignedIn: Boolean = true,
    val authProvider: String = "Google OAuth 2.0"
)

object GoogleAuthManager {
    private const val TAG = "GoogleAuthManager"
    private const val PREF_NAME = "vault_google_auth_prefs"
    private const val KEY_IS_SIGNED_IN = "is_signed_in"
    private const val KEY_DISPLAY_NAME = "display_name"
    private const val KEY_EMAIL = "email"
    private const val KEY_UID = "uid"

    fun getSavedUserAccount(context: Context): UserAccount {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val isSignedIn = prefs.getBoolean(KEY_IS_SIGNED_IN, false)
        val name = prefs.getString(KEY_DISPLAY_NAME, "Guest User") ?: "Guest User"
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val uid = prefs.getString(KEY_UID, "") ?: ""

        val firebaseUser = try {
            FirebaseAuth.getInstance().currentUser
        } catch (e: Exception) {
            null
        }

        if (firebaseUser != null) {
            return UserAccount(
                uid = firebaseUser.uid,
                displayName = firebaseUser.displayName ?: name,
                email = firebaseUser.email ?: email,
                photoUrl = firebaseUser.photoUrl?.toString(),
                isSignedIn = true,
                authProvider = "Google OAuth 2.0 (Firebase)"
            )
        }

        return UserAccount(
            uid = uid,
            displayName = name,
            email = email,
            photoUrl = null,
            isSignedIn = isSignedIn,
            authProvider = "Google OAuth 2.0"
        )
    }

    fun saveUserAccount(context: Context, account: UserAccount) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_SIGNED_IN, account.isSignedIn)
            .putString(KEY_DISPLAY_NAME, account.displayName)
            .putString(KEY_EMAIL, account.email)
            .putString(KEY_UID, account.uid)
            .apply()
    }

    fun signOut(context: Context) {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            Log.d(TAG, "Firebase sign out: ${e.message}")
        }
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_SIGNED_IN, false).apply()
    }

    fun performGoogleSignIn(
        context: Context,
        onSuccess: (UserAccount) -> Unit,
        onError: (String) -> Unit
    ) {
        val clientId = try { com.example.BuildConfig.GOOGLE_WEB_CLIENT_ID } catch (e: Exception) { "31438583-c761-49b7-9079-54a7888b5567.apps.googleusercontent.com" }
        // Build Google ID Option for CredentialManager
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(context)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (
                    credential !is CustomCredential ||
                    credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    onError("Google did not return an ID credential.")
                    return@launch
                }
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
                val firebaseUser = FirebaseAuth.getInstance()
                    .signInWithCredential(firebaseCredential)
                    .await()
                    .user
                    ?: throw IllegalStateException("Firebase did not return a signed-in user.")
                val account = UserAccount(
                    uid = firebaseUser.uid,
                    displayName = firebaseUser.displayName ?: "Vaultables Collector",
                    email = firebaseUser.email.orEmpty(),
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    isSignedIn = true,
                    authProvider = "Google OAuth 2.0 (Firebase)"
                )
                saveUserAccount(context, account)
                onSuccess(account)
            } catch (e: GetCredentialException) {
                Log.w(TAG, "Google sign-in credential request failed", e)
                onError("Google sign-in was canceled or unavailable.")
            } catch (e: Exception) {
                Log.e(TAG, "Google sign-in failed", e)
                onError("Unable to sign in with Google.")
            }
        }
    }
}
