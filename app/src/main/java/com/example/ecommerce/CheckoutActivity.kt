package com.example.ecommerce

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.ecommerce.databinding.ActivityCheckoutBinding
import com.example.ecommerce.models.CartItem
import com.example.ecommerce.models.Order
import com.example.ecommerce.models.OrderItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CheckoutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCheckoutBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val cartItems = mutableListOf<CartItem>()
    private var subtotal = 0.0
    private var shippingFee = 7.0 // Frais de livraison fixe
    private var total = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadCartItems()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        binding.ivBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun loadCartItems() {
        val userId = auth.currentUser?.uid ?: return
        showLoading(true)

        firestore.collection("cart")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                cartItems.clear()
                
                for (document in snapshot.documents) {
                    val cartItem = document.toObject(CartItem::class.java)
                    if (cartItem != null) {
                        if (cartItem.id.isEmpty()) {
                            val updatedItem = cartItem.copy(id = document.id)
                            document.reference.update("id", document.id)
                            cartItems.add(updatedItem)
                        } else {
                            cartItems.add(cartItem)
                        }
                    }
                }
                
                if (cartItems.isEmpty()) {
                    Toast.makeText(this, "Votre panier est vide", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }
                
                updateOrderSummary()
                showLoading(false)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
    
    private fun updateOrderSummary() {
        subtotal = cartItems.sumOf { it.productPrice * it.quantity }
        total = subtotal + shippingFee
        
        binding.tvSubtotal.text = "${String.format("%.2f", subtotal)} DT"
        binding.tvShippingFee.text = "${String.format("%.2f", shippingFee)} DT"
        binding.tvTotal.text = "${String.format("%.2f", total)} DT"
    }

    private fun setupClickListeners() {
        binding.btnPlaceOrder.setOnClickListener {
            validateAndPlaceOrder()
        }
    }
    
    private fun validateAndPlaceOrder() {
        // Valider l'adresse
        val address = binding.etAddress.text.toString().trim()
        val city = binding.etCity.text.toString().trim()
        val zipCode = binding.etZipCode.text.toString().trim()
        val phoneNumber = binding.etPhoneNumber.text.toString().trim()
        
        if (address.isEmpty()) {
            binding.tilAddress.error = "L'adresse est requise"
            return
        } else {
            binding.tilAddress.error = null
        }
        
        if (city.isEmpty()) {
            binding.tilCity.error = "La ville est requise"
            return
        } else {
            binding.tilCity.error = null
        }
        
        if (zipCode.isEmpty()) {
            binding.tilZipCode.error = "Le code postal est requis"
            return
        } else {
            binding.tilZipCode.error = null
        }
        
        if (phoneNumber.isEmpty()) {
            binding.tilPhoneNumber.error = "Le numéro de téléphone est requis"
            return
        } else {
            binding.tilPhoneNumber.error = null
        }
        
        // Construire l'adresse complète
        val fullAddress = "$address, $city, $zipCode"
        
        // Obtenir la méthode de paiement sélectionnée
        val selectedPaymentMethodId = binding.rgPaymentMethods.checkedRadioButtonId
        val selectedPaymentMethod = findViewById<RadioButton>(selectedPaymentMethodId)
        val paymentMethod = selectedPaymentMethod.text.toString()
        
        // Créer la commande
        placeOrder(fullAddress, phoneNumber, paymentMethod)
    }
    
    private fun placeOrder(shippingAddress: String, phoneNumber: String, paymentMethod: String) {
        val userId = auth.currentUser?.uid ?: return
        showLoading(true)
        
        // Créer les éléments de commande
        val orderItems = cartItems.map { cartItem ->
            OrderItem(
                productId = cartItem.productId,
                productName = cartItem.productName,
                productImage = cartItem.productImage,
                quantity = cartItem.quantity,
                price = cartItem.productPrice
            )
        }
        
        // Créer l'objet commande
        val order = hashMapOf(
            "userId" to userId,
            "totalAmount" to total,
            "shippingAddress" to shippingAddress,
            "phoneNumber" to phoneNumber,
            "paymentMethod" to paymentMethod,
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp()
        )
        
        // Ajouter la commande à Firestore
        firestore.collection("orders")
            .add(order)
            .addOnSuccessListener { docRef ->
                val orderId = docRef.id
                
                // Ajouter les éléments de commande
                val batch = firestore.batch()
                
                orderItems.forEach { item ->
                    val itemRef = firestore.collection("orders")
                        .document(orderId)
                        .collection("items")
                        .document()
                    
                    batch.set(itemRef, mapOf(
                        "productId" to item.productId,
                        "productName" to item.productName,
                        "productImage" to item.productImage,
                        "quantity" to item.quantity,
                        "price" to item.price
                    ))
                }
                
                // Supprimer les éléments du panier
                cartItems.forEach { item ->
                    val cartItemRef = firestore.collection("cart")
                        .document(item.id)
                    
                    batch.delete(cartItemRef)
                }
                
                // Exécuter le batch
                batch.commit()
                    .addOnSuccessListener {
                        showLoading(false)
                        showOrderConfirmation(orderId)
                    }
                    .addOnFailureListener { e ->
                        showLoading(false)
                        Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun showOrderConfirmation(orderId: String) {
        AlertDialog.Builder(this)
            .setTitle("Commande confirmée")
            .setMessage("Votre commande a été placée avec succès. Numéro de commande: $orderId")
            .setPositiveButton("OK") { _, _ ->
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }
} 