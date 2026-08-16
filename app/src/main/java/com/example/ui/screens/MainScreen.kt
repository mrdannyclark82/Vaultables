package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.*
import com.example.ui.theme.BlueEscrow
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodel.VaultViewModel
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allItems by viewModel.allItems.collectAsStateWithLifecycle()
    val marketplaceListings by viewModel.marketplaceListings.collectAsStateWithLifecycle()
    val allEscrows by viewModel.allEscrows.collectAsStateWithLifecycle()
    val allMessages by viewModel.allMessages.collectAsStateWithLifecycle()
    val allAlerts by viewModel.allAlerts.collectAsStateWithLifecycle()
    val allReviews by viewModel.allReviews.collectAsStateWithLifecycle()

    val unreadAlertsCount = allAlerts.count { !it.isRead }
    val paymentSheet = rememberPaymentSheet(
        paymentResultCallback = { paymentResult ->
            when (paymentResult) {
                is PaymentSheetResult.Completed -> {
                    viewModel.onPaymentSheetResult(true, uiState.selectedItemForDetail, uiState.currentUser.displayName ?: "Buyer")
                    Toast.makeText(context, "Payment Successful!", Toast.LENGTH_SHORT).show()
                }
                is PaymentSheetResult.Canceled -> {
                    viewModel.clearPaymentError()
                    Toast.makeText(context, "Payment Canceled", Toast.LENGTH_SHORT).show()
                }
                is PaymentSheetResult.Failed -> {
                    viewModel.onPaymentSheetResult(false, null, "")
                    Toast.makeText(context, "Payment Failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    )

    LaunchedEffect(uiState.paymentClientSecret) {
        uiState.paymentClientSecret?.let { secret ->
            val configuration = PaymentSheet.Configuration(
                merchantDisplayName = "Vaultables Escrow",
                googlePay = PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = "US",
                    currencyCode = uiState.selectedCurrency.code
                )
            )
            paymentSheet.presentWithPaymentIntent(secret, configuration)
        }
    }

    LaunchedEffect(uiState.paymentError) {
        uiState.paymentError?.let { error ->
            Toast.makeText(context, "Escrow Error: $error", Toast.LENGTH_LONG).show()
            viewModel.clearPaymentError()
        }
    }


    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.selectedItemForDetail != null) {
            ItemDetailScreen(
                item = uiState.selectedItemForDetail!!,
                formattedPrice = viewModel.formatPrice(uiState.selectedItemForDetail!!.estimatedValueUsd),
                onBack = { viewModel.selectItemForDetail(null) },
                onBuyEscrow = {
                    viewModel.setShowEscrowDialog(true)
                }
            )
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(GoldAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Vault Logo",
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "VAULTABLES",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Authentic Collectibles Exchange & AI Verification",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        actions = {
                            // Google User Account Profile Button
                            IconButton(
                                onClick = { viewModel.setShowAuthModal(true) },
                                modifier = Modifier.testTag("top_bar_auth_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(GoldAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (uiState.currentUser.displayName.isNotBlank()) uiState.currentUser.displayName.take(1).uppercase() else "G",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.Black
                                    )
                                }
                            }

                            // Real-time Trade Alerts Notification Icon Button
                            IconButton(
                                onClick = { viewModel.setShowNotificationDrawer(true) },
                                modifier = Modifier.testTag("notification_bell_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadAlertsCount > 0) {
                                            Badge(containerColor = GoldAccent) {
                                                Text(
                                                    text = unreadAlertsCount.toString(),
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Alerts"
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        NavigationBarItem(
                            selected = uiState.activeTab == 0,
                            onClick = { viewModel.setActiveTab(0) },
                            icon = { Icon(imageVector = Icons.Default.Inventory, contentDescription = "My Vault") },
                            label = { Text("My Vault") },
                            modifier = Modifier.testTag("nav_tab_vault")
                        )
                        NavigationBarItem(
                            selected = uiState.activeTab == 1,
                            onClick = { viewModel.setActiveTab(1) },
                            icon = { Icon(imageVector = Icons.Default.Storefront, contentDescription = "Marketplace") },
                            label = { Text("Market") },
                            modifier = Modifier.testTag("nav_tab_marketplace")
                        )
                        NavigationBarItem(
                            selected = uiState.activeTab == 2,
                            onClick = { viewModel.setActiveTab(2) },
                            icon = { Icon(imageVector = Icons.Default.Groups, contentDescription = "Community") },
                            label = { Text("Feed") },
                            modifier = Modifier.testTag("nav_tab_community")
                        )
                        NavigationBarItem(
                            selected = uiState.activeTab == 3,
                            onClick = { viewModel.setActiveTab(3) },
                            icon = { Icon(imageVector = Icons.Default.Lock, contentDescription = "E2EE Chat") },
                            label = { Text("E2EE Chat") },
                            modifier = Modifier.testTag("nav_tab_chat")
                        )
                        NavigationBarItem(
                            selected = uiState.activeTab == 4,
                            onClick = { viewModel.setActiveTab(4) },
                            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Hub") },
                            label = { Text("Hub") },
                            modifier = Modifier.testTag("nav_tab_hub")
                        )
                    }
                },
                floatingActionButton = {
                    if (uiState.activeTab == 0 || uiState.activeTab == 1) {
                        FloatingActionButton(
                            onClick = { viewModel.setShowAiScanner(true) },
                            containerColor = GoldAccent,
                            contentColor = Color.Black,
                            shape = CircleShape,
                            modifier = Modifier.testTag("ai_scanner_fab")
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Scan Item")
                        }
                    }
                }
            ) { innerPadding ->
                val modifierWithPadding = Modifier.padding(innerPadding)

                when (uiState.activeTab) {
                    0 -> MyVaultScreen(
                        items = allItems,
                        selectedCategory = uiState.selectedCategory,
                        selectedSubcategory = uiState.selectedSubcategory,
                        searchQuery = uiState.searchQuery,
                        selectedCurrency = uiState.selectedCurrency,
                        onCategorySelect = { viewModel.selectCategory(it) },
                        onSubcategorySelect = { viewModel.selectSubcategory(it) },
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onCurrencySelect = { viewModel.selectCurrency(it) },
                        onItemClick = { viewModel.selectItemForDetail(it) },
                        onScanClick = { viewModel.setShowAiScanner(true) },
                        formatPrice = { viewModel.formatPrice(it) },
                        modifier = modifierWithPadding
                    )

                    1 -> MarketplaceScreen(
                        listings = marketplaceListings,
                        selectedCategory = uiState.selectedCategory,
                        selectedSubcategory = uiState.selectedSubcategory,
                        onCategorySelect = { viewModel.selectCategory(it) },
                        onSubcategorySelect = { viewModel.selectSubcategory(it) },
                        onItemClick = { viewModel.selectItemForDetail(it) },
                        onEscrowBuyClick = {
                            viewModel.selectItemForDetail(it)
                            viewModel.setShowEscrowDialog(true)
                        },
                        formatPrice = { viewModel.formatPrice(it) },
                        modifier = modifierWithPadding
                    )

                    2 -> CommunityFeedScreen(
                        reviews = allReviews,
                        modifier = modifierWithPadding
                    )

                    3 -> EncryptedChatScreen(
                        messages = allMessages,
                        onSendMessage = { text -> viewModel.sendChatMessage(text, "Trader") },
                        modifier = modifierWithPadding
                    )

                    4 -> ProfileHubScreen(
                        isDarkMode = uiState.isDarkMode,
                        isBiometricLocked = uiState.isBiometricLocked,
                        selectedCurrency = uiState.selectedCurrency,
                        escrows = allEscrows,
                        currentUser = uiState.currentUser,
                        onToggleDarkMode = { viewModel.toggleDarkMode() },
                        onToggleBiometric = { viewModel.toggleBiometricLock(it) },
                        onSelectCurrency = { viewModel.selectCurrency(it) },
                        onExportReport = { viewModel.setShowActivityReportDialog(true) },
                        onViewShippingLabel = { viewModel.setShowShippingLabelDialog(true, it) },
                        onConfirmBuyer = { viewModel.confirmEscrowBuyer(it) },
                        onConfirmSeller = { viewModel.confirmEscrowSeller(it) },
                        onOpenAuthModal = { viewModel.setShowAuthModal(true) },
                        onOpenReferralModal = { viewModel.setShowReferralModal(true) },
                        onExportCsv = { viewModel.exportPortfolioCsv(context) },
                        onClearMockData = { viewModel.clearMockDataForProduction() },
                        formatPrice = { viewModel.formatPrice(it) },
                        modifier = modifierWithPadding
                    )
                }
            }
        }

        // Dialog Modals & Overlays
        if (uiState.showReferralModal) {
            ReferralModalDialog(
                referralState = uiState.referralState,
                onDismiss = { viewModel.setShowReferralModal(false) },
                onClaimReward = { email -> viewModel.claimReferralReward(email) }
            )
        }
        if (uiState.showAuthModal) {
            AuthModalDialog(
                currentUser = uiState.currentUser,
                onDismiss = { viewModel.setShowAuthModal(false) },
                onSignInWithGoogle = { viewModel.signInWithGoogle(context) },
                onSignOut = { viewModel.signOut(context) },
                onClearMockData = { viewModel.clearMockDataForProduction() }
            )
        }
        if (uiState.showAiScannerDialog) {
            AiScannerModal(
                isScanning = uiState.isScanningInProgress,
                scanMessage = uiState.scanStatusMessage,
                onDismiss = { viewModel.setShowAiScanner(false) },
                onConfirmScan = { title, cat, desc, imgType, brand, year, localImagePath, localBackImagePath ->
                    viewModel.scanAndAddCollectible(
                        title,
                        cat,
                        desc,
                        imgType,
                        brand,
                        year,
                        localImagePath,
                        localBackImagePath
                    )
                }
            )
        }

        if (uiState.showEscrowDialog) {
            EscrowTradeDialog(
                item = uiState.selectedItemForDetail,
                selectedCurrency = uiState.selectedCurrency,
                onDismiss = { viewModel.setShowEscrowDialog(false) },
                onConfirmTrade = { item, buyer, feePct ->
                    viewModel.createEscrowTrade(item, buyer, feePct)
                },
                formatPrice = { viewModel.formatPrice(it) }
            )
        }

        if (uiState.showShippingLabelDialog) {
            ShippingLabelDialog(
                escrow = uiState.selectedEscrowForLabel,
                onDismiss = { viewModel.setShowShippingLabelDialog(false) }
            )
        }

        if (uiState.showNotificationDrawer) {
            NotificationDrawer(
                alerts = allAlerts,
                onMarkAllRead = { viewModel.markAllNotificationsRead() },
                onDismiss = { viewModel.setShowNotificationDrawer(false) }
            )
        }

        if (uiState.showActivityReportDialog) {
            ActivityReportDialog(
                reportText = viewModel.generateActivityReport(),
                onDismiss = { viewModel.setShowActivityReportDialog(false) }
            )
        }

        // Biometric Security Lock Screen Overlay
        BiometricOverlay(
            isLocked = uiState.isBiometricLocked,
            onUnlock = { viewModel.toggleBiometricLock(false) }
        )
    }
}
