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
import androidx.compose.runtime.Composable
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

@Composable
fun ProfileHubScreen(
    isDarkMode: Boolean,
    isBiometricLocked: Boolean,
    selectedCurrency: CurrencyCode,
    escrows: List<EscrowTransaction>,
    onToggleDarkMode: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onSelectCurrency: (CurrencyCode) -> Unit,
    onExportReport: () -> Unit,
    onViewShippingLabel: (EscrowTransaction) -> Unit,
    onConfirmBuyer: (Long) -> Unit,
    onConfirmSeller: (Long) -> Unit,
    formatPrice: (Double) -> String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // User Collector Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile Avatar",
                        modifier = Modifier.size(32.dp),
                        tint = Color.Black
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Alex Vance",
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
                        text = "4.95 ★ • 34 Escrow Deals • GDPR Compliant",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Verified Collector Badges Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Verified Vault OG", "Top Escrow Trader", "TCG Master").forEach { badge ->
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
                        text = "Vault Portfolio Breakdown",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Level 8 Vault Master",
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

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

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

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

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
                                    text = "ESCROW #${escrow.id}",
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

                            // Dual Confirmation Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Buyer Confirmation Button
                                Button(
                                    onClick = { onConfirmBuyer(escrow.id) },
                                    enabled = !escrow.buyerConfirmed && !isReleased,
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
                                        text = if (escrow.buyerConfirmed) "Buyer OK ✓" else "Buyer Confirm",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Seller Confirmation Button
                                Button(
                                    onClick = { onConfirmSeller(escrow.id) },
                                    enabled = !escrow.sellerConfirmed && !isReleased,
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
                                        text = if (escrow.sellerConfirmed) "Seller OK ✓" else "Seller Confirm",
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
