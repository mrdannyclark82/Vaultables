package com.example.ui.components

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.CollectibleCategory
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.GoldAccent
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScannerModal(
    isScanning: Boolean,
    scanMessage: String,
    onDismiss: () -> Unit,
    onConfirmScan: (title: String, category: String, desc: String, imageType: String, brand: String, year: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var titleInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CollectibleCategory.TRADING_CARDS) }
    var brandInput by remember { mutableStateOf("") }
    var yearInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var isLiveCameraActive by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    AlertDialog(
        onDismissRequest = { if (!isScanning) onDismiss() },
        modifier = modifier.testTag("ai_scanner_modal"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = "AI Scanner",
                    tint = GoldAccent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Live Camera Item Scanner",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (isScanning) {
                    // Scanning Animation View
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(1.dp, GoldAccent, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Scanner Viewport Box
                        Box(
                            modifier = Modifier
                                .size(160.dp, 160.dp)
                                .border(1.5.dp, GoldAccent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        )

                        // Animated Laser Line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .offset(y = laserY.dp - 90.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, GoldAccent, EmeraldVerified, GoldAccent, Color.Transparent)
                                    )
                                )
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = GoldAccent,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = scanMessage,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Point camera at item or input details. Gemini AI will inspect texture, alignment, and fetch cloud valuation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Camera Viewport Preview Frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .border(1.dp, GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLiveCameraActive) {
                            CameraScanPreviewView(
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera placeholder",
                                tint = GoldAccent.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        // Scanning Reticle Overlay
                        Box(
                            modifier = Modifier
                                .size(130.dp, 130.dp)
                                .border(1.dp, GoldAccent, RoundedCornerShape(8.dp))
                        )

                        // Laser Sweep Animation
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .offset(y = laserY.dp - 90.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, GoldAccent, EmeraldVerified, Color.Transparent)
                                    )
                                )
                        )

                        // Viewfinder badge
                        Surface(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(EmeraldVerified, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CAMERA LIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = GoldAccent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Identifier",
                                tint = GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Intelligent Optical Identifier Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent
                                )
                                Text(
                                    text = "Auto-extracts Subject Name, Brand (e.g. Fleer/Topps) & Year (e.g. 1986)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Item Title / Search Query") },
                        placeholder = { Text("e.g. Michael Jordan 1986, Charizard, Rolex Daytona") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scanner_title_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Brand & Year Optional Fields Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = brandInput,
                            onValueChange = { brandInput = it },
                            label = { Text("Brand (Optional)") },
                            placeholder = { Text("e.g. Fleer, Topps, Rolex") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = yearInput,
                            onValueChange = { yearInput = it },
                            label = { Text("Year (Optional)") },
                            placeholder = { Text("e.g. 1986") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Category chips row
                    ScrollableTabRow(
                        selectedTabIndex = CollectibleCategory.values().indexOf(selectedCategory),
                        edgePadding = 0.dp,
                        indicator = {},
                        divider = {}
                    ) {
                        CollectibleCategory.values().forEach { cat ->
                            val isSelected = cat == selectedCategory
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.displayName, fontSize = 11.sp) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Condition Notes / Details") },
                        placeholder = { Text("e.g. Clean corners, centered holographic print") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scanner_desc_input")
                    )
                }
            }
        },
        confirmButton = {
            if (!isScanning) {
                Button(
                    onClick = {
                        val finalTitle = titleInput.ifBlank { "${selectedCategory.displayName} Collectible" }
                        onConfirmScan(
                            finalTitle,
                            selectedCategory.displayName,
                            descInput.ifBlank { "Scanned via Live Camera & Gemini AI Optical Identifier" },
                            selectedCategory.name,
                            brandInput,
                            yearInput
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    modifier = Modifier.testTag("start_ai_scan_button")
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Scan", tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan & Catalog", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isScanning) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScanPreviewView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    if (cameraPermissionState.status.isGranted) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview
                        )
                    } catch (exc: Exception) {
                        Log.e("CameraScanPreview", "Camera bind exception", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier
                .background(Color(0xFF1E1E1E))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera",
                    tint = GoldAccent,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Camera access required",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Enable Camera", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
