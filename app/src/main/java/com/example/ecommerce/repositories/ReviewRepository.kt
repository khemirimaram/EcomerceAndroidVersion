package com.example.ecommerce.repositories

import android.util.Log
import com.example.ecommerce.models.Review
import com.example.ecommerce.models.RatingInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.Result

class ReviewRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "ReviewRepository"

    suspend fun addOrUpdateReview(
        sellerId: String,
        productId: String,
        rating: Float,
        comment: String?
    ): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("User not logged in")
            val buyerId = currentUser.uid
            val buyerName = currentUser.displayName ?: "Anonymous"

            val review = Review(
                id = "",  // Firestore will generate this
                sellerId = sellerId,
                buyerId = buyerId,
                buyerName = buyerName,
                productId = productId,
                rating = rating,
                comment = comment ?: ""
            )

            firestore.collection("reviews")
                .add(review)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding/updating review", e)
            Result.failure(e)
        }
    }

    suspend fun getCurrentUserReview(sellerId: String): Result<Review?> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("User not logged in")
            
            val review = firestore.collection("reviews")
                .whereEqualTo("sellerId", sellerId)
                .whereEqualTo("buyerId", currentUser.uid)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.toObject(Review::class.java)

            Result.success(review)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user review", e)
            Result.failure(e)
        }
    }

    suspend fun getSellerRating(sellerId: String): Result<RatingInfo> {
        return try {
            val reviews = firestore.collection("reviews")
                .whereEqualTo("sellerId", sellerId)
                .get()
                .await()
                .toObjects(Review::class.java)

            val averageRating = if (reviews.isNotEmpty()) {
                reviews.map { it.rating }.average().toFloat()
            } else {
                0f
            }

            Result.success(RatingInfo(averageRating, reviews.size))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting seller rating", e)
            Result.failure(e)
        }
    }

    suspend fun getUserCanReviewSeller(sellerId: String): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false
            if (currentUser.uid == sellerId) return false

            // Check if user has bought from seller
            val purchases = firestore.collection("purchases")
                .whereEqualTo("buyerId", currentUser.uid)
                .whereEqualTo("sellerId", sellerId)
                .get()
                .await()

            purchases.documents.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if user can review seller", e)
            false
        }
    }
} 