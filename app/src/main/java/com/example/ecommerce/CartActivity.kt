package com.example.ecommerce

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ecommerce.adapters.CartAdapter
import com.example.ecommerce.databinding.ActivityCartBinding
import com.example.ecommerce.models.CartItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CartActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCartBinding
    private lateinit var cartAdapter: CartAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val cartItems = mutableListOf<CartItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadCartItems()
        setupClickListeners()
    }

    private fun setupToolbar() {
        try {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        binding.toolbarTitle.text = "Panier"
        
        binding.ivBack.setOnClickListener {
            try {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                onBackPressed()
            }
        }
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            cartItems,
            onQuantityChanged = { cartItem, position ->
                updateCartItem(cartItem, position)
            },
            onItemRemoved = { cartItem, position ->
                removeCartItem(cartItem, position)
            }
        )
        binding.rvCartItems.apply {
            layoutManager = LinearLayoutManager(this@CartActivity)
            adapter = cartAdapter
        }
    }

    private fun loadCartItems() {
        val userId = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("cart")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                binding.progressBar.visibility = View.GONE
                cartItems.clear()
                
                for (document in snapshot.documents) {
                    val cartItem = document.toObject(CartItem::class.java)
                    // Si l'ID n'est pas défini, l'ajouter
                    if (cartItem != null) {
                        if (cartItem.id.isEmpty()) {
                            // Mise à jour de l'ID si nécessaire
                            val updatedItem = cartItem.copy(id = document.id)
                            document.reference.update("id", document.id)
                            cartItems.add(updatedItem)
                        } else {
                            cartItems.add(cartItem)
                        }
                    }
                }
                
                // Tri local au lieu de tri dans la requête
                cartItems.sortByDescending { it.timestamp }
                
                cartAdapter.notifyDataSetChanged()
                updateTotal()
                
                if (cartItems.isEmpty()) {
                    binding.emptyCartMessage.visibility = View.VISIBLE
                    binding.rvCartItems.visibility = View.GONE
                } else {
                    binding.emptyCartMessage.visibility = View.GONE
                    binding.rvCartItems.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Erreur de chargement du panier: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.emptyCartMessage.visibility = View.VISIBLE
                binding.rvCartItems.visibility = View.GONE
            }
    }

    private fun updateCartItem(cartItem: CartItem, position: Int) {
        val userId = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("cart")
            .document(cartItem.id)
            .update("quantity", cartItem.quantity)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                updateTotal()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Erreur de mise à jour du panier: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeCartItem(cartItem: CartItem, position: Int) {
        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("cart")
            .document(cartItem.id)
            .delete()
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                cartItems.removeAt(position)
                cartAdapter.notifyItemRemoved(position)
                updateTotal()
                Toast.makeText(this, "Article supprimé du panier", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Erreur lors de la suppression: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTotal() {
        val total = cartItems.sumOf { it.productPrice * it.quantity }
        binding.tvTotal.text = "${String.format("%.2f", total)} DT"
    }

    private fun setupClickListeners() {
        binding.btnCheckout.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Votre panier est vide", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Lancer l'activité de checkout
            val intent = Intent(this, CheckoutActivity::class.java)
            startActivity(intent)
        }
    }
} 