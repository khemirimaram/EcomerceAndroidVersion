package com.example.ecommerce.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp

data class Message(
    @DocumentId
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val read: Boolean = false,
    val type: String = TYPE_TEXT,
    val imageUrl: String? = null,
    val productId: String? = null,
    val senderPhotoUrl: String? = null,
    @get:Exclude
    var isUploading: Boolean = false
) {
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_IMAGE = "image"
    }
} 