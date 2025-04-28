package com.example.ecommerce.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Order(
    var id: String = "",
    var userId: String = "",
    var totalAmount: Double = 0.0,
    var shippingAddress: String = "",
    var paymentMethod: String = "Cash on Delivery",
    var status: String = "pending", // pending, processing, shipped, delivered, cancelled
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var items: List<OrderItem> = emptyList(),
    var phoneNumber: String = "",
    var customerName: String = ""
) : Parcelable

@Parcelize
data class OrderItem(
    var id: String = "",
    var productId: String = "",
    var productName: String = "",
    var productImage: String = "",
    var quantity: Int = 1,
    var price: Double = 0.0
) : Parcelable 