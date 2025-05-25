package com.example.ecommerce.models

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Product(
    @DocumentId
    val id: String = "",
    
    @PropertyName("name")
    val name: String = "",
    
    @PropertyName("description")
    val description: String = "",
    
    @PropertyName("price")
    val price: Double = 0.0,
    
    @PropertyName("quantity")
    val quantity: Int = 1,
    
    @PropertyName("category")
    val category: String = "",
    
    @PropertyName("condition")
    val condition: String = "",
    
    @PropertyName("images")
    val images: List<String> = listOf(),
    
    @PropertyName("sellerId")
    val sellerId: String = "",
    
    @PropertyName("sellerName")
    val sellerName: String = "",
    
    @PropertyName("sellerPhoto")
    val sellerPhoto: String? = null,
    
    @PropertyName("location")
    val location: String = "",
    
    @PropertyName("isAvailableForExchange")
    val isAvailableForExchange: Boolean = false,
    
    @PropertyName("exchangePreferences")
    val exchangePreferences: String? = null,
    
    @PropertyName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    
    @PropertyName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @PropertyName("status")
    val status: String = STATUS_ACTIVE,
    
    @PropertyName("searchKeywords")
    val searchKeywords: List<String> = listOf(),
    
    @get:Exclude
    var isFavorite: Boolean = false
) : Parcelable {
    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_SOLD = "sold"
        const val STATUS_ARCHIVED = "archived"
        
        const val CONDITION_NEW = "new"
        const val CONDITION_LIKE_NEW = "likeNew"
        const val CONDITION_GOOD = "good"
        const val CONDITION_FAIR = "fair"
        const val CONDITION_POOR = "poor"
        
        private const val NEW_PRODUCT_THRESHOLD_DAYS = 7
        
        private val COMMON_WORDS = setOf(
            "le", "la", "les", "un", "une", "des", "de", "du", "et", "ou", 
            "pour", "par", "sur", "dans", "avec", "sans", "the", "and", "or"
        )
    }
    
    @get:Exclude
    val isNew: Boolean
        get() {
            val currentTime = System.currentTimeMillis()
            val ageInDays = (currentTime - createdAt) / (24 * 60 * 60 * 1000)
            return ageInDays <= NEW_PRODUCT_THRESHOLD_DAYS
        }
        
    fun generateSearchKeywords(): List<String> {
        val keywords = mutableSetOf<String>()
        
        // Add name words
        keywords.addAll(name.lowercase().split(" "))
        
        // Add description words
        keywords.addAll(description.lowercase().split(" "))
        
        // Add category
        keywords.add(category.lowercase())
        
        // Add condition
        keywords.add(condition.lowercase())
        
        // Remove empty strings and common words
        return keywords.filter { it.length > 2 && !COMMON_WORDS.contains(it) }
    }
} 