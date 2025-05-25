package com.example.ecommerce

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import com.example.ecommerce.adapters.ProductAdapter
import com.example.ecommerce.databinding.ActivityMainBinding
import com.example.ecommerce.models.Product
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import com.google.firebase.firestore.FirebaseFirestoreException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var userActionsContainer: LinearLayout
    private lateinit var profileIcon: ImageView
    private lateinit var loginButton: MaterialButton
    private lateinit var registerButton: MaterialButton
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var productAdapter: ProductAdapter
    private lateinit var productsRecyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var searchEditText: EditText
    private lateinit var categoryChipGroup: ChipGroup

    private var currentCategory: String = "Tous"
    private var currentQuery: String = ""

    private val PRODUCTS_PER_PAGE = 10
    private var lastLoadedProduct: DocumentSnapshot? = null
    private var isLoadingMore = false
    private var hasMoreProducts = true

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Vérifier Google Play Services
            if (!(application as ECommerceApp).checkGooglePlayServices(this)) {
                Log.e(TAG, "Google Play Services n'est pas disponible")
                return
            }
            
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Initialize Firebase
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()

            // Initialize views
            initializeViews()
            setupRecyclerView()
            setupClickListeners()
            setupBottomNavigation()
            setupSwipeRefresh()
            setupSearch()
            setupCategoryFilter()
            
            // Update UI based on auth state
            updateUIForAuthState()

            // Load initial data
            loadProducts()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'initialisation de MainActivity", e)
            Toast.makeText(this, "Une erreur s'est produite", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            // Check if user is signed in and update UI accordingly
            updateUIForAuthState()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onStart: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            updateUIForAuthState()
            loadProducts() // Recharger les produits au retour sur l'activité
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onResume: ${e.message}")
        }
    }

    private fun initializeViews() {
        try {
            userActionsContainer = binding.userActionsContainer
            profileIcon = binding.profileIcon
            loginButton = binding.loginButton
            registerButton = binding.registerButton
            bottomNav = binding.bottomNav
            productsRecyclerView = binding.productsRecyclerView
            swipeRefreshLayout = binding.swipeRefreshLayout
            searchEditText = binding.searchEditText
            categoryChipGroup = binding.categoryChipGroup
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing views: ${e.message}")
            throw e
        }
    }

    private fun setupClickListeners() {
        try {
            loginButton.setOnClickListener {
                startActivity(Intent(this, LoginActivity::class.java))
            }

            registerButton.setOnClickListener {
                startActivity(Intent(this, RegisterActivity::class.java))
            }

            profileIcon.setOnClickListener {
                if (auth.currentUser != null) {
                    startActivity(Intent(this, ProfileActivity::class.java))
                } else {
                    updateUIForAuthState()
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up click listeners: ${e.message}")
        }
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    // Déjà sur la page d'accueil
                    true
                }
                R.id.navigation_favorites -> {
                    if (checkAuthAndRedirect()) {
                        startActivity(Intent(this, FavoritesActivity::class.java))
                        true
                    } else false
                }
                R.id.navigation_add -> {
                    if (checkAuthAndRedirect()) {
                        startActivity(Intent(this, AddProductActivity::class.java))
                        true
                    } else false
                }
                R.id.navigation_messages -> {
                    if (checkAuthAndRedirect()) {
                        startActivity(Intent(this, com.example.ecommerce.ui.messages.MessagesActivity::class.java))
                        true
                    } else false
                }
                R.id.navigation_profile -> {
                    if (checkAuthAndRedirect()) {
                        startActivity(Intent(this, ProfileActivity::class.java))
                        true
                    } else false
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            onProductClick = { product ->
                startActivity(Intent(this, ProductDetailActivity::class.java).apply {
                    putExtra("PRODUCT_ID", product.id)
                })
            },
            onFavoriteClick = { product ->
                if (auth.currentUser != null) {
                    toggleFavorite(product)
                } else {
                    showLoginPrompt()
                }
            }
        )

        binding.productsRecyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = productAdapter
            
            // Add scroll listener for pagination
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val layoutManager = recyclerView.layoutManager as GridLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if (!isLoadingMore && hasMoreProducts) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                            loadProducts(isRefresh = false)
                        }
                    }
                }
            })
        }

        swipeRefreshLayout.setOnRefreshListener {
            loadProducts()
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            loadProducts()
        }
    }

    private fun setupSearch() {
        searchEditText.setOnEditorActionListener { _, _, _ ->
            currentQuery = searchEditText.text.toString()
            loadProducts()
            true
        }
    }

    private fun setupCategoryFilter() {
        categoryChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            val chip = if (checkedId != null) group.findViewById<View>(checkedId) else null
            currentCategory = if (chip != null) {
                (chip as? com.google.android.material.chip.Chip)?.text.toString()
            } else {
                "Tous"
            }
            loadProducts()
        }
    }

    private fun getTimestampMillis(document: DocumentSnapshot, field: String): Long {
        return try {
            when (val timestamp = document.get(field)) {
                is Long -> timestamp
                is com.google.firebase.Timestamp -> timestamp.seconds * 1000 + timestamp.nanoseconds / 1000000
                is java.util.Date -> timestamp.time
                else -> {
                    Log.w("MainActivity", "Format de timestamp non reconnu pour $field: $timestamp")
                    System.currentTimeMillis()
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Erreur lors de la conversion du timestamp pour $field", e)
            System.currentTimeMillis()
        }
    }

    private fun loadProducts(isRefresh: Boolean = true) {
        try {
            if (isRefresh) {
                swipeRefreshLayout.isRefreshing = true
                lastLoadedProduct = null
                hasMoreProducts = true
            }
            
            if (!hasMoreProducts || isLoadingMore) {
                swipeRefreshLayout.isRefreshing = false
                return
            }

            isLoadingMore = true
            Log.d(TAG, "=== Début du chargement des produits ===")
            
            // Build base query - Simplified to avoid index issues
            val productsRef = firestore.collection("produits")
            var query = productsRef.orderBy("createdAt", Query.Direction.DESCENDING)
            
            // Apply category filter if needed
            if (currentCategory != "Tous") {
                query = productsRef.whereEqualTo("category", currentCategory)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
            }
            
            // Apply search filter if needed
            if (currentQuery.isNotEmpty()) {
                query = productsRef.whereArrayContains("searchKeywords", currentQuery.lowercase())
                    .orderBy("createdAt", Query.Direction.DESCENDING)
            }
            
            // Apply limit
            query = query.limit(PRODUCTS_PER_PAGE.toLong())
            
            // Apply startAfter if not first page
            if (lastLoadedProduct != null) {
                query = query.startAfter(lastLoadedProduct!!)
            }

            query.get().addOnSuccessListener { documents ->
                lifecycleScope.launch {
                    try {
                        // Update pagination state
                        hasMoreProducts = documents.size() == PRODUCTS_PER_PAGE
                        lastLoadedProduct = documents.lastOrNull()
                        
                        // Convert documents to products and filter active ones in memory
                        val newProducts = documents.mapNotNull { doc ->
                            try {
                                val status = doc.getString("status") ?: "active"
                                if (status != "active") return@mapNotNull null
                                
                                Product(
                                    id = doc.id,
                                    name = doc.getString("name") ?: "",
                                    description = doc.getString("description") ?: "",
                                    price = doc.getDouble("price") ?: 0.0,
                                    quantity = doc.getLong("quantity")?.toInt() ?: 1,
                                    category = doc.getString("category") ?: "",
                                    condition = doc.getString("condition") ?: "",
                                    images = when (val imagesData = doc.get("images")) {
                                        is List<*> -> imagesData.filterIsInstance<String>()
                                        is String -> listOf(imagesData)
                                        else -> listOf()
                                    },
                                    sellerId = doc.getString("sellerId") ?: "",
                                    sellerName = doc.getString("sellerName") ?: "",
                                    sellerPhoto = doc.getString("sellerPhoto"),
                                    createdAt = when (val timestamp = doc.get("createdAt")) {
                                        is com.google.firebase.Timestamp -> timestamp.seconds * 1000
                                        is Date -> timestamp.time
                                        is Long -> timestamp
                                        else -> System.currentTimeMillis()
                                    },
                                    status = status
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error converting document ${doc.id}", e)
                                null
                            }
                        }

                        // Update UI
                        if (isRefresh) {
                            if (newProducts.isEmpty()) {
                                showEmptyState()
                            } else {
                                showProducts(newProducts)
                            }
                        } else {
                            // Append new products to existing list
                            val currentList = productAdapter.currentList.toMutableList()
                            currentList.addAll(newProducts)
                            showProducts(currentList)
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing products", e)
                        showError("Erreur lors du chargement des produits")
                    } finally {
                        isLoadingMore = false
                        swipeRefreshLayout.isRefreshing = false
                    }
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error loading products", e)
                showError("Impossible de charger les produits")
                isLoadingMore = false
                swipeRefreshLayout.isRefreshing = false
            }
                
        } catch (e: Exception) {
            Log.e(TAG, "Error in loadProducts", e)
            isLoadingMore = false
            swipeRefreshLayout.isRefreshing = false
            Toast.makeText(this, "Une erreur s'est produite", Toast.LENGTH_SHORT).show()
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

        // Mettre à jour l'UI
        val updatedProducts = productAdapter.currentList.toMutableList()
        val position = updatedProducts.indexOfFirst { it.id == product.id }
        if (position != -1) {
            updatedProducts[position] = product.copy(isFavorite = !product.isFavorite)
            productAdapter.submitList(updatedProducts)
        }
    }

    private fun showLoginPrompt() {
        Toast.makeText(this, "Connectez-vous pour ajouter aux favoris", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
    }

    private fun updateUIForAuthState() {
        try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // User is signed in
                userActionsContainer.visibility = View.GONE
                profileIcon.visibility = View.VISIBLE
                bottomNav.visibility = View.VISIBLE
            } else {
                // No user is signed in
                userActionsContainer.visibility = View.VISIBLE
                profileIcon.visibility = View.GONE
                bottomNav.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error updating UI state: ${e.message}")
            // En cas d'erreur, on affiche les boutons de connexion par défaut
            try {
                userActionsContainer.visibility = View.VISIBLE
                profileIcon.visibility = View.GONE
                bottomNav.visibility = View.GONE
            } catch (e2: Exception) {
                Log.e("MainActivity", "Critical error updating UI state: ${e2.message}")
            }
        }
    }

    private fun checkAuthAndRedirect(): Boolean {
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            return false
        }
        return true
    }

    private fun showEmptyState() {
        binding.emptyView.visibility = View.VISIBLE
        binding.productsRecyclerView.visibility = View.GONE
    }

    private fun showProducts(products: List<Product>) {
        binding.emptyView.visibility = View.GONE
        binding.productsRecyclerView.visibility = View.VISIBLE
        productAdapter.submitList(products)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}