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
import kotlinx.coroutines.tasks.await
import java.util.UUID
import android.content.Context

/**
 * Classe de gestion centralisée des interactions avec Firebase.
 * Cette classe est conçue pour être compatible avec la structure de données
 * utilisée par l'application web.
 */
object FirebaseManager {
    private const val TAG = "FirebaseManager"
    // Instances Firebase
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance() 
    private val storage = FirebaseStorage.getInstance()
    
    // Collections
    private val PRODUCTS_COLLECTION = "produits"
    private val USERS_COLLECTION = "users"
    
    // Référence de l'utilisateur courant
    var currentUser = auth.currentUser
    var currentUserData: Map<String, Any>? = null
    var currentProducts = mutableListOf<Product>()
    var currentFavorites = mutableListOf<String>()
    
    // Référence à la collection produits
    private val produitsCollection = db.collection("produits")
    
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
    suspend fun getAllProducts(): List<Product> {
        return try {
            val snapshot = db.collection("products")
                .whereEqualTo("status", "available")
                .whereEqualTo("visibility", "public")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                try {
                    Product(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        description = document.getString("description") ?: "",
                        price = document.getDouble("price") ?: 0.0,
                        quantity = document.getLong("quantity")?.toInt() ?: 1,
                        category = document.getString("category") ?: "",
                        condition = document.getString("condition") ?: "",
                        images = (document.get("images") as? List<*>)?.filterIsInstance<String>() ?: listOf(),
                        sellerId = document.getString("sellerId") ?: "",
                        sellerName = document.getString("sellerName") ?: "",
                        sellerPhoto = document.getString("sellerPhoto"),
                        isAvailableForExchange = document.getBoolean("isAvailableForExchange") ?: false,
                        exchangePreferences = document.getString("exchangePreferences"),
                        createdAt = document.getLong("createdAt") ?: System.currentTimeMillis(),
                        status = document.getString("status") ?: "active"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la conversion du document", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération des produits", e)
            emptyList()
        }
    }
    
    /**
     * Récupérer les produits par catégorie
     */
    suspend fun getProductsByCategory(category: String): List<Product> {
        return try {
            val snapshot = db.collection("products")
                .whereEqualTo("category", category)
                .whereEqualTo("status", "available")
                .whereEqualTo("visibility", "public")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                try {
                    Product(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        description = document.getString("description") ?: "",
                        price = document.getDouble("price") ?: 0.0,
                        quantity = document.getLong("quantity")?.toInt() ?: 1,
                        category = document.getString("category") ?: "",
                        condition = document.getString("condition") ?: "",
                        images = (document.get("images") as? List<*>)?.filterIsInstance<String>() ?: listOf(),
                        sellerId = document.getString("sellerId") ?: "",
                        sellerName = document.getString("sellerName") ?: "",
                        sellerPhoto = document.getString("sellerPhoto"),
                        isAvailableForExchange = document.getBoolean("isAvailableForExchange") ?: false,
                        exchangePreferences = document.getString("exchangePreferences"),
                        createdAt = document.getLong("createdAt") ?: System.currentTimeMillis(),
                        status = document.getString("status") ?: "active"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la conversion du document", e)
                    null
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * Récupérer un produit par son ID
     */
    suspend fun getProductById(productId: String): Product? {
        return try {
            val document = db.collection("products").document(productId).get().await()
            if (document.exists()) {
                Product(
                    id = document.id,
                    name = document.getString("name") ?: "",
                    description = document.getString("description") ?: "",
                    price = document.getDouble("price") ?: 0.0,
                    quantity = document.getLong("quantity")?.toInt() ?: 1,
                    category = document.getString("category") ?: "",
                    condition = document.getString("condition") ?: "",
                    images = (document.get("images") as? List<*>)?.filterIsInstance<String>() ?: listOf(),
                    sellerId = document.getString("sellerId") ?: "",
                    sellerName = document.getString("sellerName") ?: "",
                    sellerPhoto = document.getString("sellerPhoto"),
                    isAvailableForExchange = document.getBoolean("isAvailableForExchange") ?: false,
                    exchangePreferences = document.getString("exchangePreferences"),
                    createdAt = document.getLong("createdAt") ?: System.currentTimeMillis(),
                    status = document.getString("status") ?: "active"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * Télécharger un produit avec images
     */
    fun isUriAccessible(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.close()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Upload a product with multiple images
     */
    fun uploadProduct(product: Product, imageUris: List<Uri>, context: Context, callback: (Boolean, String?) -> Unit) {
        // Validation initiale
        if (imageUris.isEmpty()) {
            callback(false, "Aucune image sélectionnée")
            return
        }

        if (getCurrentUserId() == null) {
            callback(false, "Utilisateur non connecté")
            return
        }

        val productId = UUID.randomUUID().toString()
        val imageUrls = mutableListOf<String>()
        var uploadedCount = 0
        var hasError = false

        // Créer le dossier pour ce produit
        val productFolder = "product_images/$productId"

        imageUris.forEachIndexed { index, uri ->
            try {
                // Vérifier si l'URI est valide
                if (uri.toString().isEmpty()) {
                    Log.e(TAG, "URI invalide à l'index $index")
                    if (!hasError) {
                        hasError = true
                        callback(false, "Image invalide détectée")
                    }
                    return@forEachIndexed
                }

                // Générer un nom de fichier unique
                val fileName = "image_${System.currentTimeMillis()}_$index.jpg"
                val imageRef = storage.reference.child(productFolder).child(fileName)

                // Lire l'image
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    
                    if (bytes.isEmpty()) {
                        Log.e(TAG, "Fichier vide détecté pour l'URI: $uri")
                        if (!hasError) {
                            hasError = true
                            callback(false, "Fichier image vide détecté")
                        }
                        return@forEachIndexed
                    }

                    // Créer les métadonnées
                    val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                        .setContentType("image/jpeg")
                        .setCustomMetadata("productId", productId)
                        .setCustomMetadata("index", index.toString())
                        .build()

                    // Upload avec progression
                    imageRef.putBytes(bytes, metadata)
                        .addOnProgressListener { taskSnapshot ->
                            val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
                            Log.d(TAG, "Upload progress: $progress% for $fileName")
                        }
                        .addOnSuccessListener { taskSnapshot ->
                            Log.d(TAG, "Image uploadée avec succès: $fileName")
                            
                            // Obtenir l'URL de téléchargement
                            imageRef.downloadUrl
                                .addOnSuccessListener { downloadUrl ->
                                    synchronized(imageUrls) {
                                        imageUrls.add(downloadUrl.toString())
                                        uploadedCount++
                                        
                                        // Si toutes les images sont uploadées avec succès
                                        if (uploadedCount == imageUris.size && !hasError) {
                                            // Créer le document du produit
                                            val productData = hashMapOf(
                                                "name" to product.name,
                                                "description" to product.description,
                                                "price" to product.price,
                                                "images" to imageUrls,
                                                "category" to product.category,
                                                "condition" to product.condition,
                                                "sellerId" to (getCurrentUserId() ?: ""),
                                                "sellerName" to (auth.currentUser?.displayName ?: ""),
                                                "createdAt" to FieldValue.serverTimestamp(),
                                                "updatedAt" to FieldValue.serverTimestamp(),
                                                "status" to "available",
                                                "visibility" to "public"
                                            )

                                            // Sauvegarder dans Firestore
                                            db.collection("products")
                                                .document(productId)
                                                .set(productData)
                                                .addOnSuccessListener {
                                                    Log.d(TAG, "Produit ajouté avec succès dans Firestore")
                                                    callback(true, null)
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e(TAG, "Erreur lors de la sauvegarde du produit", e)
                                                    // En cas d'erreur, supprimer les images uploadées
                                                    deleteUploadedImages(productFolder)
                                                    callback(false, "Erreur lors de la sauvegarde du produit: ${e.message}")
                                                }
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Erreur lors de l'obtention de l'URL pour $fileName", e)
                                    if (!hasError) {
                                        hasError = true
                                        callback(false, "Erreur lors de l'obtention de l'URL: ${e.message}")
                                    }
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Erreur lors de l'upload de $fileName", e)
                            if (!hasError) {
                                hasError = true
                                callback(false, "Erreur lors de l'upload: ${e.message}")
                            }
                        }
                } ?: run {
                    Log.e(TAG, "Impossible d'ouvrir le flux pour l'URI: $uri")
                    if (!hasError) {
                        hasError = true
                        callback(false, "Impossible d'accéder au fichier image")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du traitement de l'image", e)
                if (!hasError) {
                    hasError = true
                    callback(false, "Erreur lors du traitement de l'image: ${e.message}")
                }
            }
        }
    }

    /**
     * Supprimer les images uploadées en cas d'erreur
     */
    private fun deleteUploadedImages(productFolder: String) {
        try {
            val folderRef = storage.reference.child(productFolder)
            folderRef.listAll()
                .addOnSuccessListener { result ->
                    result.items.forEach { item ->
                        item.delete()
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Erreur lors de la suppression de ${item.path}", e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Erreur lors de la liste des fichiers à supprimer", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la suppression des images", e)
        }
    }

    /**
     * Récupérer l'ID de l'utilisateur courant
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
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
    suspend fun getCartItems(): List<CartItem> {
        val userId = currentUser?.uid ?: throw Exception("User not logged in")
        val cartItems = mutableListOf<CartItem>()
        
        val result = db.collection("users")
            .document(userId)
            .collection("cart")
            .get()
            .await()
            
        if (result.documents.isEmpty()) {
            return emptyList()
        }
        
        for (doc in result.documents) {
            val productId = doc.getString("productId") ?: ""
            val quantity = doc.getLong("quantity")?.toInt() ?: 1
            
            val timestamp = doc.get("addedAt") as? Timestamp
            val addedAt = timestamp?.seconds?.times(1000)?.plus(timestamp.nanoseconds / 1000000)
                ?: System.currentTimeMillis()
            
            val product = getProductById(productId)
            if (product != null) {
                cartItems.add(
                    CartItem(
                        id = doc.id,
                        productId = productId,
                        userId = userId,
                        productName = product.name,
                        productImage = product.images.firstOrNull() ?: "",
                        productPrice = product.price,
                        quantity = quantity,
                        timestamp = addedAt
                    )
                )
            }
        }
        
        return cartItems
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

    fun isUserLoggedIn(): Boolean = auth.currentUser != null
} 