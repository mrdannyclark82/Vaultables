package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.remote.ConnectAccountLinkRequest
import com.example.data.remote.EscrowDisputeRequest
import com.example.data.remote.EscrowPaymentRequest
import com.example.data.remote.EscrowShipRequest
import com.example.data.remote.FirebaseAuthTokenProvider
import com.example.data.remote.FirestoreSyncManager
import com.example.data.remote.CardScanImage
import com.example.data.remote.CardScanImages
import com.example.data.remote.NetworkModule
import com.example.data.remote.ScanDraft
import com.example.data.remote.SecureScannerRequest
import com.example.data.remote.SecureScanResultParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.util.UUID

class VaultRepository(
    private val db: AppDatabase,
    private val appContext: Context
) {

    val allItems: Flow<List<CollectibleItem>> = db.collectibleDao().getAllItems()
    private val cloudMarketplace = MutableStateFlow<List<CollectibleItem>>(emptyList())
    private val cloudEscrows = MutableStateFlow<List<EscrowTransaction>>(emptyList())

    /** Prefer live Firestore marketplaceListings; fall back to local listed Room rows. */
    val marketplaceListings: Flow<List<CollectibleItem>> = combine(
        cloudMarketplace,
        db.collectibleDao().getMarketplaceListings()
    ) { cloud, local ->
        if (cloud.isNotEmpty()) cloud else local
    }

    /** Prefer server escrow documents for the signed-in participant. */
    val allEscrows: Flow<List<EscrowTransaction>> = combine(
        cloudEscrows,
        db.escrowDao().getAllEscrows()
    ) { cloud, local ->
        if (cloud.isNotEmpty()) cloud else local
    }

    val allMessages: Flow<List<ChatMessage>> = db.messageDao().getAllMessages()
    val allAlerts: Flow<List<TradeAlert>> = db.tradeAlertDao().getAllAlerts()
    val allReviews: Flow<List<UserReview>> = db.userReviewDao().getAllReviews()

    fun startCloudListeners() {
        // Hot flows are collected from the ViewModel scope.
    }

    fun observeCloudMarketplace(): Flow<List<CollectibleItem>> =
        FirestoreSyncManager.observePublicListings(appContext)

    fun observeCloudEscrows(): Flow<List<EscrowTransaction>> =
        FirestoreSyncManager.observeParticipantEscrows(appContext)

    suspend fun publishCloudMarketplace(listings: List<CollectibleItem>) {
        cloudMarketplace.value = listings
    }

    suspend fun publishCloudEscrows(escrows: List<EscrowTransaction>) {
        cloudEscrows.value = escrows
        escrows.filter { it.serverEscrowId.isNotBlank() }.forEach { remote ->
            val existing = db.escrowDao().getAllEscrows().first()
                .firstOrNull { it.serverEscrowId == remote.serverEscrowId }
            if (existing == null) {
                db.escrowDao().insertEscrow(remote)
            } else {
                db.escrowDao().updateEscrow(
                    existing.copy(
                        status = remote.status,
                        buyerConfirmed = remote.buyerConfirmed,
                        sellerConfirmed = remote.sellerConfirmed,
                        trackingNumber = remote.trackingNumber.ifBlank { existing.trackingNumber },
                        shippingCarrier = remote.shippingCarrier.ifBlank { existing.shippingCarrier },
                        releaseStatus = remote.releaseStatus,
                        payoutNotice = remote.payoutNotice,
                        listingId = remote.listingId.ifBlank { existing.listingId },
                        amountUsd = remote.amountUsd
                    )
                )
            }
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        // Sideload ship: do not invent a marketplace. Existing installs keep their Room data.
    }

    @Suppress("unused")
    private suspend fun seedDemoCatalogUnused() {
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

    suspend fun analyzeCollectible(
        title: String,
        category: String,
        description: String,
        imageType: String,
        brand: String = "",
        year: String = "",
        localImagePath: String? = null,
        localBackImagePath: String? = null
    ): ScanDraft {
        var base64Image = ""
        if (localImagePath != null) {
            try {
                val bytes = com.example.data.remote.CardImageProcessor.prepareForUpload(appContext, localImagePath)
                    ?: throw IllegalArgumentException("Unable to read front card image")
                base64Image = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            } catch (e: Exception) {
                android.util.Log.e("VaultRepository", "Failed to encode image for backend scanner", e)
            }
        }
        if (base64Image.isBlank()) {
            throw IllegalArgumentException("Unable to prepare the front card image for secure scanning.")
        }

        if (localImagePath == null || localBackImagePath == null) {
            throw IllegalArgumentException("Secure card scans require clear front and back images.")
        }
        val backImage = com.example.data.remote.CardImageProcessor.prepareForUpload(appContext, localBackImagePath)
            ?: throw IllegalArgumentException("Unable to read back card image.")
        val request = SecureScannerRequest(
            images = CardScanImages(
                front = CardScanImage("image/jpeg", base64Image),
                back = CardScanImage("image/jpeg", android.util.Base64.encodeToString(backImage, android.util.Base64.NO_WRAP))
            ),
            category = category,
            notes = description
        )
        val response = NetworkModule.scannerService.analyzeCollectible(
            FirebaseAuthTokenProvider.authorizationHeader(),
            request
        )
        val rawBody = response.body()?.string().orEmpty()
        if (!response.isSuccessful) {
            android.util.Log.e("VaultRepository", "Scan HTTP ${response.code()}: $rawBody")
            val serverMessage = rawBody
                .ifBlank { response.errorBody()?.string().orEmpty() }
                .let { body ->
                    runCatching { org.json.JSONObject(body).optJSONObject("error")?.optString("message") }
                        .getOrNull()
                        ?.takeIf { it.isNotBlank() }
                        ?: "HTTP ${response.code()}"
                }
            throw IllegalStateException("Secure scan failed: $serverMessage")
        }
        val secureScan = try {
            SecureScanResultParser.parse(rawBody)
        } catch (e: Exception) {
            android.util.Log.e("VaultRepository", "Scan parse failed: $rawBody", e)
            throw IllegalStateException("Scan returned unreadable data. ${e.message}")
        }
        return ScanDraft(
            title = secureScan.title.ifBlank { title },
            category = category,
            description = description,
            imageType = imageType,
            brand = brand.ifBlank { secureScan.brand },
            year = year.ifBlank { secureScan.year },
            cardNumber = secureScan.cardNumber,
            grade = secureScan.grade.ifBlank { "Unverified — visual extract only" },
            gradingCompany = secureScan.gradingCompany,
            certSerialNumber = secureScan.certSerialNumber,
            localImagePath = localImagePath,
            localBackImagePath = localBackImagePath,
            verificationSummary = listOf(secureScan.verificationSummary, secureScan.notices.joinToString(" "))
                .filter(String::isNotBlank)
                .joinToString(" | "),
            notices = secureScan.notices,
            observations = secureScan.observations,
            marketplaceSearches = secureScan.marketplaceSearches
        )
    }

    suspend fun saveScannedCollectible(draft: ScanDraft): CollectibleItem {
        val newItem = CollectibleItem(
            title = draft.title.ifBlank { "Untitled scan" },
            category = draft.category,
            description = draft.description,
            ownerName = "Vault Collector",
            ownerRating = 0f,
            estimatedValueUsd = 0.0,
            conditionGrade = draft.grade,
            certSerialNumber = draft.certSerialNumber,
            gradingCompany = draft.gradingCompany,
            centeringGrade = 0f,
            cornersGrade = 0f,
            edgesGrade = 0f,
            surfaceGrade = 0f,
            authenticityScore = 0,
            isVerified = false,
            vaultHashId = "VAULT-${UUID.randomUUID()}",
            isListedForSale = false,
            salePriceUsd = 0.0,
            imageType = draft.imageType,
            brandName = draft.brand,
            releaseYear = draft.year,
            teamName = "",
            cardNumber = draft.cardNumber,
            localImagePath = draft.localImagePath,
            localBackImagePath = draft.localBackImagePath,
            verificationSummary = draft.verificationSummary
        )
        val id = db.collectibleDao().insertItem(newItem)
        val savedItem = newItem.copy(id = id)
        FirestoreSyncManager.syncCollectibleToCloud(savedItem)
        return savedItem
    }

    suspend fun listItemForSale(item: CollectibleItem, salePriceUsd: Double): CollectibleItem {
        val priced = item.copy(
            isListedForSale = true,
            salePriceUsd = salePriceUsd,
            marketplaceListingId = FirestoreSyncManager.listingDocumentId(item)
        )
        val listingId = FirestoreSyncManager.publishListing(priced, appContext)
        val saved = priced.copy(marketplaceListingId = listingId)
        db.collectibleDao().updateItem(saved)
        FirestoreSyncManager.syncCollectibleToCloud(saved, appContext)
        return saved
    }

    suspend fun unlistItem(item: CollectibleItem): CollectibleItem {
        FirestoreSyncManager.unpublishListing(item, appContext)
        val saved = item.copy(isListedForSale = false, salePriceUsd = 0.0)
        db.collectibleDao().updateItem(saved)
        FirestoreSyncManager.syncCollectibleToCloud(saved, appContext)
        return saved
    }

    /**
     * Legacy Room insert kept only as an offline placeholder. Real escrow is created by the
     * Stripe webhook after PaymentIntent success — never invent local release authority.
     */
    suspend fun createLocalEscrowPlaceholder(
        item: CollectibleItem,
        buyerName: String,
        currencyCode: String,
        paymentIntentId: String,
        listingId: String
    ): Long {
        val itemPrice = if (item.salePriceUsd > 0) item.salePriceUsd else item.estimatedValueUsd
        val escrow = EscrowTransaction(
            itemId = item.id,
            itemTitle = item.title,
            buyerName = buyerName,
            sellerName = item.ownerName,
            amountUsd = itemPrice,
            currencyCode = currencyCode,
            feeUsd = 0.0,
            feePercentage = 0.0,
            status = EscrowStatus.PAID.name,
            serverEscrowId = "",
            listingId = listingId,
            releaseStatus = "held",
            trackingNumber = "",
            shippingCarrier = ""
        )
        val escrowId = db.escrowDao().insertEscrow(escrow)
        db.tradeAlertDao().insertAlert(
            TradeAlert(
                title = "Payment Submitted",
                message = "PaymentIntent $paymentIntentId submitted for '${item.title}'. Escrow appears after webhook confirmation.",
                alertType = "ESCROW"
            )
        )
        return escrowId
    }

    suspend fun waitForServerEscrow(listingId: String, timeoutMs: Long = 45_000L): EscrowTransaction? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val match = cloudEscrows.value.firstOrNull { it.listingId == listingId && it.serverEscrowId.isNotBlank() }
                ?: db.escrowDao().getAllEscrows().first().firstOrNull {
                    it.listingId == listingId && it.serverEscrowId.isNotBlank()
                }
            if (match != null) return match
            delay(1_500)
        }
        return null
    }

    suspend fun confirmEscrowBuyer(escrow: EscrowTransaction) {
        val serverId = escrow.serverEscrowId
        if (serverId.isBlank()) {
            throw IllegalStateException("Server escrow is not ready yet. Wait for payment webhook confirmation.")
        }
        val response = NetworkModule.paymentService.confirmEscrowReceipt(
            authorization = FirebaseAuthTokenProvider.authorizationHeader(),
            escrowId = serverId,
            body = emptyMap()
        )
        if (!response.isSuccessful) {
            throw IllegalStateException("Confirm receipt failed: HTTP ${response.code()}")
        }
        val body = response.body()
        applyServerEscrowUpdate(escrow, body?.state, body?.releaseStatus, body?.buyerConfirmedReceived, body?.sellerConfirmedShipped, body?.payoutNotice)
        if (!body?.payoutNotice.isNullOrBlank()) {
            db.tradeAlertDao().insertAlert(
                TradeAlert(
                    title = "Release blocked",
                    message = body!!.payoutNotice!!,
                    alertType = "ESCROW"
                )
            )
        }
    }

    suspend fun shipEscrowAsSeller(
        escrow: EscrowTransaction,
        carrier: String,
        trackingNumber: String
    ) {
        val serverId = escrow.serverEscrowId
        if (serverId.isBlank()) {
            throw IllegalStateException("Server escrow is not ready yet.")
        }
        val response = NetworkModule.paymentService.shipEscrow(
            authorization = FirebaseAuthTokenProvider.authorizationHeader(),
            escrowId = serverId,
            request = EscrowShipRequest(carrier = carrier, trackingNumber = trackingNumber)
        )
        if (!response.isSuccessful) {
            throw IllegalStateException("Ship failed: HTTP ${response.code()}")
        }
        val body = response.body()
        applyServerEscrowUpdate(
            escrow.copy(shippingCarrier = carrier, trackingNumber = trackingNumber),
            body?.state,
            body?.releaseStatus,
            body?.buyerConfirmedReceived,
            body?.sellerConfirmedShipped,
            body?.payoutNotice
        )
    }

    suspend fun openEscrowDispute(escrow: EscrowTransaction, reason: String, details: String? = null) {
        val serverId = escrow.serverEscrowId
        if (serverId.isBlank()) {
            throw IllegalStateException("Server escrow is not ready yet.")
        }
        val response = NetworkModule.paymentService.openEscrowDispute(
            authorization = FirebaseAuthTokenProvider.authorizationHeader(),
            escrowId = serverId,
            request = EscrowDisputeRequest(reason = reason, details = details)
        )
        if (!response.isSuccessful) {
            throw IllegalStateException("Dispute failed: HTTP ${response.code()}")
        }
        val body = response.body()
        applyServerEscrowUpdate(escrow, body?.state, body?.releaseStatus, body?.buyerConfirmedReceived, body?.sellerConfirmedShipped, body?.payoutNotice)
    }

    private suspend fun applyServerEscrowUpdate(
        escrow: EscrowTransaction,
        state: String?,
        releaseStatus: String?,
        buyerConfirmed: Boolean?,
        sellerConfirmed: Boolean?,
        payoutNotice: String?
    ) {
        if (escrow.id == 0L && escrow.serverEscrowId.isNotBlank()) {
            // Cloud-only row; listener will refresh.
            return
        }
        val updated = escrow.copy(
            status = state?.let { EscrowStatus.fromServerState(it).name } ?: escrow.status,
            releaseStatus = releaseStatus ?: escrow.releaseStatus,
            buyerConfirmed = buyerConfirmed ?: escrow.buyerConfirmed,
            sellerConfirmed = sellerConfirmed ?: escrow.sellerConfirmed,
            payoutNotice = payoutNotice ?: escrow.payoutNotice,
            trackingNumber = escrow.trackingNumber,
            shippingCarrier = escrow.shippingCarrier
        )
        if (escrow.id > 0L) {
            db.escrowDao().updateEscrow(updated)
        }
    }

    suspend fun getConnectStatus() =
        NetworkModule.paymentService.getConnectStatus(
            authorization = FirebaseAuthTokenProvider.authorizationHeader(),
            body = emptyMap()
        ).let { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Connect status failed: HTTP ${response.code()}")
            }
            response.body() ?: throw IllegalStateException("Empty Connect status response")
        }

    suspend fun createConnectOnboardingLink(
        returnUrl: String = "https://vaultables.com/connect/return",
        refreshUrl: String = "https://vaultables.com/connect/refresh"
    ): String {
        val response = NetworkModule.paymentService.createConnectAccountLink(
            authorization = FirebaseAuthTokenProvider.authorizationHeader(),
            request = ConnectAccountLinkRequest(returnUrl = returnUrl, refreshUrl = refreshUrl)
        )
        if (!response.isSuccessful) {
            throw IllegalStateException("Connect onboarding failed: HTTP ${response.code()}")
        }
        return response.body()?.url ?: throw IllegalStateException("Empty Connect onboarding URL")
    }

    suspend fun createStripePaymentIntent(item: CollectibleItem): Pair<com.example.data.remote.EscrowPaymentResponse, String> {
        val listingId = item.marketplaceListingId.ifBlank {
            if (!item.isListedForSale || item.vaultHashId.isBlank()) {
                throw IllegalArgumentException("Only a published marketplace listing can be purchased.")
            }
            FirestoreSyncManager.listingDocumentId(item)
        }
        val req = EscrowPaymentRequest(listingId)
        val response = NetworkModule.paymentService.createEscrowPaymentIntent(
            authorization = FirebaseAuthTokenProvider.authorizationHeader(),
            idempotencyKey = UUID.randomUUID().toString().replace("-", ""),
            request = req
        )
        if (response.isSuccessful) {
            val body = response.body() ?: throw Exception("Empty response from Escrow Server")
            return body to listingId
        } else {
            throw IllegalStateException("Failed to create a secure payment intent: HTTP ${response.code()}.")
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
        val bytes = plainText.toByteArray(StandardCharsets.UTF_8)
        return "E2EE-AES256:" + Base64.encodeToString(bytes, Base64.NO_WRAP)
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
        sb.append("Generated locally. Not a certified appraisal or encrypted ledger.\n")
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
        sb.append("End of local inventory export.\n")
        return sb.toString()
    }

    suspend fun clearAllMockItems() {
        db.collectibleDao().deleteAllItems()
    }
}
