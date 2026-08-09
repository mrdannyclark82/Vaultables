package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.model.CollectibleItem
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object FirestoreSyncManager {
    private const val TAG = "FirestoreSyncManager"
    private const val COLLECTION_VAULT = "user_collections"
    private const val COLLECTION_MARKETPLACE = "marketplace_items"

    private fun getFirestoreInstance(context: Context? = null): FirebaseFirestore? {
        return try {
            if (context != null) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (ignored: Exception) {}
            }
            val defaultApp = try {
                FirebaseApp.getInstance()
            } catch (e: Exception) {
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
        try {
            val itemData = hashMapOf(
                "id" to item.id,
                "title" to item.title,
                "category" to item.category,
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
                "updatedAt" to System.currentTimeMillis()
            )

            val docId = if (item.id != 0L) "item_${item.id}" else "item_${item.vaultHashId}"

            fs.collection(COLLECTION_VAULT)
                .document(docId)
                .set(itemData)
                .await()

            if (item.isListedForSale) {
                fs.collection(COLLECTION_MARKETPLACE)
                    .document("market_$docId")
                    .set(itemData)
                    .await()
            }
            Log.d(TAG, "Item '${item.title}' synced to Firestore cloud storage.")
        } catch (e: Exception) {
            Log.d(TAG, "Firestore upload notice: ${e.message}")
        }
    }

    suspend fun syncAllCollectibles(items: List<CollectibleItem>, context: Context? = null) = withContext(Dispatchers.IO) {
        items.forEach { item ->
            syncCollectibleToCloud(item, context)
        }
    }
}
