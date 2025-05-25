package com.example.ecommerce

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.example.ecommerce.databinding.ActivityProductDetailBinding
import com.example.ecommerce.models.Product
import java.text.NumberFormat
import java.util.*

class ProductDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductDetailBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var productId: String? = null
    private var currentProduct: Product? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()

            productId = intent.getStringExtra("PRODUCT_ID")
            if (productId == null) {
                Toast.makeText(this, "Erreur: ID du produit manquant", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            setupToolbar()
            loadProduct()
            setupClickListeners()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}")
            Toast.makeText(this, "Une erreur s'est produite", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupClickListeners() {
        binding.contactButton.setOnClickListener {
            currentProduct?.let { product ->
                if (auth.currentUser == null) {
                    showLoginPrompt()
                } else {
                    startChat(product)
                }
            }
        }

        binding.favoriteButton.setOnClickListener {
            currentProduct?.let { product ->
                if (auth.currentUser == null) {
                    showLoginPrompt()
                } else {
                    toggleFavorite(product)
                }
            }
        }
    }

    private fun loadProduct() {
        productId?.let { id ->
            binding.progressBar.visibility = View.VISIBLE
            
            // Essayer d'abord dans la collection "produits"
            firestore.collection("produits")
                .document(id)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        handleProductDocument(document)
                    } else {
                        // Si le produit n'est pas trouvé, essayer dans la collection "products"
                        firestore.collection("products")
                            .document(id)
                            .get()
                            .addOnSuccessListener { webDocument ->
                                if (webDocument != null && webDocument.exists()) {
                                    handleProductDocument(webDocument)
                                } else {
                                    binding.progressBar.visibility = View.GONE
                                    Toast.makeText(this, "Produit non trouvé", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            }
                            .addOnFailureListener { e ->
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
        }
    }

    private fun handleProductDocument(document: DocumentSnapshot) {
        try {
            Log.d(TAG, "Document data: ${document.data}")
            val isExchangeAvailable = document.getBoolean("isAvailableForExchange") ?: false
            val exchangePrefs = document.getString("exchangePreferences")
            
            // Récupérer l'ID du vendeur
            val sellerId = document.getString("sellerId") ?: document.getString("vendeurId") ?: ""
            val initialSellerName = document.getString("sellerName") ?: document.getString("vendeurNom")
            
            // Si le nom du vendeur n'est pas disponible ou est égal à l'ID, chercher dans la collection users
            if (initialSellerName.isNullOrEmpty() || initialSellerName == sellerId) {
                firestore.collection("users")
                    .document(sellerId)
                    .get()
                    .addOnSuccessListener { userDoc ->
                        val updatedSellerName = userDoc.getString("displayName") 
                            ?: userDoc.getString("name") 
                            ?: "Utilisateur"
                        createAndUpdateProduct(
                            document, sellerId, updatedSellerName, isExchangeAvailable, 
                            exchangePrefs
                        )
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Erreur lors de la récupération des infos vendeur", e)
                        createAndUpdateProduct(
                            document, sellerId, "Utilisateur", isExchangeAvailable, 
                            exchangePrefs
                        )
                    }
            } else {
                createAndUpdateProduct(
                    document, sellerId, initialSellerName, isExchangeAvailable, 
                    exchangePrefs
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la conversion du document", e)
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Erreur lors du chargement du produit", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun createAndUpdateProduct(
        document: DocumentSnapshot,
        sellerId: String,
        sellerName: String,
        isExchangeAvailable: Boolean,
        exchangePrefs: String?
    ) {
        val product = Product(
            id = document.id,
            name = document.getString("name") ?: document.getString("titre") ?: "",
            description = document.getString("description") ?: "",
            price = document.getDouble("price") ?: document.getDouble("prix") ?: 0.0,
            quantity = document.getLong("quantity")?.toInt() ?: 1,
            category = document.getString("category") ?: document.getString("categorie") ?: "",
            condition = document.getString("condition") ?: document.getString("etat") ?: "",
            images = when (val imagesData = document.get("images")) {
                is List<*> -> imagesData.filterIsInstance<String>()
                is String -> listOf(imagesData)
                else -> listOf()
            },
            sellerId = sellerId,
            sellerName = sellerName,
            sellerPhoto = document.getString("sellerPhoto"),
            isAvailableForExchange = isExchangeAvailable,
            exchangePreferences = exchangePrefs,
            createdAt = when (val timestamp = document.get("createdAt")) {
                is com.google.firebase.Timestamp -> timestamp.seconds * 1000
                is Date -> timestamp.time
                is Long -> timestamp
                else -> System.currentTimeMillis()
            },
            status = document.getString("status") ?: "active",
            location = document.getString("location") ?: ""
        )
        
        currentProduct = product
        updateUI(product)
        checkIfFavorite(product)
        binding.progressBar.visibility = View.GONE
    }

    private fun updateSellerInfo(name: String, photoUrl: String?) {
        binding.sellerName.text = name
        if (!photoUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ic_person)
                .into(binding.sellerAvatar)
        }
    }

    private fun updateUI(product: Product) {
        try {
            Log.d(TAG, "Updating UI with product: ${product.name}")
            Log.d(TAG, "Seller name before setting: ${product.sellerName}")
            
            binding.apply {
                productTitle.text = product.name
                productPrice.text = String.format("%.2f DT", product.price)
                productDescription.text = product.description
                
                // Afficher la localisation
                productLocation.apply {
                    text = product.location
                    visibility = if (product.location.isNotEmpty()) View.VISIBLE else View.GONE
                }
                
                // Gérer la section d'échange
                exchangeSection.visibility = if (product.isAvailableForExchange) View.VISIBLE else View.GONE
                
                if (product.isAvailableForExchange) {
                    exchangePreferences.text = product.exchangePreferences ?: "Ouvert à toutes propositions d'échange"
                }

                // Charger l'image principale
                if (product.images.isNotEmpty()) {
                    Glide.with(this@ProductDetailActivity)
                        .load(product.images[0])
                        .into(productImage)
                }

                // État et catégorie
                productCondition.text = product.condition
                categoryText.text = product.category
                
                // Informations vendeur
                sellerName.text = product.sellerName.ifEmpty { "Vendeur Anonyme" }
                Log.d(TAG, "Seller name set in UI: ${sellerName.text}")
                
                // Si on a une photo de profil du vendeur, l'afficher
                if (!product.sellerPhoto.isNullOrEmpty()) {
                    Glide.with(this@ProductDetailActivity)
                        .load(product.sellerPhoto)
                        .placeholder(R.drawable.ic_person)
                        .into(sellerAvatar)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating UI: ${e.message}")
            Toast.makeText(this, "Erreur lors de l'affichage du produit", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkIfFavorite(product: Product) {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .document(product.id)
            .get()
            .addOnSuccessListener { document ->
                val isFavorite = document.exists()
                updateFavoriteButton(isFavorite)
                currentProduct?.isFavorite = isFavorite
            }
    }

    private fun toggleFavorite(product: Product) {
        val userId = auth.currentUser?.uid ?: return
        val favoriteRef = firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .document(product.id)

        if (product.isFavorite) {
            favoriteRef.delete()
        } else {
            favoriteRef.set(mapOf("productId" to product.id))
        }

        product.isFavorite = !product.isFavorite
        updateFavoriteButton(product.isFavorite)
    }

    private fun updateFavoriteButton(isFavorite: Boolean) {
        binding.favoriteButton.setImageResource(
            if (isFavorite) R.drawable.ic_favorite
            else R.drawable.ic_favorite_border
        )
    }

    private fun startChat(product: Product) {
        // TODO: Implémenter la fonctionnalité de chat
        Toast.makeText(this, "Chat non implémenté", Toast.LENGTH_SHORT).show()
    }

    private fun showLoginPrompt() {
        Toast.makeText(this, "Connectez-vous pour continuer", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
    }

    companion object {
        private const val TAG = "ProductDetailActivity"
    }
} 