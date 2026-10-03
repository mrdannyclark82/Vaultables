package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.model.CollectibleItem
import com.example.data.model.EscrowStatus
import com.example.data.model.EscrowTransaction
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object FirestoreSyncManager {
    private const val TAG = "FirestoreSyncManager"
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_MARKETPLACE = "marketplaceListings"
    private const val COLLECTION_ESCROWS = "escrows"

    fun listingDocumentId(item: CollectibleItem): String {
        if (item.marketplaceListingId.isNotBlank()) return item.marketplaceListingId
        val hash = item.vaultHashId.ifBlank { "item_${item.id}" }
        return "market_$hash"
    }

    private fun getFirestoreInstance(context: Context? = null): FirebaseFirestore? {
        return try {
            if (context != null) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (_: Exception) {
                }
            }
            val defaultApp = try {
                FirebaseApp.getInstance()
            } catch (_: Exception) {
                null
            }
            if (defaultApp != null) {
                FirebaseFirestore.getInstance()
            } else {
                Log.d(TAG, "FirebaseApp default instance is not initialized.")
                null
            }
        } catch (e: Exception) {
            Log.d(TAG, "Firestore initialization notice: ${e.message}")
            null
        }
    }

    suspend fun syncCollectibleToCloud(item: CollectibleItem, context: Context? = null) = withContext(Dispatchers.IO) {
        val fs = getFirestoreInstance(context) ?: run {
            Log.d(TAG, "Firestore instance not available, skipping cloud sync.")
            return@withContext
        }
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Log.d(TAG, "No authenticated user, skipping cloud sync.")
            return@withContext
        }
        try {
            val itemData = hashMapOf(
                "id" to item.id,
                "title" to item.title,
                "category" to item.category,
                "subcategory" to item.subcategory,
                "description" to item.description,
                "ownerName" to item.ownerName,
                "ownerRating" to item.ownerRating,
                "estimatedValueUsd" to item.estimatedValueUsd,
                "conditionGrade" to item.conditionGrade,
                "authenticityScore" to item.authenticityScore,
                "vaultHashId" to item.vaultHashId,
                "isListedForSale" to item.isListedForSale,
                "salePriceUsd" to item.salePriceUsd,
                "imageType" to item.imageType,
                "marketplaceListingId" to listingDocumentId(item),
                "updatedAt" to System.currentTimeMillis()
            )

            val docId = item.vaultHashId.takeIf { it.isNotBlank() } ?: "item_${item.id}"

            fs.collection(COLLECTION_USERS)
                .document(userId)
                .collection("items")
                .document(docId)
                .set(itemData)
                .await()

            if (item.isListedForSale) {
                publishListing(item, userId, fs)
            }
            Log.d(TAG, "Item '${item.title}' synced to Firestore cloud storage.")
        } catch (e: Exception) {
            Log.d(TAG, "Firestore upload notice: ${e.message}")
        }
    }

    suspend fun publishListing(item: CollectibleItem, context: Context? = null): String = withContext(Dispatchers.IO) {
        val fs = getFirestoreInstance(context)
            ?: throw IllegalStateException("Firestore is unavailable.")
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Sign in is required to list an item.")
        publishListing(item, userId, fs)
    }

    private suspend fun publishListing(
        item: CollectibleItem,
        userId: String,
        fs: FirebaseFirestore
    ): String {
        val listingId = listingDocumentId(item)
        val priceUsd = if (item.salePriceUsd > 0) item.salePriceUsd else item.estimatedValueUsd
        val priceMinor = (priceUsd * 100.0).toLong()
        if (priceMinor <= 0L) {
            throw IllegalArgumentException("A positive sale price is required to list.")
        }
        val payload = hashMapOf(
            "sellerId" to userId,
            "title" to item.title,
            "category" to item.category,
            "subcategory" to item.subcategory,
            "description" to item.description,
            "ownerName" to item.ownerName,
            "ownerRating" to item.ownerRating,
            "conditionGrade" to item.conditionGrade,
            "authenticityScore" to item.authenticityScore,
            "vaultHashId" to item.vaultHashId,
            "imageType" to item.imageType,
            "localItemId" to item.id,
            "priceMinor" to priceMinor,
            "currency" to "usd",
            "visibility" to "public",
            "status" to "active",
            "salePriceUsd" to priceUsd,
            "estimatedValueUsd" to item.estimatedValueUsd,
            "updatedAt" to System.currentTimeMillis()
        )
        fs.collection(COLLECTION_MARKETPLACE)
            .document(listingId)
            .set(payload, SetOptions.merge())
            .await()
        return listingId
    }

    suspend fun unpublishListing(item: CollectibleItem, context: Context? = null) = withContext(Dispatchers.IO) {
        val fs = getFirestoreInstance(context) ?: return@withContext
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext
        val listingId = listingDocumentId(item)
        try {
            val snap = fs.collection(COLLECTION_MARKETPLACE).document(listingId).get().await()
            if (snap.exists() && snap.getString("sellerId") == userId) {
                snap.reference.delete().await()
            }
        } catch (e: Exception) {
            Log.d(TAG, "Unlist failed: ${e.message}")
            throw IllegalStateException("Could not remove the marketplace listing. Try again.", e)
        }
    }

    /** Emits null when the live feed is unavailable (no Firestore / listener error). */
    fun observePublicListings(context: Context? = null): Flow<List<CollectibleItem>?> = callbackFlow {
        val fs = getFirestoreInstance(context)
        if (fs == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val registration: ListenerRegistration = fs.collection(COLLECTION_MARKETPLACE)
            .whereEqualTo("visibility", "public")
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.d(TAG, "Marketplace listen notice: ${error.message}")
                    trySend(null)
                    return@addSnapshotListener
                }
                val listings = snapshot?.documents?.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val priceMinor = doc.getLong("priceMinor") ?: 0L
                    val salePrice = doc.getDouble("salePriceUsd")
                        ?: if (priceMinor > 0) priceMinor / 100.0 else 0.0
                    CollectibleItem(
                        id = doc.getLong("localItemId") ?: 0L,
                        title = title,
                        category = doc.getString("category") ?: "",
                        subcategory = doc.getString("subcategory") ?: "",
                        description = doc.getString("description") ?: "",
                        ownerName = doc.getString("ownerName") ?: "Seller",
                        ownerRating = (doc.getDouble("ownerRating") ?: 4.9).toFloat(),
                        estimatedValueUsd = doc.getDouble("estimatedValueUsd") ?: salePrice,
                        conditionGrade = doc.getString("conditionGrade") ?: "",
                        authenticityScore = (doc.getLong("authenticityScore") ?: 98L).toInt(),
                        vaultHashId = doc.getString("vaultHashId") ?: doc.id.removePrefix("market_"),
                        isListedForSale = true,
                        salePriceUsd = salePrice,
                        imageType = doc.getString("imageType") ?: "CARD",
                        createdAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                        marketplaceListingId = doc.id
                    )
                } ?: emptyList()
                trySend(listings.sortedByDescending { it.createdAt })
            }
        awaitClose { registration.remove() }
    }

    /** Emits the signed-in Firebase uid (or null) now and on every sign-in / sign-out. */
    fun observeAuthUid(): Flow<String?> = callbackFlow {
        val auth = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.d(TAG, "Auth unavailable: ${e.message}")
            null
        }
        if (auth == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    fun observeParticipantEscrows(context: Context? = null): Flow<List<EscrowTransaction>> = callbackFlow {
        val fs = getFirestoreInstance(context)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (fs == null || uid.isNullOrBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = fs.collection(COLLECTION_ESCROWS)
            .whereArrayContains("participantIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.d(TAG, "Escrow listen notice: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val escrows = snapshot?.documents?.mapNotNull { doc ->
                    val amountMinor = doc.getLong("amountMinor") ?: return@mapNotNull null
                    val state = doc.getString("state") ?: "paid"
                    val releaseStatus = doc.getString("releaseStatus") ?: "held"
                    val shipping = doc.get("shipping") as? Map<*, *>
                    val carrier = shipping?.get("carrier") as? String ?: ""
                    val tracking = shipping?.get("trackingNumber") as? String ?: ""
                    val buyerId = doc.getString("buyerId").orEmpty()
                    val sellerId = doc.getString("sellerId").orEmpty()
                    EscrowTransaction(
                        itemId = 0L,
                        itemTitle = doc.getString("listingId") ?: doc.id,
                        buyerName = if (buyerId == uid) "You" else "Buyer",
                        sellerName = if (sellerId == uid) "You" else "Seller",
                        amountUsd = amountMinor / 100.0,
                        currencyCode = (doc.getString("currency") ?: "usd").uppercase(),
                        feeUsd = 0.0,
                        feePercentage = 0.0,
                        buyerConfirmed = doc.getBoolean("buyerConfirmedReceived") == true,
                        sellerConfirmed = doc.getBoolean("sellerConfirmedShipped") == true,
                        status = EscrowStatus.fromServerState(state).name,
                        trackingNumber = tracking,
                        shippingCarrier = carrier,
                        serverEscrowId = doc.id,
                        listingId = doc.getString("listingId").orEmpty(),
                        releaseStatus = releaseStatus,
                        payoutNotice = if (releaseStatus == "blocked") {
                            "Seller payout is not configured; funds remain held."
                        } else {
                            ""
                        }
                    )
                } ?: emptyList()
                trySend(escrows)
            }
        awaitClose { registration.remove() }
    }

    suspend fun syncAllCollectibles(items: List<CollectibleItem>, context: Context? = null) = withContext(Dispatchers.IO) {
        items.forEach { item ->
            syncCollectibleToCloud(item, context)
        }
    }
}
