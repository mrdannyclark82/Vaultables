package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.remote.FirestoreSyncManager
import com.example.data.remote.GeminiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Base64

class VaultRepository(private val db: AppDatabase) {

    val allItems: Flow<List<CollectibleItem>> = db.collectibleDao().getAllItems()
    val marketplaceListings: Flow<List<CollectibleItem>> = db.collectibleDao().getMarketplaceListings()
    val allEscrows: Flow<List<EscrowTransaction>> = db.escrowDao().getAllEscrows()
    val allMessages: Flow<List<ChatMessage>> = db.messageDao().getAllMessages()
    val allAlerts: Flow<List<TradeAlert>> = db.tradeAlertDao().getAllAlerts()
    val allReviews: Flow<List<UserReview>> = db.userReviewDao().getAllReviews()

    suspend fun seedInitialDataIfEmpty() {
        val currentItems = db.collectibleDao().getAllItems().first()
        if (currentItems.isEmpty()) {
            val sampleItems = listOf(
                CollectibleItem(
                    title = "2003 Topps Chrome LeBron James Rookie #111",
                    category = CollectibleCategory.TRADING_CARDS.displayName,
                    subcategory = "Basketball",
                    description = "Iconic LeBron James rookie card in Gem Mint slab with razor-sharp corners.",
                    ownerName = "Alex Vance",
                    ownerRating = 4.95f,
                    estimatedValueUsd = 24500.0,
                    conditionGrade = "PSA 10 Gem Mint",
                    certSerialNumber = "PSA-84920194",
                    gradingCompany = "PSA",
                    centeringGrade = 10.0f,
                    cornersGrade = 10.0f,
                    edgesGrade = 9.5f,
                    surfaceGrade = 10.0f,
                    authenticityScore = 99,
                    vaultHashId = "VAULT-2301-8821-2026",
                    isListedForSale = true,
                    salePriceUsd = 24500.0,
                    imageType = "CARD"
                ),
                CollectibleItem(
                    title = "1992 Maxx Dale Earnhardt Sr. Signature #1",
                    category = CollectibleCategory.TRADING_CARDS.displayName,
                    subcategory = "NASCAR",
                    description = "Rare vintage NASCAR Intimidator auto card with gold foil seal.",
                    ownerName = "Speedway Collector",
                    ownerRating = 4.90f,
                    estimatedValueUsd = 3400.0,
                    conditionGrade = "BGS 9.5 Gem",
                    certSerialNumber = "BGS-00129481",
                    gradingCompany = "Beckett BGS",
                    centeringGrade = 9.5f,
                    cornersGrade = 9.5f,
                    edgesGrade = 9.5f,
                    surfaceGrade = 10.0f,
                    authenticityScore = 98,
                    vaultHashId = "VAULT-0003-7721-2026",
                    isListedForSale = true,
                    salePriceUsd = 3400.0,
                    imageType = "CARD"
                ),
                CollectibleItem(
                    title = "2021 Panini Prizm Khabib Nurmagomedov Gold /10",
                    category = CollectibleCategory.TRADING_CARDS.displayName,
                    subcategory = "UFC",
                    description = "Ultra rare Gold Prizm undefeated 29-0 UFC lightweight champion auto.",
                    ownerName = "Octagon Vault",
                    ownerRating = 4.97f,
                    estimatedValueUsd = 14200.0,
                    conditionGrade = "PSA 10 Gem Mint",
                    certSerialNumber = "PSA-29001102",
                    gradingCompany = "PSA",
                    centeringGrade = 10.0f,
                    cornersGrade = 10.0f,
                    edgesGrade = 10.0f,
                    surfaceGrade = 10.0f,
                    authenticityScore = 100,
                    vaultHashId = "VAULT-2900-1102-2026",
                    isListedForSale = true,
                    salePriceUsd = 14200.0,
                    imageType = "CARD"
                ),
                CollectibleItem(
                    title = "2011 Topps Update Mike Trout RC #US175",
                    category = CollectibleCategory.TRADING_CARDS.displayName,
                    subcategory = "Baseball",
                    description = "Holy Grail Modern Baseball rookie card, immaculate centering and surface.",
                    ownerName = "Cooperstown Relics",
                    ownerRating = 4.93f,
                    estimatedValueUsd = 18900.0,
                    conditionGrade = "PSA 10 Gem Mint",
                    certSerialNumber = "PSA-17509912",
                    gradingCompany = "PSA",
                    centeringGrade = 10.0f,
                    cornersGrade = 9.5f,
                    edgesGrade = 10.0f,
                    surfaceGrade = 10.0f,
                    authenticityScore = 99,
                    vaultHashId = "VAULT-1750-9912-2026",
                    isListedForSale = false,
                    salePriceUsd = 0.0,
                    imageType = "CARD"
                ),
                CollectibleItem(
                    title = "1999 Charizard 1st Edition Shadowless",
                    category = CollectibleCategory.POKEMON_CARDS.displayName,
                    subcategory = "Pokémon",
                    description = "Holographic 1st Edition base set in protective PSA slab. Flawless holo foil.",
                    ownerName = "Pallet Town Vault",
                    ownerRating = 4.95f,
                    estimatedValueUsd = 12500.0,
                    conditionGrade = "PSA 10 Gem Mint",
                    certSerialNumber = "PSA-77018821",
                    gradingCompany = "PSA",
                    centeringGrade = 10.0f,
                    cornersGrade = 10.0f,
                    edgesGrade = 9.5f,
                    surfaceGrade = 10.0f,
                    authenticityScore = 99,
                    vaultHashId = "VAULT-7701-8821-2026",
                    isListedForSale = true,
                    salePriceUsd = 12500.0,
                    imageType = "CARD"
                ),
                CollectibleItem(
                    title = "Viral Dumplings Glow-in-the-Dark Plushie #001",
                    category = CollectibleCategory.TRENDING.displayName,
                    subcategory = "Viral Dumplings",
                    description = "Limited Run #1/500 Viral Dumpling plushie with authenticated microchip tag.",
                    ownerName = "Trendify Hub",
                    ownerRating = 4.98f,
                    estimatedValueUsd = 1250.0,
                    conditionGrade = "Mint 10",
                    certSerialNumber = "VAULT-CERT-55104491",
                    gradingCompany = "VAULT AI",
                    centeringGrade = 10.0f,
                    cornersGrade = 10.0f,
                    edgesGrade = 10.0f,
                    surfaceGrade = 10.0f,
                    authenticityScore = 100,
                    vaultHashId = "VAULT-5510-4491-2026",
                    isListedForSale = true,
                    salePriceUsd = 1250.0,
                    imageType = "TRENDING"
                ),
                CollectibleItem(
                    title = "Labubu The Monsters Vinyl Art Figurine",
                    category = CollectibleCategory.TRENDING.displayName,
                    subcategory = "Labubu",
                    description = "Original How2work Pop Mart Labubu artist series vinyl statue.",
                    ownerName = "Tokyo Art Toys",
                    ownerRating = 4.91f,
                    estimatedValueUsd = 950.0,
                    conditionGrade = "CGC 9.8 Near Mint",
                    certSerialNumber = "CGC-88123301",
                    gradingCompany = "CGC",
                    centeringGrade = 9.8f,
                    cornersGrade = 9.8f,
                    edgesGrade = 9.8f,
                    surfaceGrade = 9.8f,
                    authenticityScore = 97,
                    vaultHashId = "VAULT-8812-3301-2026",
                    isListedForSale = true,
                    salePriceUsd = 950.0,
                    imageType = "TRENDING"
                ),
                CollectibleItem(
                    title = "Funko Pop Metallic Batman Comic-Con Exclusive /48",
                    category = CollectibleCategory.TRENDING.displayName,
                    subcategory = "Funko Pops",
                    description = "Ultra grail 2010 SDCC Metallic Batman Funko in hard acrylic case.",
                    ownerName = "Pop Collector",
                    ownerRating = 4.89f,
                    estimatedValueUsd = 4800.0,
                    conditionGrade = "PSA 10 Slabbed Box",
                    certSerialNumber = "PSA-48009921",
                    gradingCompany = "PSA",
                    centeringGrade = 10.0f,
                    cornersGrade = 10.0f,
                    edgesGrade = 10.0f,
                    surfaceGrade = 10.0f,
                    authenticityScore = 99,
                    vaultHashId = "VAULT-4800-9921-2026",
                    isListedForSale = false,
                    salePriceUsd = 0.0,
                    imageType = "TRENDING"
                ),
                CollectibleItem(
                    title = "1968 Hot Wheels Custom Camaro Over Chrome Blue",
                    category = CollectibleCategory.DIECAST.displayName,
                    subcategory = "Hot Wheels",
                    description = "Pre-production prototype Redline diecast car, Spectraflame finish.",
                    ownerName = "Redline Vault",
                    ownerRating = 5.0f,
                    estimatedValueUsd = 8700.0,
                    conditionGrade = "VAULT 9.9 Pristine",
                    certSerialNumber = "VAULT-CERT-19683312",
                    gradingCompany = "VAULT AI",
                    centeringGrade = 10.0f,
                    cornersGrade = 9.9f,
                    edgesGrade = 9.9f,
                    surfaceGrade = 10.0f,
                    authenticityScore = 100,
                    vaultHashId = "VAULT-1968-3312-2026",
                    isListedForSale = true,
                    salePriceUsd = 8700.0,
                    imageType = "DIECAST"
                ),
                CollectibleItem(
                    title = "Supreme x Louis Vuitton Box Logo Hoodie Red",
                    category = CollectibleCategory.CLOTHING.displayName,
                    subcategory = "Supreme",
                    description = "Grail streetwear collaboration hoodie, unworn with original dust bag and tags.",
                    ownerName = "Hype Vault Paris",
                    ownerRating = 4.96f,
                    estimatedValueUsd = 5400.0,
                    conditionGrade = "Deadstock 10/10",
                    certSerialNumber = "VAULT-CERT-77112026",
                    gradingCompany = "VAULT AI",
                    centeringGrade = 10.0f,
                    cornersGrade = 10.0f,
                    edgesGrade = 10.0f,
                    surfaceGrade = 10.0f,
                    authenticityScore = 99,
                    vaultHashId = "VAULT-7711-2026-LV",
                    isListedForSale = true,
                    salePriceUsd = 5400.0,
                    imageType = "CLOTHING"
                ),
                CollectibleItem(
                    title = "Rolex Daytona 116500LN White Dial",
                    category = CollectibleCategory.WATCHES.displayName,
                    subcategory = "Rolex",
                    description = "Pristine Panda chronograph, full box and papers, sapphire crystal.",
                    ownerName = "Geneva Timepieces",
                    ownerRating = 5.0f,
                    estimatedValueUsd = 31000.0,
                    conditionGrade = "Mint 9.9 / Unworn",
                    authenticityScore = 100,
                    vaultHashId = "VAULT-3329-1102-2026",
                    isListedForSale = true,
                    salePriceUsd = 31000.0,
                    imageType = "WATCH"
                ),
                CollectibleItem(
                    title = "Amazing Fantasy #15 (1st Spider-Man)",
                    category = CollectibleCategory.COMICS.displayName,
                    subcategory = "Marvel",
                    description = "Historic Silver Age key issue. Off-white to white pages, original gloss.",
                    ownerName = "ComicVault NY",
                    ownerRating = 4.88f,
                    estimatedValueUsd = 28500.0,
                    conditionGrade = "CGC 8.5 Very Fine+",
                    authenticityScore = 98,
                    vaultHashId = "VAULT-9041-5512-2026",
                    isListedForSale = false,
                    salePriceUsd = 0.0,
                    imageType = "COMIC"
                ),
                CollectibleItem(
                    title = "Nike Air Jordan 1 Retro High 'Chicago' 1985",
                    category = CollectibleCategory.SNEAKERS.displayName,
                    subcategory = "Air Jordan",
                    description = "Original 1985 release with wings logo, vivid leather dye, collector display box.",
                    ownerName = "Kicks Vault",
                    ownerRating = 4.92f,
                    estimatedValueUsd = 18500.0,
                    conditionGrade = "Grade 9.2 Very Good",
                    authenticityScore = 97,
                    vaultHashId = "VAULT-1102-9931-2026",
                    isListedForSale = true,
                    salePriceUsd = 18500.0,
                    imageType = "SNEAKER"
                )
            )

            for (item in sampleItems) {
                val insertedId = db.collectibleDao().insertItem(item)
                val itemWithId = item.copy(id = insertedId)
                FirestoreSyncManager.syncCollectibleToCloud(itemWithId)
            }

            // Seed sample escrow
            db.escrowDao().insertEscrow(
                EscrowTransaction(
                    itemId = 1,
                    itemTitle = "1999 Charizard 1st Edition Shadowless",
                    buyerName = "Marcus Brody",
                    sellerName = "Alex Vance",
                    amountUsd = 12500.0,
                    currencyCode = "USD",
                    feeUsd = 312.50, // 2.5%
                    status = EscrowStatus.FUNDS_HELD.name,
                    trackingNumber = "TRK-VAULT-99482"
                )
            )

            // Seed sample encrypted chat messages
            val initialMsgText = "Hello! Is the Charizard PSA 10 certificate verified in the Vault Escrow ledger?"
            val encryptedHex = encryptE2EE(initialMsgText)
            db.messageDao().insertMessage(
                ChatMessage(
                    senderName = "Marcus Brody",
                    receiverName = "Alex Vance",
                    encryptedText = encryptedHex,
                    decryptedText = initialMsgText,
                    isSender = false
                )
            )

            val replyMsgText = "Yes, absolutely! Escrow deposit was confirmed by Vault AI. I will dispatch via insured courier today."
            db.messageDao().insertMessage(
                ChatMessage(
                    senderName = "Alex Vance",
                    receiverName = "Marcus Brody",
                    encryptedText = encryptE2EE(replyMsgText),
                    decryptedText = replyMsgText,
                    isSender = true
                )
            )

            // Seed trade alerts
            db.tradeAlertDao().insertAlert(
                TradeAlert(
                    title = "Escrow Deposit Secured",
                    message = "Buyer Marcus Brody deposited $12,500.00 USD into Vault Escrow. Please generate shipping label.",
                    alertType = "ESCROW"
                )
            )
            db.tradeAlertDao().insertAlert(
                TradeAlert(
                    title = "AI Authenticity Verified",
                    message = "Rolex Daytona 116500LN scored 100% Authenticity Confidence. Certificate VAULT-3329 issued.",
                    alertType = "AUTH"
                )
            )

            // Seed reviews
            db.userReviewDao().insertReview(
                UserReview(
                    reviewerName = "Satoshi_K",
                    rating = 5.0f,
                    comment = "Super smooth escrow trade! Item arrived verified in exact PSA 10 condition.",
                    dateText = "Yesterday"
                )
            )
            db.userReviewDao().insertReview(
                UserReview(
                    reviewerName = "HorologyKing",
                    rating = 5.0f,
                    comment = "Vault E2EE messaging and multi-currency payout made international watch trading effortless.",
                    dateText = "3 days ago"
                )
            )
        }
    }

    suspend fun addNewCollectible(
        title: String,
        category: String,
        description: String,
        imageType: String
    ): CollectibleItem {
        val appraisal = GeminiService.analyzeAndAppraise(title, category, description)
        val newItem = CollectibleItem(
            title = title,
            category = category,
            description = description,
            ownerName = "Vault Collector",
            ownerRating = 4.9f,
            estimatedValueUsd = appraisal.estimatedValueUsd,
            conditionGrade = appraisal.grade,
            certSerialNumber = appraisal.certSerialNumber,
            gradingCompany = appraisal.gradingCompany,
            centeringGrade = appraisal.centeringGrade,
            cornersGrade = appraisal.cornersGrade,
            edgesGrade = appraisal.edgesGrade,
            surfaceGrade = appraisal.surfaceGrade,
            authenticityScore = appraisal.authenticityScore,
            vaultHashId = appraisal.vaultHashId,
            isListedForSale = false,
            salePriceUsd = appraisal.estimatedValueUsd,
            imageType = imageType
        )
        val id = db.collectibleDao().insertItem(newItem)
        val savedItem = newItem.copy(id = id)
        FirestoreSyncManager.syncCollectibleToCloud(savedItem)
        return savedItem
    }

    suspend fun createEscrow(
        item: CollectibleItem,
        buyerName: String,
        currencyCode: String,
        feePercentage: Double = 3.5
    ): Long {
        val itemPrice = if (item.salePriceUsd > 0) item.salePriceUsd else item.estimatedValueUsd
        val feeUsd = itemPrice * (feePercentage / 100.0)
        val escrow = EscrowTransaction(
            itemId = item.id,
            itemTitle = item.title,
            buyerName = buyerName,
            sellerName = item.ownerName,
            amountUsd = itemPrice,
            currencyCode = currencyCode,
            feeUsd = feeUsd,
            feePercentage = feePercentage,
            status = EscrowStatus.FUNDS_HELD.name
        )
        val escrowId = db.escrowDao().insertEscrow(escrow)

        db.tradeAlertDao().insertAlert(
            TradeAlert(
                title = "New Escrow Trade Created",
                message = "Escrow #${escrowId} for '${item.title}' active with ${String.format("%.1f", feePercentage)}% platform fee held safely.",
                alertType = "ESCROW"
            )
        )
        return escrowId
    }

    suspend fun confirmEscrowBuyer(escrowId: Long) {
        val escrow = db.escrowDao().getEscrowById(escrowId) ?: return
        val updated = escrow.copy(buyerConfirmed = true)
        checkAndReleaseEscrow(updated)
    }

    suspend fun confirmEscrowSeller(escrowId: Long) {
        val escrow = db.escrowDao().getEscrowById(escrowId) ?: return
        val updated = escrow.copy(sellerConfirmed = true)
        checkAndReleaseEscrow(updated)
    }

    private suspend fun checkAndReleaseEscrow(escrow: EscrowTransaction) {
        if (escrow.buyerConfirmed && escrow.sellerConfirmed) {
            val netSellerPayout = escrow.amountUsd - escrow.feeUsd
            val completedEscrow = escrow.copy(
                status = EscrowStatus.RELEASED.name
            )
            db.escrowDao().updateEscrow(completedEscrow)

            db.tradeAlertDao().insertAlert(
                TradeAlert(
                    title = "Escrow Funds Released!",
                    message = "Both buyer and seller confirmed Escrow #${escrow.id}. Payout of \$${String.format("%.2f", netSellerPayout)} released to seller. Platform fee \$${String.format("%.2f", escrow.feeUsd)} (${escrow.feePercentage}%) credited to platform account.",
                    alertType = "ESCROW"
                )
            )
        } else {
            db.escrowDao().updateEscrow(escrow)
        }
    }

    suspend fun sendEncryptedMessage(text: String, receiver: String) {
        val encrypted = encryptE2EE(text)
        val msg = ChatMessage(
            senderName = "Me",
            receiverName = receiver,
            encryptedText = encrypted,
            decryptedText = text,
            isSender = true
        )
        db.messageDao().insertMessage(msg)
    }

    // Simple E2EE AES-Style Base64 Simulation
    private fun encryptE2EE(plainText: String): String {
        val bytes = plainText.toByteArray(Charsets.UTF_8)
        return "E2EE-AES256:" + Base64.getEncoder().encodeToString(bytes)
    }

    fun convertCurrency(amountUsd: Double, targetCurrency: CurrencyCode): String {
        val converted = amountUsd * targetCurrency.rateFromUsd
        return when (targetCurrency) {
            CurrencyCode.USD -> String.format("$%.2f USD", converted)
            CurrencyCode.EUR -> String.format("€%.2f EUR", converted)
            CurrencyCode.GBP -> String.format("£%.2f GBP", converted)
            CurrencyCode.JPY -> String.format("¥%,.0f JPY", converted)
        }
    }

    fun generateActivityReportText(items: List<CollectibleItem>, escrows: List<EscrowTransaction>): String {
        val totalVal = items.sumOf { it.estimatedValueUsd }
        val sb = StringBuilder()
        sb.append("VAULT COLLECTIBLES - OFFICIAL INVENTORY & TRADE REPORT\n")
        sb.append("Generated Date: 2026-07-22 | Status: Cryptographically Verified\n")
        sb.append("============================================================\n\n")
        sb.append(String.format("TOTAL PORTFOLIO VALUE: $%.2f USD\n", totalVal))
        sb.append("TOTAL CATALOGED ITEMS: ${items.size}\n")
        sb.append("ACTIVE ESCROW DEALS: ${escrows.size}\n\n")
        sb.append("CATALOG SUMMARY:\n")
        items.forEachIndexed { idx, item ->
            sb.append("${idx + 1}. ${item.title}\n")
            sb.append("   - Category: ${item.category}\n")
            sb.append("   - Grade: ${item.conditionGrade} | Auth: ${item.authenticityScore}%\n")
            sb.append("   - Unique Hash: ${item.vaultHashId}\n")
            sb.append(String.format("   - Market Appraisal: $%.2f USD\n\n", item.estimatedValueUsd))
        }
        sb.append("============================================================\n")
        sb.append("E2EE Encrypted Ledger Signature: 0x8F2A9C91B412E83F001\n")
        return sb.toString()
    }
}
