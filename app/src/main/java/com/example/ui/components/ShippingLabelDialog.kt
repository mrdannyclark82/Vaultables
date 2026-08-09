package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EscrowTransaction
import com.example.ui.theme.BlueEscrow
import com.example.ui.theme.GoldAccent

@Composable
fun ShippingLabelDialog(
    escrow: EscrowTransaction?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (escrow == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("shipping_label_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = "Shipping",
                        tint = BlueEscrow
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Automated Shipping Label",
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                // Label Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VAULT LOGISTICS PRIORITY",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "INSURED $25,000",
                        color = BlueEscrow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Black)

                // FROM / TO
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "SHIP FROM:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text(text = escrow.sellerName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text(text = "Vault Verified Sender", fontSize = 10.sp, color = Color.DarkGray)
                        Text(text = "San Francisco, CA 94105", fontSize = 10.sp, color = Color.DarkGray)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "SHIP TO:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text(text = escrow.buyerName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text(text = "Vault Escrow Inspector", fontSize = 10.sp, color = Color.DarkGray)
                        Text(text = "New York, NY 10001", fontSize = 10.sp, color = Color.DarkGray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Item description & Tracking barcode
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9))
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            text = "ITEM: ${escrow.itemTitle}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "ESCROW ID: #${escrow.id} | CARRIER: ${escrow.shippingCarrier}",
                            fontSize = 10.sp,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Barcode simulation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = "QR Code",
                        modifier = Modifier.size(64.dp),
                        tint = Color.Black
                    )

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "TRACKING #:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = escrow.trackingNumber,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                        Text(
                            text = "||||| ||||||| |||| |||||||| |||||",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = BlueEscrow),
                modifier = Modifier.testTag("print_shipping_label_button")
            ) {
                Icon(imageVector = Icons.Default.Print, contentDescription = "Print")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Print / Export Label")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
