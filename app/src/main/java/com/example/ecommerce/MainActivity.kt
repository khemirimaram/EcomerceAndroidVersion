package com.example.ecommerce

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.ecommerce.adapters.ProductAdapter
import com.example.ecommerce.databinding.ActivityMainBinding
import com.example.ecommerce.models.Product

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
    private lateinit var addProductFab: FloatingActionButton

    private var currentCategory: String = "Tous"
    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            // Initialize Firebase
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()

            // Test button for adding product
            binding.addProductFab.setOnClickListener {
                testAddProduct()
            }

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
            Log.e("MainActivity", "Error in onCreate: ${e.message}")
            Toast.makeText(this, "Une erreur s'est produite", Toast.LENGTH_SHORT).show()
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
            addProductFab = binding.addProductFab
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
                        // Naviguer vers les favoris
                        true
                    } else false
                }
                R.id.navigation_add -> {
                    if (checkAuthAndRedirect()) {
                        // Naviguer vers l'ajout
                        startActivity(Intent(this, AddProductActivity::class.java))
                        true
                    } else false
                }
                R.id.navigation_messages -> {
                    if (checkAuthAndRedirect()) {
                        // Naviguer vers les messages
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
                // Ouvrir les détails du produit
                val intent = Intent(this, ProductDetailActivity::class.java)
                intent.putExtra("PRODUCT_ID", product.id)
                startActivity(intent)
            },
            onFavoriteClick = { product ->
                if (auth.currentUser != null) {
                    toggleFavorite(product)
                } else {
                    showLoginPrompt()
                }
            }
        )

        productsRecyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = productAdapter
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
        categoryChipGroup.setOnCheckedChangeListener { group, checkedId ->
            val chip = group.findViewById<View>(checkedId)
            currentCategory = if (chip != null) {
                (chip as? com.google.android.material.chip.Chip)?.text.toString()
            } else {
                "Tous"
            }
            loadProducts()
        }
    }

    private fun loadProducts() {
        try {
            swipeRefreshLayout.isRefreshing = true
            Log.d("MainActivity", "=== Début du chargement des produits ===")
            Log.d("MainActivity", "État de Firestore: ${firestore != null}")
            Log.d("MainActivity", "État de l'authentification: ${auth.currentUser?.uid ?: "Non connecté"}")

            // Requête simplifiée sans index composite
            var query = if (currentCategory != "Tous") {
                // Si une catégorie est sélectionnée, on filtre d'abord par catégorie
                firestore.collection("produits")
                    .whereEqualTo("categorie", currentCategory)
            } else {
                // Sinon, on trie juste par date
                firestore.collection("produits")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
            }

            // Appliquer la recherche si nécessaire
            if (currentQuery.isNotEmpty()) {
                query = query.whereArrayContains("motsCles", currentQuery.lowercase())
            }

            Log.d("MainActivity", "Exécution de la requête...")
            query.get()
                .addOnSuccessListener { documents ->
                    Log.d("MainActivity", "=== Résultats de la requête ===")
                    Log.d("MainActivity", "Nombre de documents reçus: ${documents.size()}")
                    
                    if (documents.isEmpty) {
                        Log.d("MainActivity", "La collection est vide")
                        binding.emptyView?.visibility = View.VISIBLE
                        binding.productsRecyclerView.visibility = View.GONE
                        swipeRefreshLayout.isRefreshing = false
                        return@addOnSuccessListener
                    }

                    val products = documents.mapNotNull { document ->
                        try {
                            val product = document.toObject(Product::class.java)
                            product?.copy(id = document.id)
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Erreur de conversion pour le document ${document.id}", e)
                            null
                        }
                    }

                    // Trier les résultats en mémoire si nécessaire
                    val sortedProducts = if (currentCategory != "Tous") {
                        products.sortedByDescending { it.createdAt }
                    } else {
                        products
                    }

                    productAdapter.submitList(sortedProducts)
                    binding.emptyView?.visibility = if (sortedProducts.isEmpty()) View.VISIBLE else View.GONE
                    binding.productsRecyclerView.visibility = if (sortedProducts.isEmpty()) View.GONE else View.VISIBLE
                    swipeRefreshLayout.isRefreshing = false
                }
                .addOnFailureListener { exception ->
                    Log.e("MainActivity", "Erreur lors de la récupération des produits", exception)
                    Log.e("MainActivity", "Message d'erreur: ${exception.message}")
                    Log.e("MainActivity", "Cause: ${exception.cause}")
                    Toast.makeText(this, "Erreur: ${exception.message}", Toast.LENGTH_SHORT).show()
                    binding.emptyView?.visibility = View.VISIBLE
                    binding.productsRecyclerView.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                }
        } catch (e: Exception) {
            Log.e("MainActivity", "Erreur critique dans loadProducts", e)
            Log.e("MainActivity", "Message: ${e.message}")
            Log.e("MainActivity", "Stack trace: ${e.stackTraceToString()}")
            Toast.makeText(this, "Erreur critique: ${e.message}", Toast.LENGTH_LONG).show()
            binding.emptyView?.visibility = View.VISIBLE
            binding.productsRecyclerView.visibility = View.GONE
            swipeRefreshLayout.isRefreshing = false
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
                addProductFab.show()
            } else {
                // No user is signed in
                userActionsContainer.visibility = View.VISIBLE
                profileIcon.visibility = View.GONE
                bottomNav.visibility = View.GONE
                addProductFab.hide()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error updating UI state: ${e.message}")
            // En cas d'erreur, on affiche les boutons de connexion par défaut
            try {
                userActionsContainer.visibility = View.VISIBLE
                profileIcon.visibility = View.GONE
                bottomNav.visibility = View.GONE
                addProductFab.hide()
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

    private fun testAddProduct() {
        val testProduct = hashMapOf(
            "title" to "Produit Test",
            "description" to "Description du produit test",
            "price" to 99.99,
            "categorie" to "Test",
            "dateCreation" to com.google.firebase.Timestamp.now(),
            "motsCles" to listOf("test", "produit"),
            "imageUrl" to "",
            "sellerName" to "Test Vendeur",
            "location" to "Test Location"
        )

        firestore.collection("produits")
            .add(testProduct)
            .addOnSuccessListener { documentReference ->
                Log.d("MainActivity", "Produit test ajouté avec ID: ${documentReference.id}")
                Toast.makeText(this, "Produit test ajouté avec succès", Toast.LENGTH_SHORT).show()
                loadProducts() // Recharger les produits
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Erreur lors de l'ajout du produit test", e)
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}