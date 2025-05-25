package com.example.ecommerce.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class Conversation(
    @DocumentId
    val id: String = "",

    @PropertyName("participants")
    val participants: List<String> = listOf(),

    @PropertyName("lastMessage")
    val lastMessage: Message? = null,

    @PropertyName("lastMessageTimestamp")
    val lastMessageTimestamp: Long = 0,

    @PropertyName("unreadCount")
    val unreadCount: Map<String, Int> = mapOf(),

    @PropertyName("productId")
    val productId: String? = null,

    @PropertyName("productInfo")
    val productInfo: ProductInfo? = null,

    @PropertyName("createdAt")
    @ServerTimestamp
    val createdAt: Timestamp? = null,

    @PropertyName("updatedAt")
    @ServerTimestamp
    val updatedAt: Timestamp? = null,

    @PropertyName("status")
    val status: String = STATUS_ACTIVE
) {
    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_ARCHIVED = "archived"
        const val STATUS_BLOCKED = "blocked"
    }

    data class Message(
        val content: String = "",
        val timestamp: Long = 0,
        val senderId: String = "",
        val type: String = TYPE_TEXT
    ) {
        companion object {
            const val TYPE_TEXT = "text"
            const val TYPE_IMAGE = "image"
            const val TYPE_PRODUCT = "product"
        }
    }

    data class ProductInfo(
        val name: String = "",
        val price: Double = 0.0,
        val imageUrl: String = "",
        val status: String = ""
    )

    fun getOtherParticipantId(currentUserId: String): String? {
        return participants.firstOrNull { it != currentUserId }
    }

    fun getUnreadCount(userId: String): Int {
        return unreadCount[userId] ?: 0
    }
} 