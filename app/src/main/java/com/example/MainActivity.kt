package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.MainScreen
import com.example.ui.theme.VaultTheme
import com.example.ui.viewmodel.VaultViewModel
import com.google.firebase.FirebaseApp
import com.stripe.android.PaymentConfiguration

class MainActivity : ComponentActivity() {
    private val viewModel: VaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.d("MainActivity", "FirebaseApp initialization notice: ${e.message}")
        }
        val stripeKey = try { BuildConfig.STRIPE_PUBLISHABLE_KEY } catch (e: Exception) { "pk_test_TYooMQauvdEDq54NiTphI7jx" }
        PaymentConfiguration.init(applicationContext, stripeKey)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            VaultTheme(darkTheme = uiState.isDarkMode) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
