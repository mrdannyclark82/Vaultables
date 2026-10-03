package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.CurrencyCode
import com.example.data.model.EscrowStatus
import com.example.data.model.EscrowTransaction
import com.example.ui.theme.BlueEscrow
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.GoldAccent

import com.example.data.remote.UserAccount

@Composable
fun ProfileHubScreen(
    isDarkMode: Boolean,
    isBiometricLocked: Boolean,
    selectedCurrency: CurrencyCode,
    escrows: List<EscrowTransaction>,
    currentUser: UserAccount = UserAccount(),
    onToggleDarkMode: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onSelectCurrency: (CurrencyCode) -> Unit,
    onExportReport: () -> Unit,
    onViewShippingLabel: (EscrowTransaction) -> Unit,
    sellerConnectConnected: Boolean = false,
    sellerConnectWarning: String? = null,
    onConfirmBuyer: (EscrowTransaction) -> Unit,
    onShipSeller: (EscrowTransaction) -> Unit,
    onOpenDispute: (EscrowTransaction) -> Unit = {},
    onStartSellerConnect: () -> Unit = {},
    onOpenAuthModal: () -> Unit = {},
    onOpenReferralModal: () -> Unit = {},
    onExportCsv: () -> Unit = {},
    onClearMockData: () -> Unit = {},
    formatPrice: (Double) -> String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // User Collector Profile Card (Google OAuth Identity Integration)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenAuthModal() }
                .testTag("user_profile_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(GoldAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (currentUser.displayName.isNotBlank()) currentUser.displayName.take(1).uppercase() else "G",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (currentUser.displayName.isNotBlank()) currentUser.displayName else "Google Collector",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Collector",
                            tint = EmeraldVerified,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (currentUser.email.isNotBlank()) currentUser.email else "Tap to sign in with Google OAuth",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Verified Collector Badges Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(currentUser.authProvider, "Top Escrow Trader", "App Store Ready").forEach { badge ->
                            Surface(
                                color = GoldAccent.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = badge,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "OAuth Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Referral & Rewards Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenReferralModal() }
                .testTag("referral_program_banner"),
            colors = CardDefaults.cardColors(containerColor = GoldAccent.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(GoldAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = "Referral Reward",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Referral Rewards Program",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Get 1 Month Free Subscription when referee completes paid trade",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = GoldAccent,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Claim",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Portfolio Category Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vaultables Portfolio Breakdown",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Level 8 Vaultables Master",
                        fontSize = 11.sp,
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                listOf(
                    Triple("Trading Cards", "35%", EmeraldVerified),
                    Triple("Pokémon & TCG", "25%", GoldAccent),
                    Triple("Trending & Pop Culture", "20%", BlueEscrow),
                    Triple("Apparel & Diecast", "20%", MaterialTheme.colorScheme.primary)
                ).forEach { (catName, pct, color) ->
                    Column(modifier = Modifier.padding(vertical = 3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(catName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(pct, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = { pct.replace("%", "").toFloat() / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Security & Preferences",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Security Toggles
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                // Biometric Auth Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Fingerprint, contentDescription = "Biometric", tint = GoldAccent)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Biometric Authentication Lock", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(text = "Require Fingerprint/Face ID", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = isBiometricLocked,
                        onCheckedChange = onToggleBiometric,
                        modifier = Modifier.testTag("biometric_toggle_switch")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                // Dark Mode Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Dark Mode",
                            tint = GoldAccent
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Dark Mode Interface", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onToggleDarkMode() },
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                // User Sales & Net Earnings Analytics Section
                var showEarningsDetails by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEarningsDetails = !showEarningsDetails }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("seller_earnings_row"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = "Earnings", tint = EmeraldVerified)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Seller Performance & Net Earnings", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(text = "Total gross sales, shipping fees & net profits", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(
                        imageVector = if (showEarningsDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle"
                    )
                }

                if (showEarningsDetails) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "NET EARNINGS OVERVIEW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Gross Sales Volume", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "$285.00", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                            }

                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "To", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Net Profit (After Fees & Shipping)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "$258.00", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldVerified)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        Spacer(modifier = Modifier.height(10.dp))

                        // Deductions breakdown
                        listOf(
                            Triple("Gross Sales (2 Completed Trades)", "$285.00", MaterialTheme.colorScheme.onSurface),
                            Triple("Vault Escrow Fee (2.5%)", "-$7.13", MaterialTheme.colorScheme.onSurfaceVariant),
                            Triple("Insured Tracked Shipping Labels", "-$19.87", MaterialTheme.colorScheme.onSurfaceVariant),
                            Triple("Net Payout Earned", "$258.00", EmeraldVerified)
                        ).forEach { (label, amount, color) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = label, fontSize = 12.sp, fontWeight = if (color == EmeraldVerified) FontWeight.Bold else FontWeight.Normal)
                                Text(text = amount, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stripe Connect payout setup
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stripe_connect_card"),
                            color = if (sellerConnectConnected) EmeraldVerified.copy(alpha = 0.12f) else GoldAccent.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (sellerConnectConnected) "Stripe Connect linked" else "Stripe Connect required for payouts",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sellerConnectConnected) EmeraldVerified else GoldAccent
                                    )
                                    Text(
                                        text = sellerConnectWarning
                                            ?: if (sellerConnectConnected) "Escrow releases can transfer to your connected account."
                                            else "Without Connect, buyer confirmation leaves funds held (release blocked).",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (!sellerConnectConnected) {
                                    Button(
                                        onClick = onStartSellerConnect,
                                        colors = ButtonDefaults.buttonColors(containerColor = BlueEscrow),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("stripe_connect_button")
                                    ) {
                                        Text("Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                // Export Activity PDF Report Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportReport() }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("export_activity_report_row"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF Report", tint = GoldAccent)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Export Inventory & Activity Report", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(text = "Generate PDF for offline tracking", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                // Export Vault CSV Ledger Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportCsv() }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("export_csv_ledger_row"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.TableChart, contentDescription = "CSV Ledger", tint = EmeraldVerified)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Export Vaultables Insurance CSV Ledger", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(text = "Download structured spreadsheet for insurance appraisal & taxes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go")
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Active Escrow Trades & Confirmations",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (escrows.isEmpty()) {
            Text(
                text = "No active escrow trades.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(escrows) { escrow ->
                    val isReleased = escrow.status == EscrowStatus.RELEASED.name

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (escrow.serverEscrowId.isNotBlank()) "ESCROW ${escrow.serverEscrowId.take(12)}" else "ESCROW #${escrow.id}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BlueEscrow
                                )
                                Text(
                                    text = formatPrice(escrow.amountUsd),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldVerified
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = escrow.itemTitle,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "Buyer: ${escrow.buyerName} • Fee (${escrow.feePercentage}%): ${formatPrice(escrow.feeUsd)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "State: ${escrow.status} • Release: ${escrow.releaseStatus}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (escrow.payoutNotice.isNotBlank()) {
                                Text(
                                    text = escrow.payoutNotice,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = { onOpenDispute(escrow) },
                                enabled = !isReleased && escrow.serverEscrowId.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dispute_btn_${escrow.id}")
                            ) {
                                Text("Open Dispute", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            // Dual Confirmation Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Buyer Confirmation Button. Stays available as "Retry Release" when the
                                // server held the payout (seller not payout-ready / transfer failed):
                                // confirm-receipt is idempotent and re-runs prepareRelease server-side.
                                val canRetryRelease = escrow.buyerConfirmed && !isReleased &&
                                    escrow.releaseStatus in setOf("blocked", "release_failed")
                                Button(
                                    onClick = { onConfirmBuyer(escrow) },
                                    enabled = (!escrow.buyerConfirmed || canRetryRelease) && !isReleased && escrow.serverEscrowId.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (escrow.buyerConfirmed) EmeraldVerified else BlueEscrow
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("buyer_confirm_btn_${escrow.id}")
                                ) {
                                    Icon(
                                        imageVector = if (escrow.buyerConfirmed) Icons.Default.CheckCircle else Icons.Default.HowToReg,
                                        contentDescription = "Buyer Confirm",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when {
                                            canRetryRelease -> "Retry Release"
                                            escrow.buyerConfirmed -> "Receipt OK ✓"
                                            else -> "Confirm Receipt"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Seller Confirmation Button
                                Button(
                                    onClick = { onShipSeller(escrow) },
                                    enabled = !escrow.sellerConfirmed && !isReleased && escrow.serverEscrowId.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (escrow.sellerConfirmed) EmeraldVerified else GoldAccent
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("seller_confirm_btn_${escrow.id}")
                                ) {
                                    Icon(
                                        imageVector = if (escrow.sellerConfirmed) Icons.Default.CheckCircle else Icons.Default.TaskAlt,
                                        contentDescription = "Seller Confirm",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (escrow.sellerConfirmed) "Shipped ✓" else "Mark Shipped",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (escrow.sellerConfirmed) Color.White else Color.Black
                                    )
                                }
                            }

                            if (isReleased) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = EmeraldVerified.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = "Released",
                                            tint = EmeraldVerified,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Trade Complete: Net payout released to seller & platform fee credited.",
                                            fontSize = 11.sp,
                                            color = EmeraldVerified,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = { onViewShippingLabel(escrow) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("view_shipping_label_button_${escrow.id}")
                            ) {
                                Icon(imageVector = Icons.Default.LocalShipping, contentDescription = "Label", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("View Shipping Label (${escrow.trackingNumber})", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
