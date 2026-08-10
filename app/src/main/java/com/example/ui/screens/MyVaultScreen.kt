package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CollectibleCategory
import com.example.data.model.CollectibleItem
import com.example.data.model.CurrencyCode
import com.example.ui.components.CollectibleCard
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.GoldAccent

@Composable
fun MyVaultScreen(
    items: List<CollectibleItem>,
    selectedCategory: String?,
    selectedSubcategory: String? = null,
    searchQuery: String,
    selectedCurrency: CurrencyCode,
    onCategorySelect: (String?) -> Unit,
    onSubcategorySelect: (String?) -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onCurrencySelect: (CurrencyCode) -> Unit,
    onItemClick: (CollectibleItem) -> Unit,
    onScanClick: () -> Unit,
    formatPrice: (Double) -> String,
    modifier: Modifier = Modifier
) {
    val filteredItems = items.filter { item ->
        val matchesCategory = selectedCategory == null || item.category.equals(selectedCategory, ignoreCase = true)
        val matchesSubcategory = selectedSubcategory == null ||
                selectedSubcategory.startsWith("All", ignoreCase = true) ||
                item.subcategory.equals(selectedSubcategory, ignoreCase = true) ||
                item.title.contains(selectedSubcategory, ignoreCase = true) ||
                item.description.contains(selectedSubcategory, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.vaultHashId.contains(searchQuery, ignoreCase = true) ||
                item.certSerialNumber.contains(searchQuery, ignoreCase = true) ||
                item.conditionGrade.contains(searchQuery, ignoreCase = true) ||
                item.subcategory.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSubcategory && matchesSearch
    }

    val totalPortfolioUsd = items.sumOf { it.estimatedValueUsd }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Portfolio Hero Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                GoldAccent.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .border(1.dp, GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Vault",
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "VAULTABLES PORTFOLIO VALUATION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                letterSpacing = 1.sp
                            )
                        }

                        // Currency Selector Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CurrencyCode.values().forEach { curr ->
                                val isSelected = curr == selectedCurrency
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) GoldAccent else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (isSelected) GoldAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onCurrencySelect(curr) }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = curr.symbol,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = formatPrice(totalPortfolioUsd),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Up",
                            tint = EmeraldVerified,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+8.4% past 30 days • ${items.size} Verified Vaultables Items",
                            fontSize = 12.sp,
                            color = EmeraldVerified,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search title or Vault Hash ID...", fontSize = 13.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vault_search_input")
        )

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
                label = { Text("All Items (${items.size})", fontSize = 12.sp) },
                modifier = Modifier.padding(end = 6.dp)
            )

            CollectibleCategory.values().forEach { cat ->
                val count = items.count { it.category.equals(cat.displayName, ignoreCase = true) }
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

        Spacer(modifier = Modifier.height(12.dp))

        // Grid Content
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = "Empty",
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No collectibles found in this filter.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onScanClick,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Your First Item", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredItems) { item ->
                    CollectibleCard(
                        item = item,
                        formattedPrice = formatPrice(item.estimatedValueUsd),
                        onClick = { onItemClick(item) }
                    )
                }
            }
        }
    }
}
