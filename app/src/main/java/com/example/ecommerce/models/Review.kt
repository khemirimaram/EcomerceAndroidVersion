package com.example.ecommerce.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Review(
    var id: String = "",
    var rating: Double = 0.0,
    var comment: String = "",
    var sellerUserId: String = "",
    var reviewerUserId: String = "",
    var reviewerName: String = "",
    var reviewerImageUrl: String? = null,
    var createdAt: Long = System.currentTimeMillis(),
    var productId: String? = null
) : Parcelable 