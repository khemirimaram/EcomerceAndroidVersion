package com.example.ecommerce

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.ecommerce.databinding.ActivityHomeBinding
import com.example.ecommerce.firebase.FirebaseManager
import com.example.ecommerce.models.Product

class HomeActivity : AppCompatActivity(), ProductAdapter.OnProductClickListener {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var productAdapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAuthButtons()
        setupCartButton()
        setupCategoryButtons()
        setupRecyclerView()
        loadProductsFromFirebase()
        setupBottomNavigation()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(listener = this)
        binding.productsRecyclerView.apply {
            layoutManager = GridLayoutManager(this@HomeActivity, 2)
            adapter = productAdapter
        }
    }

    private fun loadProductsFromFirebase() {
        // Charger les produits
        FirebaseManager.getAllProducts { products, error ->
            if (error != null) {
                Toast.makeText(this, "Erreur: $error", Toast.LENGTH_SHORT).show()
                return@getAllProducts
            }
            
            if (products.isEmpty()) {
                binding.emptyProductsMessage.visibility = View.VISIBLE
            } else {
                binding.emptyProductsMessage.visibility = View.GONE
                productAdapter.setProducts(products)
            }
        }
    }

    private fun setupAuthButtons() {
        binding.connexionButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.inscriptionButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setupCartButton() {
        // Ajouter un gestionnaire d'événements pour le bouton panier
        val cartButton = binding.cartButton
        cartButton.setOnClickListener {
            Toast.makeText(this, "Ouverture du panier", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, CartActivity::class.java))
        }
    }

    private fun setupCategoryButtons() {
        // Obtenir la liste des catégories
        val categoryLayout1 = binding.root.findViewById<View>(R.id.viewAllCategories)?.parent?.parent as? android.view.ViewGroup
        
        if (categoryLayout1 != null) {
            // Trouver le HorizontalScrollView
            val scrollView = categoryLayout1.getChildAt(1) as? android.widget.HorizontalScrollView
            
            if (scrollView != null) {
                // Obtenir le LinearLayout contenant les catégories
                val categoryContainer = scrollView.getChildAt(0) as? android.view.ViewGroup
                
                if (categoryContainer != null) {
                    // Récupérer les 4 conteneurs de catégories
                    val categoryCount = categoryContainer.childCount
                    for (i in 0 until categoryCount) {
                        val categoryView = categoryContainer.getChildAt(i)
                        
                        // Ajouter un événement OnClick à chaque catégorie
                        categoryView.setOnClickListener {
                            val categoryName = when(i) {
                                0 -> "Accessoires"
                                1 -> "Livres" 
                                2 -> "Calculatrices"
                                3 -> "Cours"
                                else -> "Catégorie"
                            }
                            
                            // Charger les produits de cette catégorie
                            loadProductsByCategory(categoryName)
                        }
                    }
                }
            }
        }
    }

    private fun loadProductsByCategory(category: String) {
        Toast.makeText(this, "Chargement des produits: $category", Toast.LENGTH_SHORT).show()
        
        FirebaseManager.getProductsByCategory(category) { products, error ->
            if (error != null) {
                Toast.makeText(this, "Erreur: $error", Toast.LENGTH_SHORT).show()
                return@getProductsByCategory
            }
            
            if (products.isEmpty()) {
                binding.emptyProductsMessage.visibility = View.VISIBLE
                binding.emptyProductsMessage.text = "Aucun produit dans la catégorie $category"
            } else {
                binding.emptyProductsMessage.visibility = View.GONE
                productAdapter.setProducts(products)
            }
        }
    }

    override fun onProductClick(product: Product) {
        val intent = Intent(this, ProductDetailActivity::class.java)
        intent.putExtra("PRODUCT_ID", product.id)
        startActivity(intent)
    }

    private fun setupBottomNavigation() {
        // Set up bottom navigation
        binding.navHome.setOnClickListener {
            Toast.makeText(this, "Vous êtes déjà sur la page d'accueil", Toast.LENGTH_SHORT).show()
        }
        
        binding.navAdd.setOnClickListener {
            val intent = Intent(this, AddProductActivity::class.java)
            startActivity(intent)
        }
        
        binding.navProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }
} 