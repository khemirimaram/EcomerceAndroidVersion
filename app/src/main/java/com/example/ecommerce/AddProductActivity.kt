package com.example.ecommerce

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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

            setupSpinners()
            setupImageSelection()
            setupButtons()
            
            Log.d(TAG, "Initialisation terminée avec succès")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'initialisation de l'activité", e)
            Toast.makeText(this, "Erreur lors de l'initialisation de l'application", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupSpinners() {
        try {
            Log.d(TAG, "Configuration des spinners")
            // Configuration du spinner de catégorie
            val categoryAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.product_categories,
                android.R.layout.simple_spinner_item
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            binding.spinnerCategory.adapter = categoryAdapter

            // Configuration du spinner d'état
            val conditionAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.product_conditions,
                android.R.layout.simple_spinner_item
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            binding.spinnerCondition.adapter = conditionAdapter

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

            binding.tvImageCount.visibility = View.VISIBLE
            updateImageCountText()
            Log.d(TAG, "Sélection d'images configurée avec succès")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la configuration de la sélection d'images", e)
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
        binding.tvImageCount.text = "${selectedImages.size}/$MAX_IMAGES images sélectionnées"
    }

    private fun validateInputs(): Boolean {
        try {
            Log.d(TAG, "Validation des entrées")
            val title = binding.etProductName.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val priceText = binding.etPrice.text.toString().trim()
            val categoryPosition = binding.spinnerCategory.selectedItemPosition
            val conditionPosition = binding.spinnerCondition.selectedItemPosition

            if (title.isEmpty()) {
                binding.etProductName.error = "Le nom du produit est requis"
                return false
            }

            if (description.isEmpty()) {
                binding.etDescription.error = "La description est requise"
                return false
            }

            if (priceText.isEmpty()) {
                binding.etPrice.error = "Le prix est requis"
                return false
            }

            val price = priceText.toDoubleOrNull()
            if (price == null || price <= 0) {
                binding.etPrice.error = "Prix invalide"
                return false
            }

            if (categoryPosition == 0) {
                Toast.makeText(this, "Veuillez sélectionner une catégorie", Toast.LENGTH_SHORT).show()
                return false
            }

            if (conditionPosition == 0) {
                Toast.makeText(this, "Veuillez sélectionner l'état du produit", Toast.LENGTH_SHORT).show()
                return false
            }

            if (selectedImages.isEmpty()) {
                Toast.makeText(this, "Veuillez sélectionner au moins une image", Toast.LENGTH_SHORT).show()
                return false
            }

            Log.d(TAG, "Validation des entrées réussie")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la validation des entrées", e)
            return false
        }
    }

    private fun submitProduct() {
        try {
            Log.d(TAG, "Début de la soumission du produit")
            binding.progressBar.visibility = View.VISIBLE
            binding.btnSubmit.isEnabled = false

            val title = binding.etProductName.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val price = binding.etPrice.text.toString().toDouble()
            val category = binding.spinnerCategory.selectedItem.toString()
            val condition = binding.spinnerCondition.selectedItem.toString()

            uploadImages(selectedImages) { imageUrls, error ->
                runOnUiThread {
                    if (error != null) {
                        handleError("Erreur lors de l'upload des images: $error")
                        return@runOnUiThread
                    }

                    val product = Product(
                        name = title,
                        description = description,
                        price = price,
                        category = category,
                        condition = condition,
                        location = "Not specified",
                        sellerUserId = FirebaseManager.getCurrentUserId() ?: "",
                        sellerName = "",
                        images = imageUrls ?: listOf()
                    )

                    FirebaseFirestore.getInstance().collection("produits")
                        .add(product)
                        .addOnSuccessListener {
                            Log.d(TAG, "Produit ajouté avec succès")
                            Toast.makeText(this, "Produit ajouté avec succès", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            handleError("Erreur lors de l'ajout du produit: ${e.message}")
                        }
                }
            }
        } catch (e: Exception) {
            handleError("Erreur inattendue: ${e.message}")
        }
    }

    private fun uploadImages(imageUris: List<Uri>, callback: (List<String>?, String?) -> Unit) {
        Thread {
            try {
                Log.d(TAG, "Début de l'upload vers Cloudinary")

                val urls = mutableListOf<String>()

                // 🔽 Initialize Cloudinary using HashMap config
                val config: HashMap<String, String> = HashMap()
                config["cloud_name"] = "dhfckhxho"     // <- Replace this
                config["api_secret"] = "eIt_X-J67PbzXTeA3ICA2KGLSGA"
                config["api_key"] = "195454145722799"     // <- Replace this

                val cloudinary = Cloudinary(config)

                for (uri in imageUris) {
                    val inputStream = contentResolver.openInputStream(uri)
                    val uploadResult = cloudinary.uploader().upload(inputStream, ObjectUtils.emptyMap())
                    val url = uploadResult["secure_url"] as? String
                    if (url != null) {
                        urls.add(url)
                    } else {
                        runOnUiThread {
                            callback(null, "URL introuvable après upload")
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
        binding.progressBar.visibility = View.GONE
        binding.btnSubmit.isEnabled = true
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
