package com.example.ecommerce.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecommerce.R
import com.example.ecommerce.adapters.CategoryAdapter
import com.example.ecommerce.adapters.ProductAdapter
import com.example.ecommerce.databinding.FragmentHomeBinding
import com.example.ecommerce.models.Product
import com.example.ecommerce.ProductDetailActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var productAdapter: ProductAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var selectedCategory: String = "Tout"
    
    companion object {
        private const val TAG = "HomeFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        setupRecyclerViews()
        setupSwipeRefresh()
        loadProducts()
    }

    private fun setupRecyclerViews() {
        // Setup categories
        val categories = resources.getStringArray(R.array.product_categories).toList()
        categoryAdapter = CategoryAdapter(categories) { category ->
            selectedCategory = category
            loadProducts()
        }
        
        binding.categoriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        // Setup products
        productAdapter = ProductAdapter(
            onProductClick = { product -> handleProductClick(product) },
            onFavoriteClick = { product -> handleFavoriteClick(product) }
        )
        
        binding.productsRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = productAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadProducts()
        }
    }

    private fun loadProducts() {
        showLoading(true)
        Log.d(TAG, "Début du chargement des produits depuis Firestore")
        
        var query = firestore.collection("produits")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            
        if (selectedCategory != "Tout") {
            query = query.whereEqualTo("category", selectedCategory)
        }

        query.get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Réponse reçue de Firestore - Nombre de documents : ${documents.size()}")
                
                if (documents.isEmpty) {
                    Log.d(TAG, "La collection est vide")
                    showEmptyState(true)
                    showLoading(false)
                    return@addOnSuccessListener
                }

                documents.forEach { doc ->
                    Log.d(TAG, """
                        Document brut :
                        - ID: ${doc.id}
                        - Données: ${doc.data}
                    """.trimIndent())
                }
                
                val products = documents.mapNotNull { document ->
                    try {
                        val product = Product(
                            id = document.id,
                            name = document.getString("name") ?: document.getString("titre") ?: "",
                            description = document.getString("description") ?: "",
                            price = document.getDouble("price") ?: document.getDouble("prix") ?: 0.0,
                            quantity = document.getLong("quantity")?.toInt() ?: 1,
                            category = document.getString("category") ?: document.getString("categorie") ?: "",
                            condition = document.getString("condition") ?: document.getString("etat") ?: "",
                            images = (document.get("images") as? List<*>)?.filterIsInstance<String>() ?: listOf(),
                            sellerId = document.getString("sellerId") ?: document.getString("vendeurId") ?: "",
                            sellerName = document.getString("sellerName") ?: document.getString("vendeurNom") ?: "",
                            createdAt = document.getLong("createdAt") ?: document.getLong("dateCreation") ?: System.currentTimeMillis(),
                            status = document.getString("status") ?: "active"
                        )
                        
                        Log.d(TAG, """
                            Produit converti avec succès :
                            - ID: ${product.id}
                            - Nom: ${product.name}
                            - Prix: ${product.price}
                            - Images: ${product.images}
                            - CreatedAt: ${product.createdAt}
                            - Status: ${product.status}
                        """.trimIndent())
                        
                        // Vérifier si le produit est dans les favoris de l'utilisateur
                        if (auth.currentUser != null) {
                            firestore.collection("users")
                                .document(auth.currentUser!!.uid)
                                .collection("favorites")
                                .document(product.id)
                                .get()
                                .addOnSuccessListener { favoriteDoc ->
                                    val updatedProduct = product.copy(isFavorite = favoriteDoc.exists())
                                    val currentList = productAdapter.currentList.toMutableList()
                                    val index = currentList.indexOfFirst { it.id == product.id }
                                    if (index != -1) {
                                        currentList[index] = updatedProduct
                                        productAdapter.submitList(currentList)
                                    }
                                }
                        }
                        
                        product
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur lors de la conversion du document ${document.id} : ${e.message}")
                        e.printStackTrace()
                        null
                    }
                }
                
                showLoading(false)
                
                if (products.isEmpty()) {
                    Log.d(TAG, "Aucun produit n'a pu être converti, affichage de l'état vide")
                    showEmptyState(true)
                } else {
                    Log.d(TAG, "${products.size} produits chargés avec succès")
                    showEmptyState(false)
                    productAdapter.submitList(products)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erreur lors du chargement des produits : ${e.message}")
                e.printStackTrace()
                showLoading(false)
                showError("Erreur lors du chargement des produits: ${e.message}")
            }
    }

    private fun handleProductClick(product: Product) {
        val intent = Intent(requireContext(), ProductDetailActivity::class.java)
        intent.putExtra("PRODUCT_ID", product.id)
        startActivity(intent)
    }

    private fun handleFavoriteClick(product: Product) {
        if (auth.currentUser == null) {
            Toast.makeText(requireContext(), "Veuillez vous connecter pour ajouter aux favoris", Toast.LENGTH_LONG).show()
            return
        }

        val userId = auth.currentUser!!.uid
        val favoriteRef = firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .document(product.id)

        if (product.isFavorite) {
            favoriteRef.delete()
                .addOnSuccessListener {
                    updateProductFavoriteStatus(product, false)
                }
        } else {
            favoriteRef.set(mapOf("productId" to product.id))
                .addOnSuccessListener {
                    updateProductFavoriteStatus(product, true)
                }
        }
    }

    private fun updateProductFavoriteStatus(product: Product, isFavorite: Boolean) {
        val currentList = productAdapter.currentList.toMutableList()
        val index = currentList.indexOfFirst { it.id == product.id }
        if (index != -1) {
            currentList[index] = product.copy(isFavorite = isFavorite)
            productAdapter.submitList(currentList)
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.swipeRefreshLayout.isRefreshing = show
    }

    private fun showEmptyState(show: Boolean) {
        binding.layoutEmpty.root.visibility = if (show) View.VISIBLE else View.GONE
        binding.productsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 