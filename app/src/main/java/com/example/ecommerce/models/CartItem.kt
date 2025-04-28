package com.example.ecommerce.models

data class CartItem(
    val id: String = "",
    val productId: String = "",
    val userId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val productPrice: Double = 0.0,
    val quantity: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
) 