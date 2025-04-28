package com.example.ecommerce

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.ecommerce.databinding.ActivityProductDetailBinding
import com.example.ecommerce.models.CartItem
import com.example.ecommerce.models.Product
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProductDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductDetailBinding
    private var isFavorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupButtons()
        loadProductDetails()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun setupButtons() {
        binding.btnAddToFavorites.setOnClickListener {
            toggleFavorite()
        }

        binding.btnContactSeller.setOnClickListener {
            // TODO: Implement contact seller functionality
            Toast.makeText(this, "Contact seller clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        binding.btnAddToFavorites.setOnClickListener {
            addToCart()
        }
    }

    private fun toggleFavorite() {
        isFavorite = !isFavorite
        updateFavoriteButton()
        
        val message = if (isFavorite) {
            getString(R.string.added_to_favorites)
        } else {
            getString(R.string.removed_from_favorites)
        }
        
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun updateFavoriteButton() {
        binding.btnAddToFavorites.text = getString(
            if (isFavorite) R.string.remove_from_favorites
            else R.string.add_to_favorites
        )
    }

    private fun loadProductDetails() {
        val product = intent.getParcelableExtra<Product>("product")
        product?.let {
            binding.tvProductTitle.text = it.name
            binding.tvProductPrice.text = "$${String.format("%.2f", it.price)}"
            binding.tvProductDescription.text = it.description
            
            if (it.imageUrl != null) {
                Glide.with(this)
                    .load(it.imageUrl)
                    .into(binding.productImage)
            }
        }
    }

    private fun addToCart() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val product = intent.getParcelableExtra<Product>("product") ?: return

        binding.root.post {
            Snackbar.make(binding.root, "Ajout au panier...", Snackbar.LENGTH_SHORT).show()
        }

        val cartItem = CartItem(
            id = "", // Ceci sera mis à jour après la création du document
            productId = product.id,
            userId = userId,
            productName = product.name,
            productImage = product.imageUrl ?: "",
            productPrice = product.price
        )

        FirebaseFirestore.getInstance().collection("cart")
            .add(cartItem)
            .addOnSuccessListener { documentReference ->
                // Mettre à jour le document avec son ID pour les futurs accès
                val itemId = documentReference.id
                documentReference.update("id", itemId)
                    .addOnSuccessListener {
                        binding.root.post {
                            Snackbar.make(binding.root, "Ajouté au panier", Snackbar.LENGTH_SHORT).show()
                        }
                    }
            }
            .addOnFailureListener { e ->
                binding.root.post {
                    Snackbar.make(binding.root, "Erreur: ${e.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 