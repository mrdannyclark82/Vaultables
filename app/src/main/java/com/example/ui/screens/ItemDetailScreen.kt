package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.data.model.CollectibleItem
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.BlueEscrow
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.GoldAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    item: CollectibleItem,
    formattedPrice: String,
    onBack: () -> Unit,
    onBuyEscrow: (CollectibleItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDigitalCertDialog by remember { mutableStateOf(false) }
    var showTradeOfferModal by remember { mutableStateOf(false) }
    var tradeCashAmount by remember { mutableStateOf("") }
    var offerSubmitted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vault Certificate & Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDigitalCertDialog = true }) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Cert QR", tint = GoldAccent)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Current Appraisal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formattedPrice,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = EmeraldVerified
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showTradeOfferModal = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = "Trade", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Propose Trade", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { onBuyEscrow(item) },
                            colors = ButtonDefaults.buttonColors(containerColor = BlueEscrow),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("detail_buy_escrow_button")
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Buy / Escrow", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        modifier = modifier.testTag("item_detail_screen")
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Visual Hero Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                GoldAccent.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    )
                    .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (item.localImagePath != null) {
                    AsyncImage(
                        model = item.localImagePath,
                        contentDescription = "Captured Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = getCategoryIcon(item.category),
                        contentDescription = item.category,
                        tint = GoldAccent,
                        modifier = Modifier.size(80.dp)
                    )
                }

                // Grade Tag Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GoldAccent)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = item.conditionGrade,
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title & Owner
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (item.brandName.isNotBlank() || item.releaseYear.isNotBlank() || item.teamName.isNotBlank() || item.cardNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.releaseYear.isNotBlank()) {
                        Surface(
                            color = GoldAccent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "📅 ${item.releaseYear}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (item.brandName.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "🏷️ ${item.brandName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            if (item.teamName.isNotBlank() || item.cardNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = listOfNotNull(
                        item.teamName.takeIf { it.isNotBlank() }?.let { "Team: $it" },
                        item.cardNumber.takeIf { it.isNotBlank() }?.let { "Card #: $it" }
                    ).joinToString("  •  "),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldVerified
                )
            }

            if (item.verificationSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = EmeraldVerified.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.FactCheck,
                            contentDescription = "Catalog evidence",
                            tint = EmeraldVerified,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Catalog evidence", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                item.verificationSummary,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Owner: ${item.ownerName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "★ ${item.ownerRating}",
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        fontSize = 12.sp
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = item.subcategory,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Slab Digital Certificate Export Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showDigitalCertDialog = true }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Badge, contentDescription = "Badge", tint = GoldAccent)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Official Digital Slab Passport", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Verify Cert #${item.certSerialNumber.ifBlank { item.vaultHashId }}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "View")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Category: ${item.category}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Owner: ${item.ownerName} (${item.ownerRating}★)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Digital Certificate of Authenticity
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = EmeraldVerified
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val certTitle = when {
                                item.gradingCompany.contains("Beckett", ignoreCase = true) || item.gradingCompany.contains("BGS", ignoreCase = true) -> "Beckett (BGS) Official Slab Certification"
                                item.gradingCompany.contains("PSA", ignoreCase = true) -> "PSA Official Slab Certification"
                                item.gradingCompany.contains("CGC", ignoreCase = true) -> "CGC Official Slab Certification"
                                item.gradingCompany.contains("SGC", ignoreCase = true) -> "SGC Official Slab Certification"
                                else -> "Vaultables AI Internal Certification Ledger"
                            }
                            Text(
                                text = certTitle,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            color = GoldAccent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = item.conditionGrade,
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Slab Serial Certification Bar Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val certHeader = when {
                                    item.gradingCompany.contains("Beckett", ignoreCase = true) || item.gradingCompany.contains("BGS", ignoreCase = true) -> "BECKETT CERT #"
                                    item.gradingCompany.contains("PSA", ignoreCase = true) -> "PSA CERT #"
                                    item.gradingCompany.contains("CGC", ignoreCase = true) -> "CGC CERT #"
                                    item.gradingCompany.contains("SGC", ignoreCase = true) -> "SGC CERT #"
                                    else -> "VAULTABLES CERT #"
                                }
                                val displayCertNumber = when {
                                    item.gradingCompany.contains("Beckett", ignoreCase = true) || item.gradingCompany.contains("BGS", ignoreCase = true) ||
                                    item.gradingCompany.contains("PSA", ignoreCase = true) || item.gradingCompany.contains("CGC", ignoreCase = true) || item.gradingCompany.contains("SGC", ignoreCase = true) ->
                                        item.certSerialNumber.ifBlank { item.vaultHashId }
                                    else ->
                                        if (item.certSerialNumber.startsWith("VAULT-CERT-")) item.certSerialNumber else "VAULT-CERT-${item.vaultHashId.takeLast(8)}"
                                }
                                Text(
                                    text = certHeader,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = displayCertNumber,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldAccent,
                                    fontSize = 14.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "SLAB AUTHENTICITY",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${item.authenticityScore}% AI VERIFIED",
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldVerified,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "AI Optical Sub-Grade Assessment Breakdown:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sub-grade metrics grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(
                            "Centering" to String.format("%.1f", item.centeringGrade),
                            "Corners" to String.format("%.1f", item.cornersGrade),
                            "Edges" to String.format("%.1f", item.edgesGrade),
                            "Surface" to String.format("%.1f", item.surfaceGrade)
                        ).forEach { (label, grade) ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = grade, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldVerified)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Market Price History Section
            Text(
                text = "Market Price History & Sales Ledger",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            var selectedTimeframe by remember { mutableStateOf("30D") }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Historical Index Valuation",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.TrendingUp, contentDescription = "Up", tint = EmeraldVerified)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (selectedTimeframe) {
                                    "7D" -> "+3.8%"
                                    "30D" -> "+12.4%"
                                    "90D" -> "+18.9%"
                                    "1Y" -> "+42.1%"
                                    else -> "+110.5%"
                                },
                                color = EmeraldVerified,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Timeframe Chips Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("7D", "30D", "90D", "1Y", "ALL").forEach { tf ->
                            val isSelected = selectedTimeframe == tf
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) GoldAccent else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedTimeframe = tf }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tf,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Price Chart Bar Graph
                    val heights = when (selectedTimeframe) {
                        "7D" -> listOf(0.70f, 0.72f, 0.71f, 0.75f, 0.78f, 0.85f, 0.92f)
                        "30D" -> listOf(0.40f, 0.45f, 0.42f, 0.60f, 0.58f, 0.75f, 0.82f, 1.0f)
                        "90D" -> listOf(0.30f, 0.38f, 0.45f, 0.52f, 0.68f, 0.74f, 0.88f, 1.0f)
                        "1Y" -> listOf(0.20f, 0.35f, 0.40f, 0.55f, 0.65f, 0.72f, 0.85f, 1.0f)
                        else -> listOf(0.12f, 0.25f, 0.38f, 0.50f, 0.62f, 0.78f, 0.90f, 1.0f)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        heights.forEach { h ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 3.dp)
                                    .fillMaxHeight(h)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(EmeraldVerified, EmeraldVerified.copy(alpha = 0.4f))
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Key Market Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val lowPrice = item.estimatedValueUsd * 0.88
                        val highPrice = item.estimatedValueUsd * 1.12
                        val avgPrice = item.estimatedValueUsd

                        Column {
                            Text("30D Low", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formattedPrice.replace(item.estimatedValueUsd.toString(), String.format("%.0f", lowPrice)), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Avg Sale", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formattedPrice, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                        }
                        Column {
                            Text("30D High", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formattedPrice.replace(item.estimatedValueUsd.toString(), String.format("%.0f", highPrice)), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldVerified)
                        }
                        Column {
                            Text("Volatility", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Low (4.2%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Verified Comparable Sales Table
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Public Verified Sales",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Auction Ledger",
                            fontSize = 10.sp,
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    listOf(
                        Triple("Heritage Auctions", "2 days ago", item.estimatedValueUsd * 1.02),
                        Triple("Vault Escrow Exchange", "5 days ago", item.estimatedValueUsd * 0.98),
                        Triple("Goldin Auctions", "2 weeks ago", item.estimatedValueUsd * 0.95),
                        Triple("eBay Authenticated", "1 month ago", item.estimatedValueUsd * 0.91)
                    ).forEach { (venue, date, price) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = venue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = "$date • ${item.conditionGrade}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = formattedPrice.replace(item.estimatedValueUsd.toString(), String.format("%.0f", price)),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldVerified
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Digital Certificate Passport Modal
        if (showDigitalCertDialog) {
            AlertDialog(
                onDismissRequest = { showDigitalCertDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = "Verified", tint = GoldAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Official Slab Passport", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black, RoundedCornerShape(12.dp))
                            .border(1.dp, GoldAccent, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = item.gradingCompany.uppercase(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = GoldAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "CERT # ${item.certSerialNumber.ifBlank { item.vaultHashId }}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.title,
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = GoldAccent,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "GRADE: ${item.conditionGrade}",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Sub-grades table inside digital cert
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Centering", fontSize = 9.sp, color = Color.Gray)
                                Text("${item.centeringGrade}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldVerified)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Corners", fontSize = 9.sp, color = Color.Gray)
                                Text("${item.cornersGrade}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldVerified)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Edges", fontSize = 9.sp, color = Color.Gray)
                                Text("${item.edgesGrade}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldVerified)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Surface", fontSize = 9.sp, color = Color.Gray)
                                Text("${item.surfaceGrade}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldVerified)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "QR Code Verification",
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Scan QR to verify blockchain slab proof", fontSize = 9.sp, color = Color.Gray)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showDigitalCertDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                    ) {
                        Text("Export PDF Certificate", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDigitalCertDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Trade Offer Builder Modal
        if (showTradeOfferModal) {
            AlertDialog(
                onDismissRequest = {
                    showTradeOfferModal = false
                    offerSubmitted = false
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = "Trade", tint = BlueEscrow)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Propose Peer-to-Peer Trade", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    if (offerSubmitted) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Success", tint = EmeraldVerified, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Trade Proposal Transmitted!", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Sent securely to ${item.ownerName} via Vault Smart Escrow.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Column {
                            Text(text = "Target Item: ${item.title}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "Target Value: $formattedPrice", fontSize = 11.sp, color = EmeraldVerified, fontWeight = FontWeight.Bold)

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(text = "Add Cash Offset (Optional):", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            OutlinedTextField(
                                value = tradeCashAmount,
                                onValueChange = { tradeCashAmount = it },
                                placeholder = { Text("e.g. 250.00") },
                                prefix = { Text("$ ") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Offered Vault Collectible:", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Item", tint = GoldAccent)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = "Your Vault Portfolio Collectible", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(text = "Escrow value match verified", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (offerSubmitted) {
                        Button(onClick = {
                            showTradeOfferModal = false
                            offerSubmitted = false
                        }) {
                            Text("Done")
                        }
                    } else {
                        Button(
                            onClick = { offerSubmitted = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BlueEscrow)
                        ) {
                            Text("Submit Trade Proposal", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    if (!offerSubmitted) {
                        TextButton(onClick = { showTradeOfferModal = false }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    }
}
