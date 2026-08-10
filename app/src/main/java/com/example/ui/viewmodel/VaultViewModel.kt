package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.VaultRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.example.data.remote.GoogleAuthManager
import com.example.data.remote.UserAccount

import com.example.ui.components.ReferralItem
import com.example.ui.components.ReferralState

data class VaultUiState(
    val isDarkMode: Boolean = true,
    val selectedCurrency: CurrencyCode = CurrencyCode.USD,
    val isBiometricLocked: Boolean = false,
    val currentUser: UserAccount = UserAccount(),
    val showAuthModal: Boolean = false,
    val referralState: ReferralState = ReferralState(),
    val showReferralModal: Boolean = false,
    val selectedCategory: String? = null,
    val selectedSubcategory: String? = null,
    val searchQuery: String = "",
    val activeTab: Int = 0,
    val selectedItemForDetail: CollectibleItem? = null,
    val showAiScannerDialog: Boolean = false,
    val showAddCustomItemDialog: Boolean = false,
    val showEscrowDialog: Boolean = false,
    val showShippingLabelDialog: Boolean = false,
    val selectedEscrowForLabel: EscrowTransaction? = null,
    val showNotificationDrawer: Boolean = false,
    val showActivityReportDialog: Boolean = false,
    val isScanningInProgress: Boolean = false,
    val scanStatusMessage: String = "",
    val paymentClientSecret: String? = null,
    val paymentError: String? = null
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = VaultRepository(db)

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    val allItems: StateFlow<List<CollectibleItem>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val marketplaceListings: StateFlow<List<CollectibleItem>> = repository.marketplaceListings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEscrows: StateFlow<List<EscrowTransaction>> = repository.allEscrows
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMessages: StateFlow<List<ChatMessage>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAlerts: StateFlow<List<TradeAlert>> = repository.allAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReviews: StateFlow<List<UserReview>> = repository.allReviews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        val loadedUser = GoogleAuthManager.getSavedUserAccount(application)
        _uiState.update { it.copy(currentUser = loadedUser) }

        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun setShowAuthModal(show: Boolean) {
        _uiState.update { it.copy(showAuthModal = show) }
    }

    fun signInWithGoogle(context: android.content.Context) {
        GoogleAuthManager.performGoogleSignIn(
            context = context,
            onSuccess = { user ->
                _uiState.update { it.copy(currentUser = user, showAuthModal = false) }
            },
            onError = { err ->
                _uiState.update { it.copy(showAuthModal = false) }
            }
        )
    }

    fun signOut(context: android.content.Context) {
        GoogleAuthManager.signOut(context)
        val signedOutUser = UserAccount(displayName = "Guest User", email = "", isSignedIn = false)
        _uiState.update { it.copy(currentUser = signedOutUser) }
    }

    fun clearMockDataForProduction() {
        viewModelScope.launch {
            repository.clearAllMockItems()
        }
    }

    fun toggleDarkMode() {
        _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun selectCurrency(currency: CurrencyCode) {
        _uiState.update { it.copy(selectedCurrency = currency) }
    }

    fun toggleBiometricLock(locked: Boolean) {
        _uiState.update { it.copy(isBiometricLocked = locked) }
    }

    fun setActiveTab(index: Int) {
        _uiState.update { it.copy(activeTab = index) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectCategory(category: String?) {
        _uiState.update {
            val newCategory = if (it.selectedCategory == category) null else category
            it.copy(selectedCategory = newCategory, selectedSubcategory = null)
        }
    }

    fun selectSubcategory(subcategory: String?) {
        _uiState.update {
            val newSubcategory = if (it.selectedSubcategory == subcategory) null else subcategory
            it.copy(selectedSubcategory = newSubcategory)
        }
    }

    fun selectItemForDetail(item: CollectibleItem?) {
        _uiState.update { it.copy(selectedItemForDetail = item) }
    }

    fun setShowAiScanner(show: Boolean) {
        _uiState.update { it.copy(showAiScannerDialog = show) }
    }

    fun setShowAddCustomItem(show: Boolean) {
        _uiState.update { it.copy(showAddCustomItemDialog = show) }
    }

    fun setShowEscrowDialog(show: Boolean) {
        _uiState.update { it.copy(showEscrowDialog = show) }
    }

    fun setShowShippingLabelDialog(show: Boolean, escrow: EscrowTransaction? = null) {
        _uiState.update { it.copy(showShippingLabelDialog = show, selectedEscrowForLabel = escrow) }
    }

    fun setShowNotificationDrawer(show: Boolean) {
        _uiState.update { it.copy(showNotificationDrawer = show) }
    }

    fun setShowActivityReportDialog(show: Boolean) {
        _uiState.update { it.copy(showActivityReportDialog = show) }
    }

    fun scanAndAddCollectible(
        title: String,
        category: String,
        description: String,
        imageType: String,
        brand: String = "",
        year: String = "",
        localImagePath: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanningInProgress = true,
                    scanStatusMessage = "Performing optical entity recognition & neural identification..."
                )
            }
            kotlinx.coroutines.delay(1000)
            _uiState.update { it.copy(scanStatusMessage = "Extracting player name, manufacturer brand & release year...") }
            kotlinx.coroutines.delay(1000)
            _uiState.update { it.copy(scanStatusMessage = "Consulting Gemini AI market pricing & grading ledger...") }
            kotlinx.coroutines.delay(800)

            val added = repository.addNewCollectible(title, category, description, imageType, brand, year, localImagePath)

            _uiState.update {
                it.copy(
                    isScanningInProgress = false,
                    showAiScannerDialog = false,
                    showAddCustomItemDialog = false,
                    selectedItemForDetail = added
                )
            }
        }
    }

    fun createEscrowTrade(item: CollectibleItem, buyerName: String, feePercentage: Double = 3.5) {
        viewModelScope.launch {
            try {
                // Simulate hitting the Vaultables backend to get a PaymentIntent for Stripe
                val response = repository.createStripePaymentIntent(item.id, item.estimatedValueUsd, buyerName)
                _uiState.update { it.copy(paymentClientSecret = response.clientSecret, paymentError = null, showEscrowDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(paymentError = e.message, showEscrowDialog = false) }
            }
        }
    }

    fun onPaymentSheetResult(success: Boolean, item: CollectibleItem?, buyerName: String, feePercentage: Double = 3.5) {
        viewModelScope.launch {
            _uiState.update { it.copy(paymentClientSecret = null, paymentError = null) }
            if (success && item != null) {
                val currCode = _uiState.value.selectedCurrency.code
                repository.createEscrow(item, buyerName, currCode, feePercentage)
                _uiState.update { it.copy(showEscrowDialog = false) }
            }
        }
    }

    fun clearPaymentError() {
        _uiState.update { it.copy(paymentError = null) }
    }

    fun confirmEscrowBuyer(escrowId: Long) {
        viewModelScope.launch {
            repository.confirmEscrowBuyer(escrowId)
        }
    }

    fun confirmEscrowSeller(escrowId: Long) {
        viewModelScope.launch {
            repository.confirmEscrowSeller(escrowId)
        }
    }

    fun sendChatMessage(text: String, receiver: String) {
        viewModelScope.launch {
            if (text.isNotBlank()) {
                repository.sendEncryptedMessage(text, receiver)
            }
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            db.tradeAlertDao().markAllAsRead()
        }
    }

    fun formatPrice(amountUsd: Double): String {
        return repository.convertCurrency(amountUsd, _uiState.value.selectedCurrency)
    }

    fun setShowReferralModal(show: Boolean) {
        _uiState.update { it.copy(showReferralModal = show) }
    }

    fun claimReferralReward(refereeEmail: String) {
        val currentRefState = _uiState.value.referralState
        val updatedList = currentRefState.referrals.map { item ->
            if (item.refereeEmail == refereeEmail) {
                item.copy(isRewardClaimed = true)
            } else {
                item
            }
        }
        val newEarned = currentRefState.freeMonthsEarned + 1
        val newClaimed = currentRefState.freeMonthsClaimed + 1

        _uiState.update {
            it.copy(
                referralState = currentRefState.copy(
                    freeMonthsEarned = newEarned,
                    freeMonthsClaimed = newClaimed,
                    referrals = updatedList
                )
            )
        }
    }

    fun exportPortfolioCsv(context: android.content.Context) {
        val items = allItems.value
        val sb = StringBuilder()
        sb.append("ID,Title,Category,Brand,ReleaseYear,ConditionGrade,CertNumber,EstimatedValueUSD,VaultHash,IsListed\n")
        items.forEach { item ->
            sb.append("${item.id},\"${item.title}\",\"${item.category}\",\"${item.brandName}\",\"${item.releaseYear}\",\"${item.conditionGrade}\",\"${item.certSerialNumber}\",${item.estimatedValueUsd},\"${item.vaultHashId}\",${item.isListedForSale}\n")
        }

        val sendIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TITLE, "Vault_Portfolio_Insurance_Ledger.csv")
            putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
            type = "text/csv"
        }
        context.startActivity(android.content.Intent.createChooser(sendIntent, "Export Vault Portfolio CSV (Insurance Ledger)"))
    }

    fun generateActivityReport(): String {
        return repository.generateActivityReportText(allItems.value, allEscrows.value)
    }
}
