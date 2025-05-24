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
        binding.progressBar.visibility = View.VISIBLE
        
        firestore.collection("produits")
            .document(productId!!)
            .get()
            .addOnSuccessListener { document ->
                binding.progressBar.visibility = View.GONE
                
                if (document != null && document.exists()) {
                    try {
                        val images = document.get("images") as? List<String> ?: listOf()
                        val product = Product(
                            id = document.id,
                            title = document.getString("titre") ?: "",
                            description = document.getString("description") ?: "",
                            price = document.getDouble("prix") ?: 0.0,
                            images = images,
                            category = document.getString("categorie") ?: "",
                            sellerId = document.getString("vendeurId") ?: "",
                            sellerName = document.getString("vendeurNom") ?: "",
                            condition = document.getString("etat") ?: "",
                            location = document.getString("localisation") ?: "",
                            createdAt = document.getLong("createdAt") ?: System.currentTimeMillis()
                        )
                        currentProduct = product
                        updateUI(product)
                        checkIfFavorite(product)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing product data", e)
                        Toast.makeText(this, "Erreur lors du chargement des données", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Produit non trouvé", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e(TAG, "Error loading product: ${e.message}")
                Toast.makeText(this, "Erreur lors du chargement du produit", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun updateUI(product: Product) {
        try {
            binding.apply {
                toolbar.title = product.title
                
                // Image du produit
                val imageUrl = product.images.firstOrNull()
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this@ProductDetailActivity)
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(productImage)
                } else {
                    productImage.setImageResource(R.drawable.placeholder_image)
                }

                // Informations du produit
                productTitle.text = product.title
                productDescription.text = product.description
                
                // Prix
                val format = NumberFormat.getCurrencyInstance(Locale.FRANCE)
                productPrice.text = format.format(product.price)
                
                // Localisation
                productLocation.text = product.location.ifEmpty { "Localisation non spécifiée" }
                
                // Informations vendeur
                sellerName.text = product.sellerName.ifEmpty { "Vendeur anonyme" }
                
                // Date de publication
                val date = Date(product.createdAt)
                publishDate.text = android.text.format.DateFormat.getDateFormat(this@ProductDetailActivity)
                    .format(date)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating UI", e)
            Toast.makeText(this, "Erreur lors de l'affichage des données", Toast.LENGTH_SHORT).show()
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