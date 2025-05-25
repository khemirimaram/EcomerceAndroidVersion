package com.example.ecommerce.firebase

import com.example.ecommerce.models.Product
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ProductRepository(private val firestore: FirebaseFirestore) {
    
    private val productsCollection = firestore.collection("produits")
    
    suspend fun addProduct(product: Product) {
        productsCollection.document(product.id).set(product).await()
    }
    
    suspend fun getProduct(productId: String): Product? {
        return productsCollection.document(productId).get().await().toObject(Product::class.java)
    }
    
    suspend fun getAllProducts(): List<Product> {
        return productsCollection.get().await().toObjects(Product::class.java)
    }
    
    suspend fun updateProduct(product: Product) {
        productsCollection.document(product.id).set(product).await()
    }
    
    suspend fun deleteProduct(productId: String) {
        productsCollection.document(productId).delete().await()
    }
} 