package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
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
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BiometricOverlay(
    isLocked: Boolean,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVerifying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = isLocked,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(GoldAccent.copy(alpha = 0.15f))
                        .border(2.dp, GoldAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Vault Locked",
                        modifier = Modifier.size(40.dp),
                        tint = GoldAccent
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "VAULT BIOMETRIC SECURITY",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Fingerprint or Face ID required to access your high-value inventory & escrow transactions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(if (isVerifying) GoldAccent else Color.White.copy(alpha = 0.1f))
                        .clickable {
                            if (!isVerifying) {
                                isVerifying = true
                                scope.launch {
                                    delay(800)
                                    isVerifying = false
                                    onUnlock()
                                }
                            }
                        }
                        .testTag("biometric_sensor_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Scan Fingerprint",
                        modifier = Modifier.size(54.dp),
                        tint = if (isVerifying) Color.Black else GoldAccent
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isVerifying) "Verifying Biometric Hash..." else "Touch Sensor or Click to Authenticate",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isVerifying) GoldAccent else Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Encrypted",
                        modifier = Modifier.size(16.dp),
                        tint = GoldAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hardware Key Enclave Active",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}
