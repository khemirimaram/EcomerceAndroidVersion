package com.example.ecommerce.utils

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirestoreUtils {
    private val firestore = FirebaseFirestore.getInstance()
    
    /**
     * Update user profile with new avatar URL
     */
    suspend fun updateUserAvatar(userId: String, avatarUrl: String) {
        val updates = mapOf(
            "avatarUrl" to avatarUrl,
            "updatedAt" to Timestamp.now()
        )
        
        firestore.collection("users")
            .document(userId)
            .update(updates)
            .await()
    }
    
    /**
     * Update conversation participant avatar
     */
    suspend fun updateConversationAvatar(conversationId: String, userId: String, avatarUrl: String) {
        val updates = mapOf(
            "participantAvatars.$userId" to avatarUrl,
            "updatedAt" to Timestamp.now()
        )
        
        firestore.collection("conversations")
            .document(conversationId)
            .update(updates)
            .await()
    }
    
    /**
     * Update message with image URL
     */
    suspend fun updateMessageWithImage(
        conversationId: String,
        messageId: String,
        imageUrl: String
    ) {
        val updates = mapOf(
            "imageUrl" to imageUrl,
            "updatedAt" to Timestamp.now()
        )
        
        firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .document(messageId)
            .update(updates)
            .await()
    }
    
    /**
     * Mark conversation messages as read for a user
     */
    suspend fun markConversationAsRead(conversationId: String, userId: String) {
        val updates = mapOf(
            "unreadCount.$userId" to 0,
            "updatedAt" to Timestamp.now()
        )
        
        firestore.collection("conversations")
            .document(conversationId)
            .update(updates)
            .await()
    }
} 