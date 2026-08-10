package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.GoldAccent

data class ReferralItem(
    val refereeName: String,
    val refereeEmail: String,
    val transactionStatus: String, // "COMPLETED" or "PENDING"
    val transactionAmountUsd: Double,
    val isRewardClaimed: Boolean,
    val dateJoined: String
)

data class ReferralState(
    val referralCode: String = "VAULT-DANNY-2026",
    val freeMonthsEarned: Int = 2,
    val freeMonthsClaimed: Int = 1,
    val referrals: List<ReferralItem> = listOf(
        ReferralItem(
            refereeName = "Sarah Jenkins",
            refereeEmail = "s.jenkins@gmail.com",
            transactionStatus = "COMPLETED",
            transactionAmountUsd = 450.0,
            isRewardClaimed = true,
            dateJoined = "2 days ago"
        ),
        ReferralItem(
            refereeName = "Marcus Vance",
            refereeEmail = "marcus.v@outlook.com",
            transactionStatus = "COMPLETED",
            transactionAmountUsd = 1200.0,
            isRewardClaimed = false,
            dateJoined = "Yesterday"
        ),
        ReferralItem(
            refereeName = "Elena Rostova",
            refereeEmail = "elena.rostova@gmail.com",
            transactionStatus = "PENDING",
            transactionAmountUsd = 0.0,
            isRewardClaimed = false,
            dateJoined = "5 hours ago"
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralModalDialog(
    referralState: ReferralState,
    onDismiss: () -> Unit,
    onClaimReward: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shareLink = "https://vaultcollectibles.app/invite?ref=${referralState.referralCode}"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .testTag("referral_modal_dialog")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GoldAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = "Referral Reward",
                        tint = GoldAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Referrals & Rewards",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Earn 1 Month Free Vault Pro per active transaction",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Reward Card
            Surface(
                color = GoldAccent.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PRO SUBSCRIPTION EARNED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                        Text(
                            text = "${referralState.freeMonthsEarned} Months Free Pro",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Triggered when referee completes first escrow transaction",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Gold Star",
                        tint = GoldAccent,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Unique Referral Code & Copy/Share Box
            Text(
                text = "YOUR EXCLUSIVE REFERRAL LINK",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = referralState.referralCode,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = GoldAccent
                        )
                        Text(
                            text = shareLink,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    Row {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Referral Link", shareLink)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Referral link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Link",
                                tint = GoldAccent
                            )
                        }

                        IconButton(onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Join me on Vaultables Exchange! Use my referral link $shareLink to get $15 escrow credit on your first item authentication.")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Referral Link"))
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "INVITED COLLECTORS (${referralState.referrals.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(referralState.referrals) { referee ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (referee.transactionStatus == "COMPLETED") EmeraldVerified.copy(alpha = 0.2f)
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (referee.transactionStatus == "COMPLETED") Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                        contentDescription = "Status",
                                        tint = if (referee.transactionStatus == "COMPLETED") EmeraldVerified else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = referee.refereeName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (referee.transactionStatus == "COMPLETED") "Completed \$${referee.transactionAmountUsd.toInt()} Escrow Trade • ${referee.dateJoined}"
                                        else "Signed up • Pending 1st paid transaction",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (referee.transactionStatus == "COMPLETED") {
                                if (referee.isRewardClaimed) {
                                    Surface(
                                        color = EmeraldVerified.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "✓ 1Mo Free Claimed",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldVerified,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            onClaimReward(referee.refereeEmail)
                                            Toast.makeText(context, "🎉 +1 Month Free Vault Pro Applied!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Claim +1Mo Pro",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Pending",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Close Referral Hub", fontWeight = FontWeight.Bold)
            }
        }
    }
}
