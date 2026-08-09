package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.data.model.CollectibleCategory
import com.example.data.model.CollectibleItem
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.BlueEscrow
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.GoldAccent

@Composable
fun MarketplaceScreen(
    listings: List<CollectibleItem>,
    selectedCategory: String? = null,
    selectedSubcategory: String? = null,
    onCategorySelect: (String?) -> Unit = {},
    onSubcategorySelect: (String?) -> Unit = {},
    onItemClick: (CollectibleItem) -> Unit,
    onEscrowBuyClick: (CollectibleItem) -> Unit,
    formatPrice: (Double) -> String,
    modifier: Modifier = Modifier
) {
    val filteredListings = listings.filter { item ->
        val matchesCategory = selectedCategory == null || item.category.equals(selectedCategory, ignoreCase = true)
        val matchesSubcategory = selectedSubcategory == null ||
                selectedSubcategory.startsWith("All", ignoreCase = true) ||
                item.subcategory.equals(selectedSubcategory, ignoreCase = true) ||
                item.title.contains(selectedSubcategory, ignoreCase = true)
        matchesCategory && matchesSubcategory
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Marketplace Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(BlueEscrow.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = "Marketplace",
                        tint = BlueEscrow,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Secure Escrow Exchange",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "100% AI Authenticated Goods • 2.5% Escrow Protection",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Chips
        ScrollableTabRow(
            selectedTabIndex = if (selectedCategory == null) 0 else 1,
            edgePadding = 0.dp,
            divider = {},
            indicator = {}
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelect(null) },
                label = { Text("All Market (${listings.size})", fontSize = 12.sp) },
                modifier = Modifier.padding(end = 6.dp)
            )

            CollectibleCategory.values().forEach { cat ->
                val count = listings.count { it.category.equals(cat.displayName, ignoreCase = true) }
                FilterChip(
                    selected = selectedCategory.equals(cat.displayName, ignoreCase = true),
                    onClick = { onCategorySelect(cat.displayName) },
                    label = { Text("${cat.displayName} ($count)", fontSize = 12.sp) },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }

        // Active Category Subcategory Row
        val activeCategoryEnum = CollectibleCategory.values().find { it.displayName.equals(selectedCategory, ignoreCase = true) }
        if (activeCategoryEnum != null && activeCategoryEnum.subcategories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            ScrollableTabRow(
                selectedTabIndex = 0,
                edgePadding = 0.dp,
                divider = {},
                indicator = {}
            ) {
                activeCategoryEnum.subcategories.forEach { sub ->
                    val isSelected = selectedSubcategory.equals(sub, ignoreCase = true) ||
                            (selectedSubcategory == null && sub.startsWith("All", ignoreCase = true))
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSubcategorySelect(if (sub.startsWith("All", ignoreCase = true)) null else sub) },
                        label = {
                            Text(
                                text = sub,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldAccent,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredListings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No items currently listed for sale in this filter.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredListings) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onItemClick(item) }
                            .testTag("marketplace_item_${item.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Category thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(item.category),
                                        contentDescription = item.category,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Seller Star Rating
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Rating",
                                            tint = GoldAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "${item.ownerRating} ★ ${item.ownerName}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = item.conditionGrade,
                                            fontSize = 11.sp,
                                            color = GoldAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Sale Price",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatPrice(if (item.salePriceUsd > 0) item.salePriceUsd else item.estimatedValueUsd),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = EmeraldVerified
                                    )
                                }

                                Button(
                                    onClick = { onEscrowBuyClick(item) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BlueEscrow),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("buy_escrow_button_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Escrow",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Buy with Escrow", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
