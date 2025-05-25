package com.example.ecommerce

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.ecommerce.databinding.ActivityAddProductBinding
import com.example.ecommerce.firebase.FirebaseManager
import com.example.ecommerce.models.Product
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils

class AddProductActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "AddProductActivity"
    }

    private lateinit var binding: ActivityAddProductBinding
    private val selectedImages = mutableListOf<Uri>()
    private val MAX_IMAGES = 5
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris != null) {
            val remainingSlots = MAX_IMAGES - selectedImages.size
            val validUris = uris.take(remainingSlots)
            
            selectedImages.addAll(validUris)
            updateImageCountText()
            
            if (uris.size > remainingSlots) {
                Toast.makeText(this, "Maximum $MAX_IMAGES images autorisées", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Log.d(TAG, "Initialisation de l'activité")
            binding = ActivityAddProductBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Check if user is logged in
            if (FirebaseManager.getCurrentUserId() == null) {
                Toast.makeText(this, "Veuillez vous connecter pour ajouter un produit", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return
            }

            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()

            setupToolbar()
            setupSpinners()
            setupImageSelection()
            setupButtons()
            setupExchangeSection()
            
            Log.d(TAG, "Initialisation terminée avec succès")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'initialisation de l'activité", e)
            Toast.makeText(this, "Erreur lors de l'initialisation de l'application", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupSpinners() {
        try {
            Log.d(TAG, "Configuration des spinners")
            
            // Configuration du spinner de catégorie
            val categories = resources.getStringArray(R.array.product_categories)
            val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
            (binding.spinnerCategory as? AutoCompleteTextView)?.setAdapter(categoryAdapter)

            // Configuration du spinner d'état
            val conditions = resources.getStringArray(R.array.product_conditions)
            val conditionAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, conditions)
            (binding.spinnerCondition as? AutoCompleteTextView)?.setAdapter(conditionAdapter)

            Log.d(TAG, "Spinners configurés avec succès")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la configuration des spinners", e)
            Toast.makeText(this, "Erreur lors de l'initialisation des listes déroulantes", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupImageSelection() {
        try {
            Log.d(TAG, "Configuration de la sélection d'images")
            binding.btnSelectImages.setOnClickListener {
                if (selectedImages.size >= MAX_IMAGES) {
                    Toast.makeText(this, "Maximum $MAX_IMAGES images autorisées", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                imagePickerLauncher.launch("image/*")
            }

            binding.tvImageCount.isVisible = false
            updateImageCountText()
            Log.d(TAG, "Sélection d'images configurée avec succès")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la configuration de la sélection d'images", e)
        }
    }

    private fun setupExchangeSection() {
        binding.cbExchange.setOnCheckedChangeListener { _, isChecked ->
            binding.tilExchangePreferences.isVisible = isChecked
        }
    }

    private fun setupButtons() {
        try {
            Log.d(TAG, "Configuration des boutons")
            binding.btnSubmit.setOnClickListener {
                if (validateInputs()) {
                    submitProduct()
                }
            }
            
            binding.btnCancel.setOnClickListener {
                finish()
            }
            Log.d(TAG, "Boutons configurés avec succès")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la configuration des boutons", e)
        }
    }

    private fun updateImageCountText() {
        binding.tvImageCount.apply {
            text = "${selectedImages.size}/$MAX_IMAGES images sélectionnées"
            isVisible = selectedImages.isNotEmpty()
        }
    }

    private fun validateInputs(): Boolean {
        try {
            Log.d(TAG, "Validation des entrées")
            var isValid = true

            // Validation du nom du produit
            if (binding.etProductName.text.toString().trim().isEmpty()) {
                binding.etProductName.error = "Le nom du produit est requis"
                isValid = false
            }

            // Validation de la description
            if (binding.etDescription.text.toString().trim().isEmpty()) {
                binding.etDescription.error = "La description est requise"
                isValid = false
            }

            // Validation du prix
            val priceText = binding.etPrice.text.toString().trim()
            if (priceText.isEmpty()) {
                binding.etPrice.error = "Le prix est requis"
                isValid = false
            } else {
                val price = priceText.toDoubleOrNull()
                if (price == null || price <= 0) {
                    binding.etPrice.error = "Prix invalide"
                    isValid = false
                }
            }

            // Validation de la localisation
            if (binding.etLocation.text.toString().trim().isEmpty()) {
                binding.etLocation.error = "La localisation est requise"
                isValid = false
            }

            // Validation de la catégorie
            if (binding.spinnerCategory.text.toString().isEmpty()) {
                binding.spinnerCategory.error = "Veuillez sélectionner une catégorie"
                isValid = false
            }

            // Validation de l'état
            if (binding.spinnerCondition.text.toString().isEmpty()) {
                binding.spinnerCondition.error = "Veuillez sélectionner l'état du produit"
                isValid = false
            }

            // Validation des images
            if (selectedImages.isEmpty()) {
                Toast.makeText(this, "Veuillez sélectionner au moins une image", Toast.LENGTH_SHORT).show()
                isValid = false
            }

            Log.d(TAG, "Validation des entrées terminée. Résultat: $isValid")
            return isValid
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la validation des entrées", e)
            return false
        }
    }

    private fun submitProduct() {
        try {
            Log.d(TAG, "Début de la soumission du produit")
            binding.progressBar.isVisible = true
            binding.btnSubmit.isEnabled = false

            val title = binding.etProductName.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val price = binding.etPrice.text.toString().toDouble()
            val location = binding.etLocation.text.toString().trim()
            val category = binding.spinnerCategory.text.toString()
            val condition = binding.spinnerCondition.text.toString()
            
            val currentUser = auth.currentUser
            if (currentUser == null) {
                handleError("Erreur: Utilisateur non connecté")
                return
            }

            Log.d(TAG, "Current user ID: ${currentUser.uid}")
            Log.d(TAG, "Current user display name: ${currentUser.displayName}")

            firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { userDoc ->
                    Log.d(TAG, "User document data: ${userDoc.data}")
                    
                    val userName = userDoc.getString("displayName")
                        ?: userDoc.getString("name")
                        ?: userDoc.getString("username")
                        ?: userDoc.getString("fullName")
                        ?: currentUser.displayName
                        ?: "Utilisateur"
                    
                    Log.d(TAG, "Final user name selected: $userName")
                    
                    proceedWithUpload(
                        title, description, price, location, category, condition,
                        currentUser.uid, userName, currentUser.photoUrl?.toString()
                    )
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Erreur lors de la récupération du nom d'utilisateur", e)
                    val fallbackName = currentUser.displayName ?: "Utilisateur"
                    proceedWithUpload(
                        title, description, price, location, category, condition,
                        currentUser.uid, fallbackName, currentUser.photoUrl?.toString()
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur inattendue lors de la soumission du produit", e)
            handleError("Erreur inattendue: ${e.message}")
        }
    }

    private fun proceedWithUpload(
        title: String, description: String, price: Double, location: String,
        category: String, condition: String, sellerId: String, sellerName: String,
        sellerPhoto: String?
    ) {
        Log.d(TAG, "Proceeding with upload. Seller name: $sellerName")
        
        uploadImages(selectedImages) { imageUrls, error ->
            runOnUiThread {
                if (error != null) {
                    handleError("Erreur lors de l'upload des images: $error")
                    return@runOnUiThread
                }

                // Création d'un Map simple avec des types de base
                val productData = hashMapOf(
                    "name" to title,
                    "description" to description,
                    "price" to price,
                    "location" to location,
                    "category" to category,
                    "condition" to condition,
                    "sellerId" to sellerId,
                    "sellerName" to sellerName,
                    "sellerPhoto" to sellerPhoto,
                    "images" to (imageUrls ?: listOf<String>()),
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis(),
                    "status" to "active",
                    "quantity" to (binding.etQuantity.text.toString().toIntOrNull() ?: 1),
                    "isAvailableForExchange" to binding.cbExchange.isChecked,
                    "exchangePreferences" to if (binding.cbExchange.isChecked) 
                        binding.etExchangePreferences.text.toString() else null
                )

                Log.d(TAG, "Données du produit à enregistrer : $productData")

                // Utilisation de set() au lieu de add() pour être plus explicite
                FirebaseFirestore.getInstance()
                    .collection("produits")
                    .document()
                    .set(productData)
                    .addOnSuccessListener { 
                        binding.progressBar.isVisible = false
                        Toast.makeText(this@AddProductActivity, 
                            "Produit ajouté avec succès", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Erreur lors de l'ajout du produit", e)
                        handleError("Erreur lors de l'ajout du produit: ${e.message}")
                    }
            }
        }
    }

    private fun uploadImages(imageUris: List<Uri>, callback: (List<String>?, String?) -> Unit) {
        Thread {
            try {
                Log.d(TAG, "Début de l'upload vers Cloudinary")

                val urls = mutableListOf<String>()

                //cloudinary config
                val config = ObjectUtils.asMap(
                    "cloud_name", "dhfckhxho",
                    "api_key", "195454145722799",
                    "api_secret", "eIt_X-J67PbzXTeA3ICA2KGLSGA"
                )

                val cloudinary = Cloudinary(config)

                for (uri in imageUris) {
                    try {
                        val inputStream = contentResolver.openInputStream(uri)
                        val bytes = inputStream?.readBytes()
                        inputStream?.close()
                        
                        if (bytes != null) {
                            val uploadParams = ObjectUtils.asMap(
                                "resource_type", "auto",
                                "unique_filename", true
                            )
                            
                            val uploadResult = cloudinary.uploader().upload(bytes, uploadParams)
                            val secureUrl = uploadResult["secure_url"] as? String
                            
                            if (secureUrl != null) {
                                urls.add(secureUrl)
                            } else {
                                runOnUiThread {
                                    callback(null, "URL introuvable après upload")
                                }
                                return@Thread
                            }
                        } else {
                            runOnUiThread {
                                callback(null, "Impossible de lire l'image")
                            }
                            return@Thread
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur lors de l'upload de l'image", e)
                        runOnUiThread {
                            callback(null, "Erreur lors de l'upload: ${e.message}")
                        }
                        return@Thread
                    }
                }

                runOnUiThread {
                    callback(urls, null)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Erreur upload Cloudinary", e)
                runOnUiThread {
                    callback(null, e.message)
                }
            }
        }.start()
    }

    private fun handleError(message: String) {
        Log.e(TAG, message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        binding.progressBar.isVisible = false
        binding.btnSubmit.isEnabled = true
    }

    private fun generateKeywords(title: String, description: String): List<String> {
        val keywords = mutableSetOf<String>()
        
        // Ajouter le titre et la description en minuscules
        keywords.add(title.lowercase())
        keywords.add(description.lowercase())
        
        // Ajouter chaque mot du titre
        title.split(" ").forEach { word ->
            if (word.length > 2) { // Ignorer les mots trop courts
                keywords.add(word.lowercase())
            }
        }
        
        // Ajouter chaque mot de la description
        description.split(" ").forEach { word ->
            if (word.length > 2) { // Ignorer les mots trop courts
                keywords.add(word.lowercase())
            }
        }
        
        return keywords.toList()
    }
}
