package com.example.ecommerce.models

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    @DocumentId
    val id: String = "",
    
    @PropertyName("titre")
    val title: String = "",
    
    @PropertyName("description")
    val description: String = "",
    
    @PropertyName("prix")
    val price: Double = 0.0,
    
    @PropertyName("images")
    val images: List<String> = listOf(),
    
    @PropertyName("categorie")
    val category: String = "",
    
    @PropertyName("vendeurId")
    val sellerId: String = "",
    
    @PropertyName("vendeurNom")
    val sellerName: String = "",
    
    @PropertyName("etat")
    val condition: String = "",
    
    @PropertyName("localisation")
    val location: String = "",
    
    @PropertyName("dateCreation")
    val createdAt: Long = System.currentTimeMillis(),
    
    @PropertyName("dateModification")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @get:Exclude
    var isFavorite: Boolean = false,
    
    @get:Exclude
    val isNew: Boolean = (System.currentTimeMillis() - createdAt) < 7 * 24 * 60 * 60 * 1000 // 7 days
) : Parcelable {
    constructor(
        id: String = "",
        name: String = "",
        description: String = "",
        price: Double = 0.0,
        category: String = "",
        condition: String = "",
        sellerUserId: String = "",
        sellerName: String = "",
        location: String = "",
        images: List<String> = listOf(),
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    ) : this(
        id = id,
        title = name,
        description = description,
        price = price,
        images = images,
        category = category,
        sellerId = sellerUserId,
        sellerName = sellerName,
        condition = condition,
        location = location,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
} 