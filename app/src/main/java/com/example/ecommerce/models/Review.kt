package com.example.ecommerce.models

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class Review(
    val id: String = "",
    val sellerId: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val productId: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val createdAt: Timestamp = Timestamp.now()
) : Parcelable 