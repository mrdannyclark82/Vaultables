package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.EscrowTransaction

private val TRACKING_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9 -]{3,63}$")

/** Returns null when valid, otherwise a user-facing error. Mirrors functions validateShipping. */
fun shipmentInputError(carrier: String, tracking: String): String? = when {
    carrier.isBlank() -> "Enter the carrier."
    !TRACKING_PATTERN.matches(tracking.trim()) -> "Enter the carrier's tracking number."
    tracking.contains("PENDING", ignoreCase = true) ||
        tracking.trim().startsWith("TRK-VAULT", ignoreCase = true) -> "Enter a real tracking number."
    else -> null
}

/** Seller must supply real carrier + tracking before the escrow is marked shipped. */
@Composable
fun ShipEscrowDialog(
    escrow: EscrowTransaction,
    onConfirm: (carrier: String, trackingNumber: String) -> Unit,
    onDismiss: () -> Unit
) {
    var carrier by remember(escrow.serverEscrowId) { mutableStateOf(escrow.shippingCarrier) }
    var tracking by remember(escrow.serverEscrowId) { mutableStateOf(escrow.trackingNumber) }
    val error = shipmentInputError(carrier, tracking)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("ship_escrow_dialog"),
        title = { Text("Mark as shipped") },
        text = {
            Column {
                OutlinedTextField(
                    value = carrier,
                    onValueChange = { carrier = it.take(64) },
                    label = { Text("Carrier (e.g. UPS, USPS, FedEx)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("ship_carrier_field")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tracking,
                    onValueChange = { tracking = it.take(64) },
                    label = { Text("Tracking number") },
                    singleLine = true,
                    isError = tracking.isNotBlank() && error != null,
                    modifier = Modifier.fillMaxWidth().testTag("ship_tracking_field")
                )
                if (tracking.isNotBlank() && error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(carrier.trim(), tracking.trim()) },
                enabled = error == null,
                modifier = Modifier.testTag("ship_confirm_button")
            ) { Text("Confirm shipment") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
