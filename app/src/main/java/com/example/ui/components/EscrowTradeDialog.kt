package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
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
import com.example.data.model.CollectibleItem
import com.example.data.model.CurrencyCode
import com.example.ui.theme.BlueEscrow
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.GoldAccent

@Composable
fun EscrowTradeDialog(
    item: CollectibleItem?,
    selectedCurrency: CurrencyCode,
    onDismiss: () -> Unit,
    onConfirmTrade: (item: CollectibleItem, buyerName: String, feePercentage: Double) -> Unit,
    formatPrice: (Double) -> String,
    modifier: Modifier = Modifier
) {
    if (item == null) return

    var buyerNameInput by remember { mutableStateOf("Vault Collector") }
    var selectedFeePct by remember { mutableStateOf(3.5) } // Default 3.5%
    val availableFeeOptions = listOf(3.0, 3.5, 4.0, 5.0)

    val itemPriceUsd = if (item.salePriceUsd > 0) item.salePriceUsd else item.estimatedValueUsd
    val platformFeeUsd = itemPriceUsd * (selectedFeePct / 100.0)
    val totalAmountUsd = itemPriceUsd + platformFeeUsd

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("escrow_trade_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Escrow",
                        tint = BlueEscrow
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Secure Escrow Trade",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Vault Escrow holds funds safely until both buyer and seller confirm delivery. Platform fee is credited upon release.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Configurable Platform Fee Rate:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableFeeOptions.forEach { feeOption ->
                        val isSelected = feeOption == selectedFeePct
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFeePct = feeOption },
                            label = { Text("${feeOption}%", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldAccent,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = item.title,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Seller: ${item.ownerName} • Grade: ${item.conditionGrade}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Item Price:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = formatPrice(itemPriceUsd), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Platform Fee (${selectedFeePct}%):",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(text = formatPrice(platformFeeUsd), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Total Deposit:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = formatPrice(totalAmountUsd),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = EmeraldVerified
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = buyerNameInput,
                    onValueChange = { buyerNameInput = it },
                    label = { Text("Buyer Name / Handle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmTrade(item, buyerNameInput, selectedFeePct)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BlueEscrow),
                modifier = Modifier.testTag("confirm_escrow_deposit_button")
            ) {
                Icon(imageVector = Icons.Default.Shield, contentDescription = "Deposit")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Lock Funds in Escrow", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
