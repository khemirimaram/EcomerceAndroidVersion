package com.example.ecommerce.firebase

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.ecommerce.models.Product
import com.example.ecommerce.models.User
import com.example.ecommerce.models.CartItem
import com.google.firebase.storage.FirebaseStorage
import android.util.Log
import com.google.firebase.Timestamp

/**
 * Classe de gestion centralisée des interactions avec Firebase.
 * Cette classe est conçue pour être compatible avec la structure de données
 * utilisée par l'application web.
 */
object FirebaseManager {
    // Instances Firebase
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance() 
    private val storage = FirebaseStorage.getInstance()
    
    // Référence de l'utilisateur courant
    val currentUser get() = auth.currentUser
    
    // ===== AUTHENTIFICATION =====
    
    /**
     * Connexion par email/mot de passe
     */
    fun loginWithEmail(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { 
                callback(true, null) 
            }
            .addOnFailureListener { 
                callback(false, it.message) 
            }
    }
    
    /**
     * Inscription par email/mot de passe
     */
    fun registerWithEmail(email: String, password: String, name: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "role" to "user",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                
                db.collection("users")
                    .document(authResult.user?.uid ?: "")
                    .set(user)
                    .addOnSuccessListener { callback(true, null) }
                    .addOnFailureListener { callback(false, it.message) }
            }
            .addOnFailureListener { 
                callback(false, it.message) 
            }
    }
    
    /**
     * Connexion avec Google
     */
    fun authenticateWithGoogle(idToken: String, callback: (Boolean, String?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { authResult -> 
                // Vérifier si c'est un nouvel utilisateur
                val isNewUser = authResult.additionalUserInfo?.isNewUser ?: false
                if (isNewUser) {
                    // Créer le profil dans Firestore
                    val user = hashMapOf(
                        "name" to authResult.user?.displayName,
                        "email" to authResult.user?.email,
                        "role" to "user",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    
                    db.collection("users")
                        .document(authResult.user?.uid ?: "")
                        .set(user)
                        .addOnSuccessListener { callback(true, null) }
                        .addOnFailureListener { callback(false, it.message) }
                } else {
                    callback(true, null)
                }
            }
            .addOnFailureListener { 
                callback(false, it.message) 
            }
    }
    
    /**
     * Déconnexion
     */
    fun logout() {
        auth.signOut()
    }
    
    // ===== PRODUITS =====
    
    /**
     * Récupérer tous les produits
     */
    fun getAllProducts(callback: (List<Product>, String?) -> Unit) {
        db.collection("products")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val products = result.documents.mapNotNull { doc ->
                    try {
                        val product = doc.toObject(Product::class.java)?.copy(id = doc.id)
                        
                        // Convertir Timestamp en Long si nécessaire
                        val timestamp = doc.get("createdAt") as? Timestamp
                        timestamp?.let {
                            product?.createdAt = it.seconds * 1000 + it.nanoseconds / 1000000
                        }
                        
                        val updatedTimestamp = doc.get("updatedAt") as? Timestamp
                        updatedTimestamp?.let {
                            product?.updatedAt = it.seconds * 1000 + it.nanoseconds / 1000000
                        }
                        
                        product
                    } catch (e: Exception) {
                        Log.e("FirebaseManager", "Error converting product: ${e.message}")
                        null
                    }
                }
                callback(products, null)
            }
            .addOnFailureListener {
                callback(emptyList(), it.message)
            }
    }
    
    /**
     * Récupérer les produits par catégorie
     */
    fun getProductsByCategory(category: String, callback: (List<Product>, String?) -> Unit) {
        db.collection("products")
            .whereEqualTo("category", category)
            .get()
            .addOnSuccessListener { result ->
                val products = result.documents.mapNotNull { doc ->
                    try {
                        val product = doc.toObject(Product::class.java)?.copy(id = doc.id)
                        
                        // Convertir Timestamp en Long si nécessaire
                        val timestamp = doc.get("createdAt") as? Timestamp
                        timestamp?.let {
                            product?.createdAt = it.seconds * 1000 + it.nanoseconds / 1000000
                        }
                        
                        val updatedTimestamp = doc.get("updatedAt") as? Timestamp
                        updatedTimestamp?.let {
                            product?.updatedAt = it.seconds * 1000 + it.nanoseconds / 1000000
                        }
                        
                        product
                    } catch (e: Exception) {
                        Log.e("FirebaseManager", "Error converting product: ${e.message}")
                        null
                    }
                }
                callback(products, null)
            }
            .addOnFailureListener {
                callback(emptyList(), it.message)
            }
    }
    
    /**
     * Récupérer un produit par ID
     */
    fun getProductById(productId: String, callback: (Product?, String?) -> Unit) {
        db.collection("products")
            .document(productId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    try {
                        val product = doc.toObject(Product::class.java)?.copy(id = doc.id)
                        
                        // Convertir Timestamp en Long si nécessaire
                        val timestamp = doc.get("createdAt") as? Timestamp
                        timestamp?.let {
                            product?.createdAt = it.seconds * 1000 + it.nanoseconds / 1000000
                        }
                        
                        val updatedTimestamp = doc.get("updatedAt") as? Timestamp
                        updatedTimestamp?.let {
                            product?.updatedAt = it.seconds * 1000 + it.nanoseconds / 1000000
                        }
                        
                        callback(product, null)
                    } catch (e: Exception) {
                        Log.e("FirebaseManager", "Error converting product: ${e.message}")
                        callback(null, "Erreur lors de la conversion du produit: ${e.message}")
                    }
                } else {
                    callback(null, "Produit non trouvé")
                }
            }
            .addOnFailureListener {
                callback(null, it.message)
            }
    }
    
    /**
     * Télécharger un produit avec images
     */
    fun uploadProduct(product: Product, images: List<Uri>, callback: (Boolean, String?) -> Unit) {
        try {
            // Vérifier si l'utilisateur est connecté
            val userId = currentUser?.uid
            if (userId == null) {
                println("Error: uploadProduct - User not authenticated")
                return callback(false, "Utilisateur non connecté")
            }
            
            // Créer une référence pour le produit dans Firestore
            val productRef = db.collection("products").document()
            val productId = productRef.id
            
            // Créer le produit sans image d'abord
            val productWithoutImage = product.copy(
                id = productId,
                imageUrl = null,
                sellerName = currentUser?.displayName ?: "",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            // Convertir en Map pour Firestore
            val productMap = hashMapOf(
                "id" to productWithoutImage.id,
                "name" to productWithoutImage.name,
                "description" to productWithoutImage.description,
                "price" to productWithoutImage.price,
                "category" to productWithoutImage.category,
                "condition" to productWithoutImage.condition,
                "sellerUserId" to productWithoutImage.sellerUserId,
                "sellerName" to productWithoutImage.sellerName,
                "location" to productWithoutImage.location,
                "imageUrl" to productWithoutImage.imageUrl,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "isAvailable" to true,
                "viewCount" to 0
            )
            
            // Sauvegarder dans Firestore
            productRef.set(productMap)
                .addOnSuccessListener {
                    // Si pas d'images, on a terminé
                    if (images.isEmpty()) {
                        callback(true, productId)
                        return@addOnSuccessListener
                    }
                    
                    // Sinon, on tente d'uploader la première image
                    try {
                        val imageRef = storage.reference.child("products/$productId/image_0.jpg")
                        val uploadTask = imageRef.putFile(images[0])
                        
                        uploadTask.addOnSuccessListener {
                            // Obtenir l'URL de l'image
                            imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                                // Mettre à jour le produit avec l'URL
                                productRef.update("imageUrl", downloadUrl.toString())
                                    .addOnSuccessListener {
                                        callback(true, productId)
                                    }
                                    .addOnFailureListener { e ->
                                        // Produit créé mais URL d'image non mise à jour
                                        Log.e("FirebaseManager", "Erreur lors de la mise à jour de l'URL d'image: ${e.message}")
                                        callback(true, productId)
                                    }
                            }.addOnFailureListener { e ->
                                // Produit créé mais URL d'image non obtenue
                                Log.e("FirebaseManager", "Erreur lors de l'obtention de l'URL d'image: ${e.message}")
                                callback(true, productId)
                            }
                        }.addOnFailureListener { e ->
                            // Produit créé mais image non uploadée
                            Log.e("FirebaseManager", "Erreur lors de l'upload de l'image: ${e.message}")
                            callback(true, productId)
                        }
                    } catch (e: Exception) {
                        // Produit créé mais erreur lors de l'upload
                        Log.e("FirebaseManager", "Exception lors de l'upload de l'image: ${e.message}")
                        callback(true, productId)
                    }
                }
                .addOnFailureListener { e ->
                    // Échec de la création du produit
                    Log.e("FirebaseManager", "Erreur lors de la création du produit: ${e.message}")
                    callback(false, e.message)
                }
        } catch (e: Exception) {
            // Exception générale
            Log.e("FirebaseManager", "Exception générale: ${e.message}")
            callback(false, "Erreur inattendue: ${e.message}")
        }
    }
    
    /**
     * Récupérer l'ID de l'utilisateur courant
     */
    fun getCurrentUserId(): String? {
        val uid = currentUser?.uid
        if (uid == null) {
            println("Warning: getCurrentUserId - User not authenticated")
        }
        return uid
    }
    
    // ===== PANIER =====
    
    /**
     * Ajouter un produit au panier
     */
    fun addToCart(productId: String, quantity: Int, callback: (Boolean, String?) -> Unit) {
        val userId = currentUser?.uid ?: return callback(false, "Utilisateur non connecté")
        
        val cartItem = hashMapOf(
            "productId" to productId,
            "quantity" to quantity,
            "addedAt" to FieldValue.serverTimestamp()
        )
        
        db.collection("users")
            .document(userId)
            .collection("cart")
            .add(cartItem)
            .addOnSuccessListener { 
                callback(true, null) 
            }
            .addOnFailureListener { 
                callback(false, it.message) 
            }
    }
    
    /**
     * Récupérer les éléments du panier
     */
    fun getCartItems(callback: (List<CartItem>, String?) -> Unit) {
        val userId = currentUser?.uid ?: return callback(emptyList(), "Utilisateur non connecté")
        
        db.collection("users")
            .document(userId)
            .collection("cart")
            .get()
            .addOnSuccessListener { result ->
                val cartItems = mutableListOf<CartItem>()
                
                if (result.documents.isEmpty()) {
                    callback(emptyList(), null)
                    return@addOnSuccessListener
                }
                
                // Récupérer les détails des produits pour chaque élément du panier
                result.documents.forEach { doc ->
                    val productId = doc.getString("productId") ?: ""
                    val quantity = doc.getLong("quantity")?.toInt() ?: 1
                    
                    // Convertir Timestamp en Long si nécessaire
                    val timestamp = doc.get("addedAt") as? Timestamp
                    val addedAt = timestamp?.seconds?.times(1000)?.plus(timestamp.nanoseconds / 1000000)
                        ?: System.currentTimeMillis()
                    
                    getProductById(productId) { product, error ->
                        if (product != null) {
                            val productImage = product.imageUrl ?: ""
                            cartItems.add(
                                CartItem(
                                    id = doc.id,
                                    productId = productId,
                                    userId = userId,
                                    productName = product.name,
                                    productImage = productImage,
                                    productPrice = product.price,
                                    quantity = quantity,
                                    timestamp = addedAt
                                )
                            )
                        }
                        
                        // Si nous avons traité tous les éléments, retourner la liste complète
                        if (cartItems.size == result.size()) {
                            callback(cartItems, null)
                        }
                    }
                }
            }
            .addOnFailureListener {
                callback(emptyList(), it.message)
            }
    }
    
    /**
     * Retirer un élément du panier
     */
    fun removeFromCart(cartItemId: String, callback: (Boolean, String?) -> Unit) {
        val userId = currentUser?.uid ?: return callback(false, "Utilisateur non connecté")
        
        db.collection("users")
            .document(userId)
            .collection("cart")
            .document(cartItemId)
            .delete()
            .addOnSuccessListener { 
                callback(true, null) 
            }
            .addOnFailureListener { 
                callback(false, it.message) 
            }
    }
    
    // ===== FAVORIS =====
    
    /**
     * Ajouter un produit aux favoris
     */
    fun addToFavorites(productId: String, callback: (Boolean, String?) -> Unit) {
        val userId = currentUser?.uid ?: return callback(false, "Utilisateur non connecté")
        
        val favorite = hashMapOf(
            "productId" to productId,
            "addedAt" to FieldValue.serverTimestamp()
        )
        
        db.collection("users")
            .document(userId)
            .collection("favorites")
            .document(productId) // Utilisation de l'ID du produit comme ID du document pour éviter les doublons
            .set(favorite)
            .addOnSuccessListener { 
                callback(true, null) 
            }
            .addOnFailureListener { 
                callback(false, it.message) 
            }
    }
    
    /**
     * Vérifier si un produit est dans les favoris
     */
    fun isProductFavorite(productId: String, callback: (Boolean) -> Unit) {
        val userId = currentUser?.uid ?: return callback(false)
        
        db.collection("users")
            .document(userId)
            .collection("favorites")
            .document(productId)
            .get()
            .addOnSuccessListener { doc ->
                callback(doc.exists())
            }
            .addOnFailureListener {
                callback(false)
            }
    }
    
    /**
     * Supprimer un produit des favoris
     */
    fun removeFromFavorites(productId: String, callback: (Boolean, String?) -> Unit) {
        val userId = currentUser?.uid ?: return callback(false, "Utilisateur non connecté")
        
        db.collection("users")
            .document(userId)
            .collection("favorites")
            .document(productId)
            .delete()
            .addOnSuccessListener { 
                callback(true, null) 
            }
            .addOnFailureListener { 
                callback(false, it.message) 
            }
    }
    
    // ===== COMMANDES =====
    
    /**
     * Créer une nouvelle commande
     */
    fun createOrder(
        items: List<CartItem>,
        totalAmount: Double,
        shippingAddress: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val userId = currentUser?.uid ?: return callback(false, "Utilisateur non connecté")
        
        // Créer l'objet commande
        val order = hashMapOf(
            "userId" to userId,
            "totalAmount" to totalAmount,
            "shippingAddress" to shippingAddress,
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp()
        )
        
        // Ajouter la commande à Firestore
        db.collection("orders")
            .add(order)
            .addOnSuccessListener { docRef ->
                // Ajouter les éléments de la commande
                val batch = db.batch()
                
                items.forEachIndexed { _, item ->
                    val itemRef = db.collection("orders")
                        .document(docRef.id)
                        .collection("items")
                        .document()
                    
                    batch.set(itemRef, hashMapOf(
                        "productId" to item.productId,
                        "quantity" to item.quantity,
                        "price" to item.productPrice
                    ))
                    
                    // Supprimer l'élément du panier
                    val cartItemRef = db.collection("users")
                        .document(userId)
                        .collection("cart")
                        .document(item.id)
                    
                    batch.delete(cartItemRef)
                }
                
                // Exécuter le batch
                batch.commit()
                    .addOnSuccessListener { 
                        callback(true, docRef.id) 
                    }
                    .addOnFailureListener { 
                        callback(false, it.message) 
                    }
            }
            .addOnFailureListener { 
                callback(false, it.message) 
            }
    }
} 