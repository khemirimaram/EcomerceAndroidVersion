package com.example.ecommerce

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.ecommerce.adapters.ProductAdapter
import com.example.ecommerce.databinding.ActivityFavoritesBinding
import com.example.ecommerce.models.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class FavoritesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFavoritesBinding
    private lateinit var productAdapter: ProductAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        setupToolbar()
        setupRecyclerView()
        loadFavorites()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Mes Favoris"
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }
    
    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            onProductClick = { product ->
                val intent = Intent(this, ProductDetailActivity::class.java)
                intent.putExtra("PRODUCT_ID", product.id)
                startActivity(intent)
            },
            onFavoriteClick = { product ->
                toggleFavorite(product)
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@FavoritesActivity, 2)
            adapter = productAdapter
        }
    }
    
    private fun loadFavorites() {
        showLoading(true)
        
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showEmptyState("Connectez-vous pour voir vos favoris")
            return
        }
        
        firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .get()
            .addOnSuccessListener { favoritesSnapshot ->
                if (favoritesSnapshot.isEmpty) {
                    showEmptyState("Aucun favori")
                    return@addOnSuccessListener
                }
                
                val productIds = favoritesSnapshot.documents.map { it.id }
                val allProducts = mutableListOf<Product>()
                var loadedCollections = 0
                
                // Fonction pour traiter les résultats
                fun processResults() {
                    if (loadedCollections == 2) { // Les deux collections ont été chargées
                        showLoading(false)
                        if (allProducts.isEmpty()) {
                            showEmptyState("Aucun favori")
                        } else {
                            binding.emptyView.visibility = View.GONE
                            binding.recyclerView.visibility = View.VISIBLE
                            productAdapter.submitList(allProducts)
                        }
                    }
                }
                
                // Chercher dans la collection "produits"
                firestore.collection("produits")
                    .whereIn("__name__", productIds)
                    .get()
                    .addOnSuccessListener { productsSnapshot ->
                        productsSnapshot.documents.mapNotNull { document ->
                            try {
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
                                    sellerId = document.getString("sellerId") ?: document.getString("vendeurId") ?: "",
                                    sellerName = document.getString("sellerName") ?: document.getString("vendeurNom") ?: "",
                                    sellerPhoto = document.getString("sellerPhoto"),
                                    createdAt = when (val timestamp = document.get("createdAt")) {
                                        is com.google.firebase.Timestamp -> timestamp.seconds * 1000
                                        is Date -> timestamp.time
                                        is Long -> timestamp
                                        else -> System.currentTimeMillis()
                                    },
                                    status = document.getString("status") ?: "active",
                                    isFavorite = true
                                )
                                allProducts.add(product)
                            } catch (e: Exception) {
                                Log.e("FavoritesActivity", "Error converting document: ${e.message}")
                                null
                            }
                        }
                        loadedCollections++
                        processResults()
                    }
                    .addOnFailureListener { e ->
                        Log.e("FavoritesActivity", "Error loading products from 'produits': ${e.message}")
                        loadedCollections++
                        processResults()
                    }
                
                // Chercher dans la collection "products"
                firestore.collection("products")
                    .whereIn("__name__", productIds)
                    .get()
                    .addOnSuccessListener { productsSnapshot ->
                        productsSnapshot.documents.mapNotNull { document ->
                            try {
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
                                    sellerId = document.getString("sellerId") ?: document.getString("vendeurId") ?: "",
                                    sellerName = document.getString("sellerName") ?: document.getString("vendeurNom") ?: "",
                                    sellerPhoto = document.getString("sellerPhoto"),
                                    createdAt = when (val timestamp = document.get("createdAt")) {
                                        is com.google.firebase.Timestamp -> timestamp.seconds * 1000
                                        is Date -> timestamp.time
                                        is Long -> timestamp
                                        else -> System.currentTimeMillis()
                                    },
                                    status = document.getString("status") ?: "active",
                                    isFavorite = true
                                )
                                allProducts.add(product)
                            } catch (e: Exception) {
                                Log.e("FavoritesActivity", "Error converting document: ${e.message}")
                                null
                            }
                        }
                        loadedCollections++
                        processResults()
                    }
                    .addOnFailureListener { e ->
                        Log.e("FavoritesActivity", "Error loading products from 'products': ${e.message}")
                        loadedCollections++
                        processResults()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FavoritesActivity", "Error loading favorites: ${e.message}")
                showLoading(false)
                showEmptyState("Erreur lors du chargement des favoris")
            }
    }
    
    private fun toggleFavorite(product: Product) {
        val userId = auth.currentUser?.uid ?: return
        val favoriteRef = firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .document(product.id)
        
        favoriteRef.delete()
            .addOnSuccessListener {
                val currentList = productAdapter.currentList.toMutableList()
                currentList.removeAll { it.id == product.id }
                productAdapter.submitList(currentList)
                
                if (currentList.isEmpty()) {
                    showEmptyState("Aucun favori")
                }
            }
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    private fun showEmptyState(message: String) {
        binding.recyclerView.visibility = View.GONE
        binding.emptyView.visibility = View.VISIBLE
        binding.emptyText.text = message
    }
} 