package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.VaultRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class VaultUiState(
    val isDarkMode: Boolean = true,
    val selectedCurrency: CurrencyCode = CurrencyCode.USD,
    val isBiometricLocked: Boolean = false,
    val selectedCategory: String? = null,
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
    val scanStatusMessage: String = ""
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
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
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
        _uiState.update { it.copy(selectedCategory = if (it.selectedCategory == category) null else category) }
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

    fun scanAndAddCollectible(title: String, category: String, description: String, imageType: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanningInProgress = true,
                    scanStatusMessage = "Analyzing micro-texture, corner alignment, and luster..."
                )
            }
            kotlinx.coroutines.delay(1200)
            _uiState.update { it.copy(scanStatusMessage = "Consulting Gemini AI market pricing ledger...") }
            kotlinx.coroutines.delay(1000)

            val added = repository.addNewCollectible(title, category, description, imageType)

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
            val currCode = _uiState.value.selectedCurrency.code
            repository.createEscrow(item, buyerName, currCode, feePercentage)
            _uiState.update { it.copy(showEscrowDialog = false) }
        }
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

    fun generateActivityReport(): String {
        return repository.generateActivityReportText(allItems.value, allEscrows.value)
    }
}
