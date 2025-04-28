package com.example.ecommerce.activities

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ecommerce.databinding.ActivityAddProductBinding
import com.example.ecommerce.firebase.FirebaseManager
import com.example.ecommerce.models.Product

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductBinding
    private val firebaseManager = FirebaseManager
    private val selectedImageUris = mutableListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup submit buttons
        binding.btnSubmit.setOnClickListener {
            submitProduct()
        }
        
        binding.btnSubmitBottom.setOnClickListener {
            submitProduct()
        }
        
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    // Méthode simplifiée pour gérer les URIs d'images
    private fun convertGoogleDriveUris(uris: List<Uri>): List<Uri> {
        val convertedUris = mutableListOf<Uri>()
        
        try {
            for (uri in uris) {
                // Ajouter l'URI originale par défaut
                convertedUris.add(uri)
                
                // Log l'URI pour le débogage
                Log.d("AddProductActivity", "URI originale: $uri")
            }
        } catch (e: Exception) {
            Log.e("AddProductActivity", "Erreur lors de la conversion des URIs: ${e.message}")
        }
        
        return convertedUris
    }

    private fun submitProduct() {
        // Vérifier si l'utilisateur est connecté
        if (firebaseManager.currentUser == null) {
            Toast.makeText(this, "Veuillez vous connecter pour publier un produit", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtenir les données du formulaire
        val name = binding.etProductName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val price = binding.etPrice.text.toString().trim()
        val category = binding.spinnerCategory.selectedItem.toString()
        val condition = binding.spinnerCondition.selectedItem.toString()
        val location = "Not specified" // We don't have a location field in the form

        // Validation de base
        if (name.isEmpty() || description.isEmpty() || price.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs obligatoires", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Créer un objet Product
            val product = Product(
                id = "",  // Sera généré par Firebase
                name = name,
                description = description,
                price = price.toDouble(),
                category = category,
                condition = condition,
                sellerUserId = firebaseManager.currentUser?.uid ?: "",
                sellerName = "",  // Sera défini par FirebaseManager
                location = location,
                imageUrl = null,  // Sera mis à jour après l'upload de l'image
                createdAt = System.currentTimeMillis(),    // Sera défini par FirebaseManager
                updatedAt = System.currentTimeMillis()     // Sera défini par FirebaseManager
            )

            // Montrer le dialogue de progression
            showProgressDialog("Publication du produit en cours...")

            // Utiliser les URIs originales sans conversion
            firebaseManager.uploadProduct(product, selectedImageUris) { success, message ->
                runOnUiThread {
                    hideProgressDialog()
                    if (success) {
                        Toast.makeText(this, "Produit publié avec succès", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Erreur: $message", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            hideProgressDialog()
            Log.e("AddProductActivity", "Erreur lors de la création du produit: ${e.message}")
            Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    // Helper methods for progress dialog
    private fun showProgressDialog(message: String) {
        // Show the progress bar
        binding.progressBar.visibility = android.view.View.VISIBLE
    }
    
    private fun hideProgressDialog() {
        // Hide the progress bar
        binding.progressBar.visibility = android.view.View.GONE
    }
} 