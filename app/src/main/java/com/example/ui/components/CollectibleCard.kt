package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.data.model.CollectibleItem
import com.example.ui.theme.BlueEscrow
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.GoldAccent

@Composable
fun CollectibleCard(
    item: CollectibleItem,
    formattedPrice: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("collectible_card_${item.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Image Placeholder / Icon Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    ),
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
                    // Category Icon Illustration
                    val categoryIcon = getCategoryIcon(item.category)
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = item.category,
                        modifier = Modifier.size(48.dp),
                        tint = GoldAccent.copy(alpha = 0.85f)
                    )
                }

                // Top Left Authenticity Tag
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(1.dp, EmeraldVerified, RoundedCornerShape(20.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified",
                        modifier = Modifier.size(11.dp),
                        tint = EmeraldVerified
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${item.authenticityScore}% AI Auth",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Top Right Category Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = item.category,
                        color = GoldAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Condition Grade (Replaces cert serial # & seller for cleaner look)
            Surface(
                color = GoldAccent.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "Grade: ${item.conditionGrade}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Price & Horizontal Escrow Bar Below Value
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Est. Value",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Text(
                    text = formattedPrice,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldVerified
                )

                if (item.isListedForSale) {
                    Spacer(modifier = Modifier.height(4.dp))
                    // Horizontal sleek pill below value
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(BlueEscrow.copy(alpha = 0.12f))
                            .border(1.dp, BlueEscrow.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Escrow",
                            modifier = Modifier.size(10.dp),
                            tint = BlueEscrow
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Escrow Ready",
                            color = BlueEscrow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category.uppercase()) {
        "CARDS", "TRADING CARDS" -> Icons.Default.Style
        "POKÉMON & TCG", "POKEMON & TCG", "POKEMON" -> Icons.Default.AutoAwesome
        "DIECAST & MODELS", "DIECAST" -> Icons.Default.DirectionsCar
        "APPAREL & STREETWEAR", "CLOTHING" -> Icons.Default.Checkroom
        "TRENDING & POP CULTURE", "TRENDING" -> Icons.Default.Whatshot
        "COMICS", "COMIC BOOKS" -> Icons.Default.Book
        "WATCHES", "LUXURY WATCHES" -> Icons.Default.Watch
        "SNEAKERS" -> Icons.Default.DirectionsRun
        "COINS", "COINS & BULLION" -> Icons.Default.MonetizationOn
        "ART", "FINE ART" -> Icons.Default.Palette
        else -> Icons.Default.Toys
    }
}
