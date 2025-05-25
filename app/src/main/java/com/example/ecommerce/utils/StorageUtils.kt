package com.example.ecommerce.utils

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

object StorageUtils {
    private val storage = FirebaseStorage.getInstance()
    
    /**
     * Upload an image to Firebase Storage
     * @param imageUri The local URI of the image to upload
     * @param path The path in storage where to save the image (e.g. "chat_images" or "avatars")
     * @return The download URL of the uploaded image
     */
    suspend fun uploadImage(imageUri: Uri, path: String): String {
        val filename = "${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child(path).child(filename)
        
        return try {
            ref.putFile(imageUri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * Delete an image from Firebase Storage
     * @param imageUrl The download URL of the image to delete
     */
    suspend fun deleteImage(imageUrl: String) {
        try {
            val ref = storage.getReferenceFromUrl(imageUrl)
            ref.delete().await()
        } catch (e: Exception) {
            // Handle or rethrow the error
            throw e
        }
    }
    
    /**
     * Upload a chat image and update the message in Firestore
     * @param imageUri The local URI of the image to upload
     * @param conversationId The ID of the conversation
     * @param messageId The ID of the message to update
     */
    suspend fun uploadChatImage(
        imageUri: Uri,
        conversationId: String,
        messageId: String
    ): String {
        return uploadImage(imageUri, "chat_images/$conversationId")
    }
    
    /**
     * Upload a user avatar and update the user profile
     * @param imageUri The local URI of the image to upload
     * @param userId The ID of the user
     */
    suspend fun uploadAvatar(imageUri: Uri, userId: String): String {
        return uploadImage(imageUri, "avatars/$userId")
    }
} 