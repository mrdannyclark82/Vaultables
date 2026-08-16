package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.remote.ScanDraft
import com.example.ui.theme.GoldAccent

@Composable
fun ScanReviewDialog(
    draft: ScanDraft,
    onDismiss: () -> Unit,
    onSave: (title: String, brand: String, year: String) -> Unit
) {
    var title by remember { mutableStateOf(draft.title) }
    var brand by remember { mutableStateOf(draft.brand) }
    var year by remember { mutableStateOf(draft.year) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Review before save", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Nothing is in your vault until you save. Gemini may be wrong. CardSight and price search are not live.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand / set") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Year") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Grade: ${draft.grade.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall)
                if (draft.cardNumber.isNotBlank()) {
                    Text("Number: ${draft.cardNumber}", style = MaterialTheme.typography.bodySmall)
                }
                if (draft.gradingCompany.isNotBlank()) {
                    Text("Slab: ${draft.gradingCompany} ${draft.certSerialNumber}", style = MaterialTheme.typography.bodySmall)
                }
                if (draft.verificationSummary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(draft.verificationSummary, style = MaterialTheme.typography.bodySmall)
                }
                draft.notices.forEach { notice ->
                    Text(notice, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title.trim(), brand.trim(), year.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
            ) {
                Text("Save to vault", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Discard")
            }
        }
    )
}
