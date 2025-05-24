package com.example.ecommerce.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.ecommerce.adapters.ProductAdapter
import com.example.ecommerce.databinding.FragmentHomeBinding
import com.example.ecommerce.firebase.FirebaseManager
import com.example.ecommerce.models.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var productAdapter: ProductAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

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
        setupRecyclerView()
        setupSwipeRefresh()
        loadProducts()
    }

    private fun setupRecyclerView() {
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
        
        firestore.collection("produits")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val products = documents.mapNotNull { document ->
                    try {
                        document.toObject(Product::class.java).also { product ->
                            // Vérifier si le produit est dans les favoris de l'utilisateur
                            if (auth.currentUser != null) {
                                firestore.collection("users")
                                    .document(auth.currentUser!!.uid)
                                    .collection("favorites")
                                    .document(product.id)
                                    .get()
                                    .addOnSuccessListener { favoriteDoc ->
                                        product.isFavorite = favoriteDoc.exists()
                                        // Notifier l'adaptateur du changement
                                        productAdapter.notifyItemChanged(
                                            productAdapter.currentList.indexOfFirst { it.id == product.id }
                                        )
                                    }
                            }
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                
                showLoading(false)
                
                if (products.isEmpty()) {
                    showEmptyState(true)
                } else {
                    showEmptyState(false)
                    productAdapter.submitList(products)
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showError("Erreur lors du chargement des produits: ${e.message}")
            }
    }

    private fun handleProductClick(product: Product) {
        // TODO: Implémenter la navigation vers les détails du produit
        Toast.makeText(requireContext(), "Produit sélectionné: ${product.title}", Toast.LENGTH_SHORT).show()
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
                    product.isFavorite = false
                    productAdapter.notifyItemChanged(
                        productAdapter.currentList.indexOfFirst { it.id == product.id }
                    )
                }
        } else {
            favoriteRef.set(mapOf("productId" to product.id))
                .addOnSuccessListener {
                    product.isFavorite = true
                    productAdapter.notifyItemChanged(
                        productAdapter.currentList.indexOfFirst { it.id == product.id }
                    )
                }
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