package com.example.ecommerce.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var price: Double = 0.0,
    var category: String = "",
    var imageUrl: String? = null,
    var sellerUserId: String = "",
    var sellerName: String = "",
    var location: String = "",
    var condition: String = "Neuf",
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var isAvailable: Boolean = true,
    var viewCount: Int = 0
) : Parcelable 