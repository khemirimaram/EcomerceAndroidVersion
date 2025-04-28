package com.example.ecommerce.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Category(
    val id: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val description: String = "",
    val productCount: Int = 0
) : Parcelable 